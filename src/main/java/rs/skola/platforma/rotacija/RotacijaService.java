package rs.skola.platforma.rotacija;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.rotacija.domain.RotKonfiguracija;
import rs.skola.platforma.rotacija.domain.RotNedelja;
import rs.skola.platforma.rotacija.repo.RotKonfiguracijaRepository;
import rs.skola.platforma.rotacija.repo.RotNedeljaRepository;
import rs.skola.platforma.rotacija.web.AzurirajNedeljuRequest;
import rs.skola.platforma.rotacija.web.KreirajRotacijuRequest;
import rs.skola.platforma.rotacija.web.OdeljenjeKratko;
import rs.skola.platforma.rotacija.web.RotNedeljaResponse;
import rs.skola.platforma.rotacija.web.RotacijaResponse;
import rs.skola.platforma.predmeti.repo.PredmetRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RotacijaService {

    private final RotKonfiguracijaRepository konfigRepo;
    private final RotNedeljaRepository nedeljaRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final PredmetRepository predmetRepo;
    private final KorisnikRepository korisnikRepo;
    private final RotacijaAlgorithm algoritam;

    @Transactional
    public RotacijaResponse kreirajKonfiguraciju(CustomUserDetails ja, KreirajRotacijuRequest req) {
        UUID skolaId = TenantContext.require();

        if (req.grupaVelicina() > req.odeljenjaIds().size()) {
            throw new ValidationException("Velicina grupe ne moze biti veca od broja odeljenja");
        }

        List<Odeljenje> odeljenja = ucitajOdeljenjaIzSkole(skolaId, req.odeljenjaIds());

        Korisnik nastavnik = korisnikRepo.findById(ja.id())
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", ja.id()));

        Predmet predmet = null;
        if (req.predmetId() != null) {
            predmet = predmetRepo.findById(req.predmetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Predmet", req.predmetId()));
            if (!skolaId.equals(predmet.getSkolaId())) {
                throw new TenantViolationException("Predmet ne pripada vasoj skoli");
            }
        }

        RotKonfiguracija k = RotKonfiguracija.builder()
                .nastavnik(nastavnik)
                .predmet(predmet)
                .naziv(req.naziv())
                .odeljenjaIds(odeljenja.stream().map(Odeljenje::getId).toList())
                .grupaVelicina(req.grupaVelicina())
                .casovaNedeljno(req.casovaNedeljno())
                .skolskaGodina(req.skolskaGodina())
                .build();
        k.setSkolaId(skolaId);
        return toResponse(konfigRepo.save(k), odeljenja, List.of());
    }

    @Transactional
    public RotacijaResponse generisi(UUID konfiguracijaId) {
        UUID skolaId = TenantContext.require();
        RotKonfiguracija k = nadjiUSkoli(konfiguracijaId, skolaId);

        nedeljaRepo.deleteAllByKonfiguracijaId(k.getId());

        List<List<UUID>> kombinacije = algoritam.generisiCiklus(
                k.getOdeljenjaIds(), k.getGrupaVelicina());

        List<RotNedelja> nedelje = new ArrayList<>();
        short broj = 1;
        for (List<UUID> kombinacija : kombinacije) {
            RotNedelja n = RotNedelja.builder()
                    .konfiguracija(k)
                    .brojNedelje(broj++)
                    .odeljenjaIds(kombinacija)
                    .build();
            n.setSkolaId(skolaId);
            nedelje.add(nedeljaRepo.save(n));
        }
        List<Odeljenje> odeljenja = ucitajOdeljenjaIzSkole(skolaId, k.getOdeljenjaIds());
        return toResponse(k, odeljenja, nedelje);
    }

    @Transactional(readOnly = true)
    public List<RotacijaResponse> sveZaSkolu() {
        UUID skolaId = TenantContext.require();
        return konfigRepo.sveZaSkolu(skolaId).stream()
                .map(k -> toResponse(k, ucitajOdeljenjaIzSkole(skolaId, k.getOdeljenjaIds()),
                        nedeljaRepo.findAllByKonfiguracijaIdOrderByBrojNedeljeAsc(k.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RotacijaResponse> mojeKonfiguracije(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        return konfigRepo.mojeKonfiguracije(skolaId, ja.id()).stream()
                .map(k -> toResponse(k, ucitajOdeljenjaIzSkole(skolaId, k.getOdeljenjaIds()),
                        nedeljaRepo.findAllByKonfiguracijaIdOrderByBrojNedeljeAsc(k.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RotacijaResponse pregled(UUID konfiguracijaId) {
        UUID skolaId = TenantContext.require();
        RotKonfiguracija k = nadjiUSkoli(konfiguracijaId, skolaId);
        return toResponse(k,
                ucitajOdeljenjaIzSkole(skolaId, k.getOdeljenjaIds()),
                nedeljaRepo.findAllByKonfiguracijaIdOrderByBrojNedeljeAsc(k.getId()));
    }

    @Transactional
    public RotNedeljaResponse azurirajNedelju(UUID nedeljaId, AzurirajNedeljuRequest req) {
        UUID skolaId = TenantContext.require();
        RotNedelja n = nedeljaRepo.findById(nedeljaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rotaciona nedelja", nedeljaId));
        if (!skolaId.equals(n.getSkolaId())) {
            throw new TenantViolationException();
        }
        RotKonfiguracija k = n.getKonfiguracija();
        Set<UUID> dozvoljena = new HashSet<>(k.getOdeljenjaIds());
        if (!dozvoljena.containsAll(req.odeljenjaIds())) {
            throw new ConflictException("Nedelja sme da sadrzi samo odeljenja iz konfiguracije");
        }
        if (req.odeljenjaIds().size() != k.getGrupaVelicina()) {
            throw new ValidationException("Nedelja mora imati tacno %d odeljenja".formatted(k.getGrupaVelicina()));
        }
        n.setOdeljenjaIds(req.odeljenjaIds());

        List<Odeljenje> odeljenja = ucitajOdeljenjaIzSkole(skolaId, n.getOdeljenjaIds());
        Map<UUID, Odeljenje> mapa = new HashMap<>();
        odeljenja.forEach(o -> mapa.put(o.getId(), o));
        return new RotNedeljaResponse(n.getId(), n.getBrojNedelje(),
                n.getOdeljenjaIds().stream().map(mapa::get).filter(java.util.Objects::nonNull)
                        .map(this::toKratko).toList());
    }

    @Transactional
    public void obrisi(UUID konfiguracijaId) {
        UUID skolaId = TenantContext.require();
        RotKonfiguracija k = nadjiUSkoli(konfiguracijaId, skolaId);
        konfigRepo.delete(k);
    }

    // -------- helpers --------

    private RotKonfiguracija nadjiUSkoli(UUID id, UUID skolaId) {
        RotKonfiguracija k = konfigRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rotacija", id));
        if (!skolaId.equals(k.getSkolaId())) {
            throw new TenantViolationException();
        }
        return k;
    }

    private List<Odeljenje> ucitajOdeljenjaIzSkole(UUID skolaId, List<UUID> ids) {
        List<Odeljenje> odeljenja = odeljenjeRepo.findAllById(ids);
        for (Odeljenje o : odeljenja) {
            if (!skolaId.equals(o.getSkolaId())) {
                throw new TenantViolationException("Odeljenje " + o.getId() + " ne pripada vasoj skoli");
            }
        }
        if (odeljenja.size() != ids.size()) {
            throw new ResourceNotFoundException("Neka od navedenih odeljenja ne postoje");
        }
        odeljenja.sort(Comparator.comparing(Odeljenje::getRazred).thenComparing(Odeljenje::getOznaka));
        return odeljenja;
    }

    private OdeljenjeKratko toKratko(Odeljenje o) {
        return new OdeljenjeKratko(o.getId(), o.label(), o.getRazred(), o.getOznaka());
    }

    private RotacijaResponse toResponse(RotKonfiguracija k, List<Odeljenje> odeljenja, List<RotNedelja> nedelje) {
        Map<UUID, Odeljenje> mapa = new HashMap<>();
        odeljenja.forEach(o -> mapa.put(o.getId(), o));

        List<RotNedeljaResponse> nedeljeResp = nedelje.stream()
                .map(n -> new RotNedeljaResponse(
                        n.getId(),
                        n.getBrojNedelje(),
                        n.getOdeljenjaIds().stream()
                                .map(mapa::get).filter(java.util.Objects::nonNull)
                                .map(this::toKratko).toList()))
                .toList();

        List<List<UUID>> kombinacije = nedelje.stream().map(RotNedelja::getOdeljenjaIds).toList();
        RotacijaAlgorithm.IzvestajBalansa izv = algoritam.validirajBalans(k.getOdeljenjaIds(), kombinacije);
        RotacijaResponse.Statistika stat = new RotacijaResponse.Statistika(
                izv.balansirano(), izv.minCasova(), izv.maxCasova(), izv.casoviPoOdeljenju(), nedelje.size());

        Korisnik n = k.getNastavnik();
        Predmet p = k.getPredmet();
        return new RotacijaResponse(
                k.getId(),
                k.getNaziv(),
                n == null ? null : n.getId(),
                n == null ? null : n.punoIme(),
                p == null ? null : p.getId(),
                p == null ? null : p.getNaziv(),
                k.getGrupaVelicina(),
                k.getCasovaNedeljno(),
                k.getSkolskaGodina(),
                odeljenja.stream().map(this::toKratko).toList(),
                nedeljeResp,
                stat
        );
    }
}
