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
import rs.skola.platforma.katalog.domain.Tema;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.GodisnjiPlanTema;
import rs.skola.platforma.planovi.domain.PlanStatus;
import rs.skola.platforma.planovi.repo.GodisnjiPlanRepository;
import rs.skola.platforma.planovi.web.GodisnjiPlanResponse;
import rs.skola.platforma.planovi.web.KreirajGodisnjiPlanRequest;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GodisnjiPlanService {

    private static final List<String> SVI_MESECI = List.of(
            "IX", "X", "XI", "XII", "I", "II", "III", "IV", "V", "VI");

    private final GodisnjiPlanRepository planRepo;
    private final KorisnikRepository korisnikRepo;
    private final PredmetRepository predmetRepo;
    private final KatalogService katalogService;
    private final PlanIsporukaService isporukaService;

    @Transactional
    public GodisnjiPlanResponse kreirajIliAzuriraj(CustomUserDetails ja, KreirajGodisnjiPlanRequest req) {
        UUID skolaId = TenantContext.require();
        Korisnik nastavnik = korisnikRepo.findById(ja.id())
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", ja.id()));
        Predmet predmet = predmetRepo.findById(req.predmetId())
                .orElseThrow(() -> new ResourceNotFoundException("Predmet", req.predmetId()));
        if (!skolaId.equals(predmet.getSkolaId())) {
            throw new TenantViolationException("Predmet ne pripada vasoj skoli");
        }

        // Postojeci plan za isti predmet/godinu/nastavnika se azurira; inace kreiramo novi.
        GodisnjiPlan plan = planRepo
                .findBySkolaIdAndNastavnik_IdAndPredmet_IdAndSkolskaGodina(
                        skolaId, ja.id(), predmet.getId(), req.skolskaGodina())
                .orElseGet(() -> {
                    GodisnjiPlan nov = GodisnjiPlan.builder()
                            .nastavnik(nastavnik)
                            .predmet(predmet)
                            .skolskaGodina(req.skolskaGodina())
                            .status(PlanStatus.NACRT)
                            .build();
                    nov.setSkolaId(skolaId);
                    return nov;
                });
        if (plan.getStatus() == PlanStatus.ARHIVIRAN) {
            throw new ConflictException("Arhiviran plan se ne moze menjati");
        }

        plan.setRazred(req.razred());
        plan.setOdeljenjaIds(req.odeljenjaIds() == null ? List.of() : req.odeljenjaIds());
        plan.setCiljeviZadaci(req.ciljeviZadaci());
        plan.setUdzebenik(req.udzebenik());
        plan.setAutori(req.autori());
        plan.setLiteratura(req.literatura());
        plan.setGodisnjiFond(req.godisnjiFond());
        plan.setNedeljniFond(req.nedeljniFond());
        plan.setDopunskiRad(req.dopunskiRad());
        plan.setDodatniRad(req.dodatniRad());
        plan.setNapomene(req.napomene());

        // Brisemo stare veze pre dodavanja novih i flush-ujemo da Hibernate ne pokusa
        // INSERT pre DELETE-a (sto bi pokrenulo UNIQUE constraint violation pri idempotent re-save-u).
        plan.getTeme().clear();
        planRepo.saveAndFlush(plan);

        for (KreirajGodisnjiPlanRequest.StavkaTemeRequest stavka : req.teme()) {
            GodisnjiPlanTema gpt = prepraviStavku(predmet.getId(), plan, stavka);
            plan.getTeme().add(gpt);
        }

        plan = planRepo.save(plan);
        UUID planId = plan.getId();
        // Async isporuka tek nakon commit-a — inace bi @Async thread otvorio novu
        // konekciju i ne bi video plan (test/integracioni scenario fail-uje).
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                isporukaService.isporuciAsinhrono(planId);
            }
        });
        return toResponse(plan);
    }

    @Transactional
    public GodisnjiPlanResponse podnesi(UUID planId, CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        GodisnjiPlan plan = nadji(planId, skolaId);
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

    @Transactional(readOnly = true)
    public List<GodisnjiPlanResponse> mojiPlanovi(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        return planRepo.mojiPlanovi(skolaId, ja.id()).stream()
                .map(this::toResponseLite)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GodisnjiPlanResponse> sviZaSkolu(String skolskaGodina, PlanStatus status) {
        UUID skolaId = TenantContext.require();
        return planRepo.sviZaSkolu(skolaId, skolskaGodina, status).stream()
                .map(this::toResponseLite)
                .toList();
    }

    @Transactional(readOnly = true)
    public GodisnjiPlanResponse pregled(UUID planId) {
        UUID skolaId = TenantContext.require();
        GodisnjiPlan plan = nadji(planId, skolaId);
        return toResponse(plan);
    }

    // -------- helpers --------

    private GodisnjiPlanTema prepraviStavku(UUID predmetId, GodisnjiPlan plan,
                                            KreirajGodisnjiPlanRequest.StavkaTemeRequest s) {
        Tema tema;
        if (s.temaId() != null) {
            tema = katalogService.nadjiTemu(s.temaId());
            if (!tema.getPredmet().getId().equals(predmetId)) {
                throw new ValidationException("Tema ne pripada izabranom predmetu");
            }
        } else if (s.nazivTeme() != null && !s.nazivTeme().isBlank()) {
            tema = katalogService.findOrCreateTema(predmetId, s.nazivTeme(), s.redniBroj(),
                    s.casObrada(), s.casUtvrd(), s.casOstalo());
        } else {
            throw new ValidationException("Stavka mora imati ili temaId ili nazivTeme");
        }

        // Auto-save novih ishoda
        List<Ishod> ishodi = new ArrayList<>();
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

        Map<String, Boolean> meseci = new LinkedHashMap<>();
        for (String m : SVI_MESECI) meseci.put(m, false);
        if (s.meseci() != null) {
            for (String m : s.meseci()) {
                if (meseci.containsKey(m)) meseci.put(m, true);
            }
        }

        GodisnjiPlanTema gpt = GodisnjiPlanTema.builder()
                .godisnjiPlan(plan)
                .tema(tema)
                .redniBroj(s.redniBroj() == null ? 0 : s.redniBroj())
                .meseci(meseci)
                .casObrada(s.casObrada() == null ? 0 : s.casObrada())
                .casUtvrd(s.casUtvrd() == null ? 0 : s.casUtvrd())
                .casOstalo(s.casOstalo() == null ? 0 : s.casOstalo())
                .ukupnoCasova(s.ukupnoCasova() == null ? 0 : s.ukupnoCasova())
                .build();
        // Custom map za ishode jer godisnji_plan_teme nije mappiran sa ishodima direktno,
        // vec je veza preko teme (ishodi su teme-level u katalogu). Ako se zahteva da plan
        // referencira tacno odabrane ishode, dodajemo to u Sprintu 4 (operativni plan).
        // Za sad: ishodi su sacuvani u katalog kao posledica, samim cuvanjem plana.
        return gpt;
    }

    private GodisnjiPlan nadji(UUID id, UUID skolaId) {
        GodisnjiPlan p = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Godisnji plan", id));
        if (!skolaId.equals(p.getSkolaId())) {
            throw new TenantViolationException();
        }
        return p;
    }

    private GodisnjiPlanResponse toResponse(GodisnjiPlan p) {
        List<GodisnjiPlanResponse.TemaResponse> teme = p.getTeme().stream()
                .map(t -> new GodisnjiPlanResponse.TemaResponse(
                        t.getId(),
                        t.getTema().getId(),
                        t.getTema().getNaziv(),
                        t.getRedniBroj(),
                        t.getMeseci(),
                        t.getCasObrada(),
                        t.getCasUtvrd(),
                        t.getCasOstalo(),
                        t.getUkupnoCasova(),
                        List.of()
                ))
                .toList();
        return baseResponse(p, teme);
    }

    private GodisnjiPlanResponse toResponseLite(GodisnjiPlan p) {
        return baseResponse(p, List.of());
    }

    private GodisnjiPlanResponse baseResponse(GodisnjiPlan p, List<GodisnjiPlanResponse.TemaResponse> teme) {
        return new GodisnjiPlanResponse(
                p.getId(),
                p.getNastavnik().getId(),
                p.getNastavnik().punoIme(),
                p.getPredmet().getId(),
                p.getPredmet().getNaziv(),
                p.getRazred(),
                p.getSkolskaGodina(),
                p.getOdeljenjaIds() == null ? List.of() : p.getOdeljenjaIds(),
                p.getCiljeviZadaci(),
                p.getUdzebenik(),
                p.getAutori(),
                p.getLiteratura(),
                p.getGodisnjiFond(),
                p.getNedeljniFond(),
                p.getDopunskiRad(),
                p.getDodatniRad(),
                p.getNapomene(),
                p.getStatus(),
                p.getPodnetAt(),
                p.getWordFajlPutanja() != null,
                p.getPdfFajlPutanja() != null,
                teme,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
