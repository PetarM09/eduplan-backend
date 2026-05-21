package rs.skola.platforma.zamene;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.raspored.domain.Dan;
import rs.skola.platforma.raspored.domain.RasporedStavka;
import rs.skola.platforma.raspored.domain.VerzijaRasporeda;
import rs.skola.platforma.raspored.repo.RasporedStavkaRepository;
import rs.skola.platforma.raspored.repo.VerzijaRasporedaRepository;
import rs.skola.platforma.zamene.domain.Zamena;
import rs.skola.platforma.zamene.domain.ZamenaStatus;
import rs.skola.platforma.zamene.repo.ZamenaRepository;
import rs.skola.platforma.zamene.web.DodeliZamenikaRequest;
import rs.skola.platforma.zamene.web.KandidatZamenikResponse;
import rs.skola.platforma.zamene.web.OdbijZamenuRequest;
import rs.skola.platforma.zamene.web.PrijaviOdsustvoRequest;
import rs.skola.platforma.zamene.web.ZamenaMapper;
import rs.skola.platforma.zamene.web.ZamenaResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Svi upiti i izmene proveravaju da resurs pripada {@code TenantContext.require()} skoli.
 * Kandidati za zamenika ukljucuju iskljucivo aktivne NASTAVNIK-e iz iste skole, koji nemaju
 * svoj cas u tom terminu i nisu vec predlozeni za istu zamenu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZameneService {

    private static final int PROZOR_OPTERECENJA_DANA = 30;

    private final ZamenaRepository zamenaRepository;
    private final KorisnikRepository korisnikRepository;
    private final VerzijaRasporedaRepository verzijaRepository;
    private final RasporedStavkaRepository stavkaRepository;
    private final ZamenaMapper mapper;

    @Transactional
    public List<ZamenaResponse> prijaviOdsustvo(CustomUserDetails ja, PrijaviOdsustvoRequest req) {
        UUID skolaId = TenantContext.require();
        Korisnik odsutni = korisnikRepository.findById(ja.id())
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", ja.id()));

        Dan dan = toDan(req.datum());
        if (dan == null) {
            throw new ValidationException("Datum pada na nedelju — toga dana nema nastave");
        }

        // Mapa cas -> stavka iz aktivne verzije (ako postoji), da popunimo odeljenje/predmet
        Map<Short, RasporedStavka> stavkePoCasu = stavkePoCasuZaNastavnika(skolaId, ja.id(), dan);

        List<Zamena> kreirane = new ArrayList<>();
        for (Short cas : req.casovi()) {
            validirajCas(cas);
            RasporedStavka stavka = stavkePoCasu.get(cas);
            Zamena z = Zamena.builder()
                    .odsutni(odsutni)
                    .zamenik(null)
                    .datum(req.datum())
                    .cas(cas)
                    .odeljenje(stavka == null ? null : stavka.getOdeljenje())
                    .predmetLabel(stavka == null ? null : stavka.getPredmetLabel())
                    .razlog(req.razlog())
                    .status(ZamenaStatus.PREDLOZENA)
                    .build();
            z.setSkolaId(skolaId);
            kreirane.add(zamenaRepository.save(z));
        }
        return kreirane.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ZamenaResponse> zameneDana(LocalDate datum) {
        UUID skolaId = TenantContext.require();
        return zamenaRepository.zameneDana(skolaId, datum).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZamenaResponse> mojeKaoOdsutni(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        return zamenaRepository.mojeKaoOdsutni(skolaId, ja.id()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZamenaResponse> mojeKaoZamenik(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        return zamenaRepository.mojeKaoZamenik(skolaId, ja.id()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KandidatZamenikResponse> predloziKandidate(UUID zamenaId) {
        UUID skolaId = TenantContext.require();
        Zamena z = nadjiUSkoli(zamenaId, skolaId);

        Dan dan = toDan(z.getDatum());
        if (dan == null) return List.of();

        // Skup nastavnika koji su zauzeti u tom terminu (iz aktivne verzije rasporeda).
        Set<UUID> zauzeti = new java.util.HashSet<>(verzijaRepository.findFirstBySkolaIdAndAktivanTrue(skolaId)
                .map(v -> stavkaRepository.zauzetiNastavnici(skolaId, v.getId(), dan, z.getCas()))
                .orElse(List.of()));
        // Plus nastavnici koji su vec dodeljeni kao zamenici za isti termin u drugim zamenama
        zauzeti.addAll(zamenaRepository.zameniciVecPredlozeniZa(skolaId, z.getDatum(), z.getCas()));
        // Odsutni sam sebe ne moze biti zamenik
        zauzeti.add(z.getOdsutni().getId());

        List<Korisnik> svi = korisnikRepository
                .findAllBySkolaIdAndUlogaOrderByPrezimeAscImeAsc(skolaId, Uloga.NASTAVNIK);

        List<Korisnik> slobodni = svi.stream()
                .filter(k -> k.isAktivan() && !zauzeti.contains(k.getId()))
                .toList();
        if (slobodni.isEmpty()) return List.of();

        // Brojanje opterecenja u poslednjih 30 dana
        LocalDate od = z.getDatum().minusDays(PROZOR_OPTERECENJA_DANA);
        List<UUID> ids = slobodni.stream().map(Korisnik::getId).toList();
        Map<UUID, Long> opterecenje = new HashMap<>();
        for (Object[] row : zamenaRepository.brojZamenaPoZameniku(skolaId, od, ids)) {
            opterecenje.put((UUID) row[0], (Long) row[1]);
        }

        return slobodni.stream()
                .map(k -> new KandidatZamenikResponse(
                        k.getId(), k.getUsername(), k.getIme(), k.getPrezime(),
                        opterecenje.getOrDefault(k.getId(), 0L)))
                .sorted(Comparator
                        .comparingLong(KandidatZamenikResponse::brojZamena30d)
                        .thenComparing(KandidatZamenikResponse::prezime, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(KandidatZamenikResponse::ime, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public ZamenaResponse dodeliZamenika(UUID zamenaId, DodeliZamenikaRequest req) {
        UUID skolaId = TenantContext.require();
        Zamena z = nadjiUSkoli(zamenaId, skolaId);

        if (z.getStatus() != ZamenaStatus.PREDLOZENA) {
            throw new ConflictException("Zamenik se moze dodeliti samo dok je status PREDLOZENA");
        }
        Korisnik zamenik = korisnikRepository.findById(req.zamenikId())
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", req.zamenikId()));
        if (zamenik.getSkola() == null || !skolaId.equals(zamenik.getSkola().getId())) {
            throw new TenantViolationException("Predlozeni zamenik nije iz iste skole");
        }
        if (zamenik.getUloga() != Uloga.NASTAVNIK || !zamenik.isAktivan()) {
            throw new ValidationException("Zamenik mora biti aktivan NASTAVNIK");
        }
        if (zamenik.getId().equals(z.getOdsutni().getId())) {
            throw new ValidationException("Odsutni nastavnik ne moze biti svoj zamenik");
        }

        z.setZamenik(zamenik);
        if (req.napomena() != null) z.setNapomena(req.napomena());
        return mapper.toResponse(z);
    }

    @Transactional
    public ZamenaResponse odobri(UUID zamenaId, CustomUserDetails odobrava) {
        UUID skolaId = TenantContext.require();
        Zamena z = nadjiUSkoli(zamenaId, skolaId);
        if (z.getZamenik() == null) {
            throw new ConflictException("Zamenik mora biti dodeljen pre odobrenja");
        }
        promeniStatus(z, ZamenaStatus.ODOBRENA, odobrava);
        return mapper.toResponse(z);
    }

    @Transactional
    public ZamenaResponse odbij(UUID zamenaId, OdbijZamenuRequest req, CustomUserDetails ko) {
        UUID skolaId = TenantContext.require();
        Zamena z = nadjiUSkoli(zamenaId, skolaId);
        z.setNapomena(req.razlog());
        promeniStatus(z, ZamenaStatus.ODBIJENA, ko);
        return mapper.toResponse(z);
    }

    @Transactional
    public ZamenaResponse otkazi(UUID zamenaId, CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        Zamena z = nadjiUSkoli(zamenaId, skolaId);
        if (!ja.id().equals(z.getOdsutni().getId()) && !imaUloguAdminskog(ja)) {
            throw new TenantViolationException("Samo odsutni nastavnik ili admin moze otkazati zamenu");
        }
        promeniStatus(z, ZamenaStatus.OTKAZANA, ja);
        return mapper.toResponse(z);
    }

    private void promeniStatus(Zamena z, ZamenaStatus novi, CustomUserDetails ko) {
        if (!z.getStatus().mozePreci(novi)) {
            throw new ConflictException("Tranzicija " + z.getStatus() + " -> " + novi + " nije dozvoljena");
        }
        z.setStatus(novi);
        if (novi == ZamenaStatus.ODOBRENA || novi == ZamenaStatus.ODBIJENA) {
            Korisnik koJe = korisnikRepository.getReferenceById(ko.id());
            z.setOdobrio(koJe);
            z.setOdobrioAt(OffsetDateTime.now());
        }
    }

    private Map<Short, RasporedStavka> stavkePoCasuZaNastavnika(UUID skolaId, UUID korisnikId, Dan dan) {
        return verzijaRepository.findFirstBySkolaIdAndAktivanTrue(skolaId)
                .map(VerzijaRasporeda::getId)
                .map(vId -> stavkaRepository.casoviNastavnikaPoDanu(skolaId, vId, korisnikId, dan))
                .orElse(List.of()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        RasporedStavka::getCas, s -> s, (a, b) -> a));
    }

    private Zamena nadjiUSkoli(UUID zamenaId, UUID skolaId) {
        Zamena z = zamenaRepository.findById(zamenaId)
                .orElseThrow(() -> new ResourceNotFoundException("Zamena", zamenaId));
        if (!skolaId.equals(z.getSkolaId())) {
            throw new TenantViolationException();
        }
        return z;
    }

    private boolean imaUloguAdminskog(CustomUserDetails ja) {
        return ja.uloga() == Uloga.ADMIN || ja.uloga() == Uloga.DIREKTOR || ja.uloga() == Uloga.KOORDINATOR;
    }

    private void validirajCas(Short cas) {
        if (cas == null || cas < 1 || cas > 8) {
            throw new ValidationException("Cas mora biti izmedju 1 i 8");
        }
    }

    /** Vraca {@code null} za nedelju — toga dana se ne odrzava nastava. */
    static Dan toDan(LocalDate datum) {
        DayOfWeek dow = datum.getDayOfWeek();
        return switch (dow) {
            case MONDAY -> Dan.PONEDELJAK;
            case TUESDAY -> Dan.UTORAK;
            case WEDNESDAY -> Dan.SREDA;
            case THURSDAY -> Dan.CETVRTAK;
            case FRIDAY -> Dan.PETAK;
            case SATURDAY -> Dan.SUBOTA;
            case SUNDAY -> null;
        };
    }
}
