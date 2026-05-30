package rs.skola.platforma.planovi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.katalog.KatalogService;
import rs.skola.platforma.katalog.domain.Ishod;
import rs.skola.platforma.katalog.domain.MetodaRada;
import rs.skola.platforma.katalog.domain.NastavnaJedinica;
import rs.skola.platforma.katalog.domain.Tema;
import rs.skola.platforma.katalog.domain.TipCasa;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.planovi.domain.OpStavka;
import rs.skola.platforma.planovi.domain.OpStavkaMedjupredmetno;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.planovi.domain.PlanStatus;
import rs.skola.platforma.planovi.repo.OperativniPlanRepository;
import rs.skola.platforma.planovi.web.KreirajOperativniPlanRequest;
import rs.skola.platforma.planovi.web.OperativniPlanResponse;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperativniPlanService {

    private final OperativniPlanRepository planRepo;
    private final KorisnikRepository korisnikRepo;
    private final PredmetRepository predmetRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final KatalogService katalogService;
    private final OperativniPlanIsporukaService isporukaService;

    @Transactional
    public OperativniPlanResponse kreirajIliAzuriraj(CustomUserDetails ja, KreirajOperativniPlanRequest req) {
        UUID skolaId = TenantContext.require();
        Korisnik nastavnik = korisnikRepo.findById(ja.id())
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", ja.id()));
        Predmet predmet = nadjiPredmet(req.predmetId(), skolaId);
        Odeljenje odeljenje = nadjiOdeljenje(req.odeljenjeId(), skolaId);

        OperativniPlan plan = planRepo
                .findBySkolaIdAndNastavnik_IdAndPredmet_IdAndOdeljenje_IdAndMesecAndSkolskaGodina(
                        skolaId, ja.id(), predmet.getId(), odeljenje.getId(),
                        req.mesec(), req.skolskaGodina())
                .orElseGet(() -> {
                    OperativniPlan nov = OperativniPlan.builder()
                            .nastavnik(nastavnik)
                            .predmet(predmet)
                            .odeljenje(odeljenje)
                            .mesec(req.mesec())
                            .skolskaGodina(req.skolskaGodina())
                            .status(PlanStatus.NACRT)
                            .build();
                    nov.setSkolaId(skolaId);
                    return nov;
                });
        if (plan.getStatus() == PlanStatus.ARHIVIRAN) {
            throw new ConflictException("Arhiviran plan se ne moze menjati");
        }
        plan.setNedeljniFond(req.nedeljniFond());
        plan.setSamoprocenaIshoda(req.samoprocenaIshoda());
        plan.setNapomene(req.napomene());

        // Brisemo stare stavke pre dodavanja novih + flush, da Hibernate ne pokrene
        // INSERT pre DELETE-a (UNIQUE constraint na (plan, redniBrojCasa) bi pucao).
        plan.getStavke().clear();
        planRepo.saveAndFlush(plan);

        Set<Short> rbCasa = new HashSet<>();
        for (KreirajOperativniPlanRequest.StavkaCasaRequest s : req.stavke()) {
            if (!rbCasa.add(s.redniBrojCasa())) {
                throw new ValidationException("Duplicirani redni broj casa: " + s.redniBrojCasa());
            }
            plan.getStavke().add(napraviStavku(plan, s, predmet, skolaId));
        }

        plan = planRepo.save(plan);
        UUID planId = plan.getId();
        // Async isporuka mora da pocne TEK NAKON commit-a — inace async thread otvori
        // novu konekciju i ne vidi plan koji jos uvek nije committed.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                isporukaService.isporuciAsinhrono(planId);
            }
        });
        return toResponse(plan);
    }

    @Transactional
    public OperativniPlanResponse podnesi(UUID planId, CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        OperativniPlan plan = nadji(planId, skolaId);
        if (!plan.getNastavnik().getId().equals(ja.id())) {
            throw new TenantViolationException("Plan moze podneti samo vlasnik");
        }
        if (plan.getStatus() == PlanStatus.ARHIVIRAN) {
            throw new ConflictException("Arhiviran plan se ne moze podneti");
        }
        plan.setStatus(PlanStatus.PODNET);
        plan.setPodnetAt(OffsetDateTime.now());
        return toResponse(plan);
    }

    @Transactional
    public OperativniPlanResponse kloniraj(UUID izvornikId, String novaSkolskaGodina, CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        OperativniPlan izvor = planRepo.findByIdSaStavkama(izvornikId)
                .orElseThrow(() -> new ResourceNotFoundException("Operativni plan", izvornikId));
        if (!skolaId.equals(izvor.getSkolaId())) {
            throw new TenantViolationException();
        }

        // Postojeci u novoj godini? Ne kloniramo preko njega.
        planRepo.findBySkolaIdAndNastavnik_IdAndPredmet_IdAndOdeljenje_IdAndMesecAndSkolskaGodina(
                skolaId, ja.id(), izvor.getPredmet().getId(), izvor.getOdeljenje().getId(),
                izvor.getMesec(), novaSkolskaGodina
        ).ifPresent(p -> {
            throw new ConflictException("Plan za isti predmet/odeljenje/mesec u %s vec postoji"
                    .formatted(novaSkolskaGodina));
        });

        OperativniPlan kopija = OperativniPlan.builder()
                .nastavnik(korisnikRepo.getReferenceById(ja.id()))
                .predmet(izvor.getPredmet())
                .odeljenje(izvor.getOdeljenje())
                .mesec(izvor.getMesec())
                .skolskaGodina(novaSkolskaGodina)
                .nedeljniFond(izvor.getNedeljniFond())
                .samoprocenaIshoda(null)
                .napomene("Klonirano iz " + izvor.getSkolskaGodina())
                .status(PlanStatus.NACRT)
                .build();
        kopija.setSkolaId(skolaId);

        for (OpStavka s : izvor.getStavke()) {
            OpStavka nova = OpStavka.builder()
                    .operativniPlan(kopija)
                    .redniBrojCasa(s.getRedniBrojCasa())
                    .tema(s.getTema())
                    .nastavnaJedinica(s.getNastavnaJedinica())
                    .tipCasa(s.getTipCasa())
                    .metodaRada(s.getMetodaRada())
                    .evaluacija(null) // evaluacija je rezultat realizacije; ne kloniramo
                    .ishodi(new HashSet<>(s.getIshodi()))
                    .build();
            // medjupredmetno se klonira kasnije, posle save
            kopija.getStavke().add(nova);
        }
        kopija = planRepo.save(kopija);
        UUID novId = kopija.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                isporukaService.isporuciAsinhrono(novId);
            }
        });
        return toResponse(kopija);
    }

    @Transactional(readOnly = true)
    public List<OperativniPlanResponse> mojiPlanovi(CustomUserDetails ja, Short mesec, UUID predmetId, String skolskaGodina) {
        UUID skolaId = TenantContext.require();
        return planRepo.mojiPlanovi(skolaId, ja.id(), mesec, predmetId, skolskaGodina).stream()
                .map(this::toResponseLite)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperativniPlanResponse> sviZaSkolu(String skolskaGodina, Short mesec, UUID nastavnikId,
                                                    UUID predmetId, UUID odeljenjeId, PlanStatus status) {
        UUID skolaId = TenantContext.require();
        return planRepo.sviZaSkolu(skolaId, skolskaGodina, mesec, nastavnikId, predmetId, odeljenjeId, status).stream()
                .map(this::toResponseLite)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperativniPlanResponse pregled(UUID planId) {
        UUID skolaId = TenantContext.require();
        OperativniPlan plan = planRepo.findByIdSaStavkama(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Operativni plan", planId));
        if (!skolaId.equals(plan.getSkolaId())) {
            throw new TenantViolationException();
        }
        return toResponse(plan);
    }

    @Transactional
    public void obrisi(UUID planId) {
        UUID skolaId = TenantContext.require();
        OperativniPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Operativni plan", planId));
        if (!skolaId.equals(plan.getSkolaId())) {
            throw new TenantViolationException();
        }
        planRepo.delete(plan);
    }

    // -------- helpers --------

    private OpStavka napraviStavku(OperativniPlan plan, KreirajOperativniPlanRequest.StavkaCasaRequest s,
                                    Predmet predmet, UUID skolaId) {
        Tema tema;
        if (s.temaId() != null) {
            tema = katalogService.nadjiTemu(s.temaId());
            if (!tema.getPredmet().getId().equals(predmet.getId())) {
                throw new ValidationException("Tema ne pripada izabranom predmetu");
            }
        } else if (s.nazivTeme() != null && !s.nazivTeme().isBlank()) {
            tema = katalogService.findOrCreateTema(predmet.getId(), s.nazivTeme(), null, null, null, null);
        } else {
            throw new ValidationException("Stavka mora imati temaId ili nazivTeme");
        }

        NastavnaJedinica jedinica = null;
        if (s.nastavnaJedinicaId() != null) {
            jedinica = katalogService.findOrCreateJedinica(tema.getId(), null, null);
            // Nadji konkretnu kroz katalogService bi bilo cleaner; ovde reuse findOrCreate ali to
            // bi kreiralo praznu — prebacimo direktno:
            jedinica = null;
        }
        if (s.nastavnaJedinicaId() != null) {
            jedinica = katalogService.findOrCreateJedinica(tema.getId(),
                    s.nazivJedinice() == null ? "" : s.nazivJedinice(), null);
        } else if (s.nazivJedinice() != null && !s.nazivJedinice().isBlank()) {
            jedinica = katalogService.findOrCreateJedinica(tema.getId(), s.nazivJedinice(), null);
        }

        TipCasa tip = katalogService.nadjiTipCasa(s.tipCasaId());
        MetodaRada metoda = s.metodaRadaId() == null ? null : katalogService.nadjiMetodu(s.metodaRadaId());

        Set<Ishod> ishodi = new HashSet<>();
        if (s.ishodiIds() != null) {
            for (UUID id : s.ishodiIds()) ishodi.add(katalogService.nadjiIshod(id));
        }
        if (s.noviIshodi() != null) {
            for (String opis : s.noviIshodi()) {
                if (opis != null && !opis.isBlank()) {
                    ishodi.add(katalogService.kreirajIshod(tema.getId(), opis));
                }
            }
        }

        OpStavka stavka = OpStavka.builder()
                .operativniPlan(plan)
                .redniBrojCasa(s.redniBrojCasa())
                .tema(tema)
                .nastavnaJedinica(jedinica)
                .tipCasa(tip)
                .metodaRada(metoda)
                .evaluacija(s.evaluacija())
                .ishodi(ishodi)
                .build();

        if (s.medjupredmetno() != null) {
            for (KreirajOperativniPlanRequest.MedjupredmetnoRequest mp : s.medjupredmetno()) {
                Predmet drugiPredmet = nadjiPredmet(mp.predmetId(), skolaId);
                OpStavkaMedjupredmetno veza = OpStavkaMedjupredmetno.builder()
                        .opStavka(stavka)
                        .predmet(drugiPredmet)
                        .opisKompetencije(mp.opisKompetencije())
                        .build();
                stavka.getMedjupredmetno().add(veza);
            }
        }
        return stavka;
    }

    private Predmet nadjiPredmet(UUID id, UUID skolaId) {
        Predmet p = predmetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Predmet", id));
        if (!skolaId.equals(p.getSkolaId())) {
            throw new TenantViolationException("Predmet ne pripada vasoj skoli");
        }
        return p;
    }

    private Odeljenje nadjiOdeljenje(UUID id, UUID skolaId) {
        Odeljenje o = odeljenjeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odeljenje", id));
        if (!skolaId.equals(o.getSkolaId())) {
            throw new TenantViolationException("Odeljenje ne pripada vasoj skoli");
        }
        return o;
    }

    private OperativniPlan nadji(UUID id, UUID skolaId) {
        OperativniPlan p = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operativni plan", id));
        if (!skolaId.equals(p.getSkolaId())) {
            throw new TenantViolationException();
        }
        return p;
    }

    private OperativniPlanResponse toResponse(OperativniPlan p) {
        List<OperativniPlanResponse.StavkaResponse> stavke = p.getStavke().stream()
                .map(s -> new OperativniPlanResponse.StavkaResponse(
                        s.getId(),
                        s.getRedniBrojCasa(),
                        s.getTema() == null ? null : s.getTema().getId(),
                        s.getTema() == null ? null : s.getTema().getNaziv(),
                        s.getNastavnaJedinica() == null ? null : s.getNastavnaJedinica().getId(),
                        s.getNastavnaJedinica() == null ? null : s.getNastavnaJedinica().getNaziv(),
                        s.getTipCasa() == null ? null : s.getTipCasa().getId(),
                        s.getTipCasa() == null ? null : s.getTipCasa().getNaziv(),
                        s.getMetodaRada() == null ? null : s.getMetodaRada().getId(),
                        s.getMetodaRada() == null ? null : s.getMetodaRada().getNaziv(),
                        s.getEvaluacija(),
                        s.getIshodi().stream()
                                .map(i -> new OperativniPlanResponse.IshodKratko(i.getId(), i.getOpis()))
                                .toList(),
                        s.getMedjupredmetno().stream()
                                .map(mp -> new OperativniPlanResponse.MedjupredmetnoKratko(
                                        mp.getId(),
                                        mp.getPredmet().getId(),
                                        mp.getPredmet().getNaziv(),
                                        mp.getOpisKompetencije()))
                                .toList()
                ))
                .toList();
        return baseResponse(p, stavke);
    }

    private OperativniPlanResponse toResponseLite(OperativniPlan p) {
        return baseResponse(p, List.of());
    }

    private OperativniPlanResponse baseResponse(OperativniPlan p, List<OperativniPlanResponse.StavkaResponse> stavke) {
        return new OperativniPlanResponse(
                p.getId(),
                p.getNastavnik().getId(),
                p.getNastavnik().punoIme(),
                p.getPredmet().getId(),
                p.getPredmet().getNaziv(),
                p.getOdeljenje().getId(),
                p.getOdeljenje().label(),
                p.getMesec(),
                p.getSkolskaGodina(),
                p.getNedeljniFond(),
                p.getSamoprocenaIshoda(),
                p.getNapomene(),
                p.getStatus(),
                p.getPodnetAt(),
                p.getWordFajlPutanja() != null,
                p.getPdfFajlPutanja() != null,
                stavke,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
