package rs.skola.platforma.rotacija;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.raspored.domain.Dan;
import rs.skola.platforma.raspored.domain.RasporedStavka;
import rs.skola.platforma.raspored.domain.VerzijaRasporeda;
import rs.skola.platforma.raspored.repo.RasporedStavkaRepository;
import rs.skola.platforma.raspored.repo.VerzijaRasporedaRepository;
import rs.skola.platforma.rotacija.domain.RotDodela;
import rs.skola.platforma.rotacija.domain.RotPredmet;
import rs.skola.platforma.rotacija.domain.Rotacija;
import rs.skola.platforma.rotacija.repo.RotacijaRepository;
import rs.skola.platforma.rotacija.web.DetekcijaVezbiResponse;
import rs.skola.platforma.rotacija.web.KreirajRotacijuRequest;
import rs.skola.platforma.rotacija.web.RotacijaResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RotacijaService {

    private final RotacijaRepository rotacijaRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final KorisnikRepository korisnikRepo;
    private final VerzijaRasporedaRepository verzijaRepo;
    private final RasporedStavkaRepository stavkaRepo;
    private final RotacijaAlgoritam algoritam;

    // -------- DETEKCIJA VEZBI --------

    @Transactional(readOnly = true)
    public DetekcijaVezbiResponse detektujVezbe(UUID odeljenjeId) {
        UUID skolaId = TenantContext.require();
        Odeljenje od = odeljenjeRepo.findById(odeljenjeId)
                .orElseThrow(() -> new ResourceNotFoundException("Odeljenje", odeljenjeId));
        if (!skolaId.equals(od.getSkolaId())) {
            throw new TenantViolationException();
        }

        VerzijaRasporeda verzija = verzijaRepo.findFirstBySkolaIdAndAktivanTrue(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "VEZBE_NEMA_RASPOREDA",
                        "Aktivna verzija rasporeda ne postoji — uvezi raspored pre kreiranja rotacije"));

        List<RasporedStavka> stavke = stavkaRepo.sveZaOdeljenje(skolaId, verzija.getId(), odeljenjeId);

        // Grupisi po terminu (dan, cas). Profesor se identifikuje po imenu (label)
        // iz rasporeda — distinct po terminu, jer neki XML formati ubacuju istog
        // profesora vise puta za isti termin (npr. "4-1/4-1").
        // Ne koristimo Map.merge jer ne dozvoljava null vrednost (kad korisnik
        // nije mapiran u sistemu), pa rucno radimo "put-if-better".
        Map<TerminKljuc, Map<String, UUID>> poTerminu = new LinkedHashMap<>();
        for (RasporedStavka s : stavke) {
            String label = s.getNastavnikLabel();
            if (label == null) continue;
            UUID korisnikId = s.getKorisnik() == null ? null : s.getKorisnik().getId();
            Map<String, UUID> mapaTermina = poTerminu.computeIfAbsent(
                    new TerminKljuc(s.getDan(), s.getCas()), key -> new LinkedHashMap<>());
            if (!mapaTermina.containsKey(label) || (mapaTermina.get(label) == null && korisnikId != null)) {
                mapaTermina.put(label, korisnikId);
            }
        }

        List<DetekcijaVezbiResponse.TerminVezbi> sviTermini = new ArrayList<>();
        List<DetekcijaVezbiResponse.TerminVezbi> vezbeTermini = new ArrayList<>();
        Map<String, Integer> brojCasovaPoLabelu = new LinkedHashMap<>();
        Map<String, UUID> idPoLabelu = new HashMap<>();

        for (Map.Entry<TerminKljuc, Map<String, UUID>> e : poTerminu.entrySet()) {
            Map<String, UUID> profesoriMap = e.getValue();
            List<String> imena = new ArrayList<>(profesoriMap.keySet());
            List<UUID> ids = imena.stream().map(profesoriMap::get).toList();
            DetekcijaVezbiResponse.TerminVezbi termin =
                    new DetekcijaVezbiResponse.TerminVezbi(e.getKey().dan, e.getKey().cas, ids, imena);
            sviTermini.add(termin);

            if (profesoriMap.size() >= 2) {
                vezbeTermini.add(termin);
                for (Map.Entry<String, UUID> p : profesoriMap.entrySet()) {
                    // Rucno put-if-better umesto Map.merge — dozvoljen je null UUID
                    UUID existing = idPoLabelu.get(p.getKey());
                    if (!idPoLabelu.containsKey(p.getKey()) || (existing == null && p.getValue() != null)) {
                        idPoLabelu.put(p.getKey(), p.getValue());
                    }
                    brojCasovaPoLabelu.merge(p.getKey(), 1, Integer::sum);
                }
            }
        }

        List<DetekcijaVezbiResponse.ProfesorVezbi> profesori = brojCasovaPoLabelu.entrySet().stream()
                .map(e -> new DetekcijaVezbiResponse.ProfesorVezbi(
                        idPoLabelu.get(e.getKey()),
                        e.getKey(),
                        idPoLabelu.get(e.getKey()) != null,
                        e.getValue()))
                .sorted(Comparator.comparing(DetekcijaVezbiResponse.ProfesorVezbi::profesorIme))
                .toList();

        return new DetekcijaVezbiResponse(
                odeljenjeId,
                od.label(),
                stavke.size(),
                poTerminu.size(),
                profesori,
                vezbeTermini,
                sviTermini);
    }

    // -------- KREIRANJE ROTACIJE --------

    @Transactional
    public RotacijaResponse kreiraj(CustomUserDetails ja, KreirajRotacijuRequest req) {
        UUID skolaId = TenantContext.require();

        DetekcijaVezbiResponse detekcija = detektujVezbe(req.odeljenjeId());

        // Provera: svi profesori sa vezbama u rasporedu moraju biti u sistemu
        List<String> neuSistemu = detekcija.profesori().stream()
                .filter(p -> !p.uSistemu())
                .map(DetekcijaVezbiResponse.ProfesorVezbi::profesorIme)
                .toList();
        if (!neuSistemu.isEmpty()) {
            throw new ValidationException(
                    "PROFESORI_VAN_SISTEMA",
                    "Sledeci profesori vezbi nisu u sistemu kao korisnici: %s. "
                            .formatted(String.join(", ", neuSistemu))
                            + "Dodaj ih u Korisnici pre kreiranja rotacije.");
        }

        // Validacija: suma casova po profesoru iz request-a mora biti = detektovani broj
        Map<UUID, Integer> detektovaniPoProfesoru = new HashMap<>();
        Map<UUID, String> imePoIdu = new HashMap<>();
        detekcija.profesori().forEach(p -> {
            detektovaniPoProfesoru.put(p.profesorId(), p.brojCasovaVezbi());
            imePoIdu.put(p.profesorId(), p.profesorIme());
        });

        Map<UUID, Integer> unetiPoProfesoru = new HashMap<>();
        for (KreirajRotacijuRequest.PredmetStavka st : req.predmeti()) {
            unetiPoProfesoru.merge(st.profesorId(), st.casovaNedeljno().intValue(), Integer::sum);
        }

        for (DetekcijaVezbiResponse.ProfesorVezbi p : detekcija.profesori()) {
            int uneto = unetiPoProfesoru.getOrDefault(p.profesorId(), 0);
            if (uneto != p.brojCasovaVezbi()) {
                throw new ValidationException(
                        "SUMA_CASOVA",
                        "Profesor %s ima %d casova vezbi u rasporedu, a unet je zbir %d"
                                .formatted(p.profesorIme(), p.brojCasovaVezbi(), uneto));
            }
        }

        for (UUID profesorId : unetiPoProfesoru.keySet()) {
            if (!detektovaniPoProfesoru.containsKey(profesorId)) {
                throw new ValidationException(
                        "Profesor sa id %s nema casove vezbi u odeljenju".formatted(profesorId));
            }
        }

        Odeljenje od = odeljenjeRepo.findById(req.odeljenjeId()).orElseThrow();
        Korisnik kreator = korisnikRepo.findById(ja.id())
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", ja.id()));

        Rotacija r = Rotacija.builder()
                .nastavnik(kreator)
                .odeljenje(od)
                .naziv(req.naziv())
                .brojGrupa(req.brojGrupa())
                .brojNedelja(req.brojNedelja())
                .skolskaGodina(req.skolskaGodina())
                .build();
        r.setSkolaId(skolaId);

        short rb = 1;
        for (KreirajRotacijuRequest.PredmetStavka st : req.predmeti()) {
            Korisnik profesor = korisnikRepo.findById(st.profesorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profesor", st.profesorId()));
            RotPredmet p = RotPredmet.builder()
                    .rotacija(r)
                    .profesor(profesor)
                    .naziv(st.naziv())
                    .casovaNedeljno(st.casovaNedeljno())
                    .redniBroj(rb++)
                    .build();
            r.getPredmeti().add(p);
        }

        // Generisi dodele
        List<RotDodela> dodele = algoritam.generisi(r, detekcija);
        r.getDodele().addAll(dodele);

        Rotacija sacuvana = rotacijaRepo.save(r);
        return toResponse(sacuvana);
    }

    // -------- CITANJE --------

    @Transactional(readOnly = true)
    public List<RotacijaResponse> sveZaSkolu() {
        UUID skolaId = TenantContext.require();
        return rotacijaRepo.sveZaSkolu(skolaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RotacijaResponse> moje(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        return rotacijaRepo.rotacijeNastavnika(skolaId, ja.id()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RotacijaResponse pregled(UUID id) {
        return toResponse(nadji(id));
    }

    @Transactional
    public void obrisi(UUID id) {
        rotacijaRepo.delete(nadji(id));
    }

    private Rotacija nadji(UUID id) {
        UUID skolaId = TenantContext.require();
        Rotacija r = rotacijaRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rotacija", id));
        if (!skolaId.equals(r.getSkolaId())) {
            throw new TenantViolationException();
        }
        return r;
    }

    // -------- MAPIRANJE --------

    private RotacijaResponse toResponse(Rotacija r) {
        List<RotacijaResponse.PredmetResponse> predmeti = r.getPredmeti().stream()
                .sorted(Comparator.comparing(RotPredmet::getRedniBroj))
                .map(p -> new RotacijaResponse.PredmetResponse(
                        p.getId(),
                        p.getProfesor().getId(),
                        p.getProfesor().punoIme(),
                        p.getNaziv(),
                        p.getCasovaNedeljno(),
                        p.getRedniBroj()))
                .toList();

        // Grupisi dodele po nedelji, zatim po (dan, cas)
        Map<Short, List<RotDodela>> poNedelji = new LinkedHashMap<>();
        for (RotDodela d : r.getDodele()) {
            poNedelji.computeIfAbsent(d.getBrojNedelje(), k -> new ArrayList<>()).add(d);
        }

        List<RotacijaResponse.NedeljaResponse> nedelje = new ArrayList<>();
        for (short i = 1; i <= r.getBrojNedelja(); i++) {
            List<RotDodela> lista = poNedelji.getOrDefault(i, List.of());
            lista = new ArrayList<>(lista);
            lista.sort(Comparator
                    .comparing((RotDodela d) -> d.getDan().ordinal())
                    .thenComparing(RotDodela::getCas));
            List<RotacijaResponse.TerminDodela> termini = lista.stream()
                    .map(d -> new RotacijaResponse.TerminDodela(
                            d.getDan(),
                            d.getCas(),
                            d.getProfesor().getId(),
                            d.getProfesor().punoIme(),
                            d.getPredmetNaziv(),
                            d.getBrojGrupe()))
                    .toList();
            nedelje.add(new RotacijaResponse.NedeljaResponse(i, termini));
        }

        return new RotacijaResponse(
                r.getId(),
                r.getNaziv(),
                r.getNastavnik().getId(),
                r.getNastavnik().punoIme(),
                r.getOdeljenje().getId(),
                r.getOdeljenje().label(),
                r.getBrojGrupa(),
                r.getBrojNedelja(),
                r.getSkolskaGodina(),
                predmeti,
                nedelje,
                r.getCreatedAt()
        );
    }

    /** Kompozitni kljuc za grupisanje stavki rasporeda po terminu. */
    private record TerminKljuc(Dan dan, Short cas) {}
}
