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

        // Grupisi po terminu (dan, cas) i ostavi samo termine sa 2+ profesora.
        Map<TerminKljuc, List<RasporedStavka>> poTerminu = new LinkedHashMap<>();
        for (RasporedStavka s : stavke) {
            poTerminu.computeIfAbsent(new TerminKljuc(s.getDan(), s.getCas()), k -> new ArrayList<>()).add(s);
        }

        List<DetekcijaVezbiResponse.TerminVezbi> termini = new ArrayList<>();
        Map<UUID, Integer> brojCasovaPoProfesoru = new LinkedHashMap<>();
        Map<UUID, String> imenaProfesora = new HashMap<>();

        for (Map.Entry<TerminKljuc, List<RasporedStavka>> e : poTerminu.entrySet()) {
            List<RasporedStavka> grupa = e.getValue();
            if (grupa.size() < 2) continue;

            List<UUID> ids = new ArrayList<>();
            List<String> imena = new ArrayList<>();
            for (RasporedStavka s : grupa) {
                Korisnik k = s.getKorisnik();
                ids.add(k.getId());
                imena.add(k.punoIme());
                imenaProfesora.put(k.getId(), k.punoIme());
                brojCasovaPoProfesoru.merge(k.getId(), 1, Integer::sum);
            }
            termini.add(new DetekcijaVezbiResponse.TerminVezbi(e.getKey().dan, e.getKey().cas, ids, imena));
        }

        List<DetekcijaVezbiResponse.ProfesorVezbi> profesori = brojCasovaPoProfesoru.entrySet().stream()
                .map(e -> new DetekcijaVezbiResponse.ProfesorVezbi(
                        e.getKey(), imenaProfesora.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(DetekcijaVezbiResponse.ProfesorVezbi::profesorIme))
                .toList();

        return new DetekcijaVezbiResponse(odeljenjeId, od.label(), profesori, termini);
    }

    // -------- KREIRANJE ROTACIJE --------

    @Transactional
    public RotacijaResponse kreiraj(CustomUserDetails ja, KreirajRotacijuRequest req) {
        UUID skolaId = TenantContext.require();

        DetekcijaVezbiResponse detekcija = detektujVezbe(req.odeljenjeId());

        // Validacija: suma casova po profesoru iz request-a mora biti = detektovani broj
        Map<UUID, Integer> detektovaniPoProfesoru = new HashMap<>();
        detekcija.profesori().forEach(p -> detektovaniPoProfesoru.put(p.profesorId(), p.brojCasovaVezbi()));

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
        return rotacijaRepo.moje(skolaId, ja.id()).stream().map(this::toResponse).toList();
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
