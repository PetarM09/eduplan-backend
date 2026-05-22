package rs.skola.platforma.planovi;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import rs.skola.platforma.AbstractIntegrationTest;
import rs.skola.platforma.TestDataSeeder;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.katalog.repo.IshodRepository;
import rs.skola.platforma.katalog.repo.TemaRepository;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.planovi.domain.PlanStatus;
import rs.skola.platforma.planovi.repo.GodisnjiPlanRepository;
import rs.skola.platforma.planovi.web.GodisnjiPlanResponse;
import rs.skola.platforma.planovi.web.KreirajGodisnjiPlanRequest;
import rs.skola.platforma.predmeti.PredmetService;
import rs.skola.platforma.predmeti.web.KreirajPredmetRequest;
import rs.skola.platforma.predmeti.web.PredmetResponse;
import rs.skola.platforma.tenant.domain.Skola;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end test godisnjeg plana: kreira plan sa 2 teme i 3 ishoda → verifikuje da su
 * teme/ishodi automatski upisani u katalog → ceka async pipeline (Word + PDF + mail) →
 * verifikuje putanje u bazi i da je {@code mailSender.send} pozvan tacno jednom.
 */
class GodisnjiPlanIT extends AbstractIntegrationTest {

    @Autowired GodisnjiPlanService godisnjiService;
    @Autowired GodisnjiPlanRepository planRepo;
    @Autowired PredmetService predmetService;
    @Autowired TemaRepository temaRepo;
    @Autowired IshodRepository ishodRepo;
    @Autowired TestDataSeeder seeder;
    // mailSender mock dolazi iz AbstractIntegrationTest (deljen kontekst preko svih IT)

    @Test
    @DisplayName("Plan kreiran sa novim temama i ishodima → katalog dobija teme i ishode, async pipeline generise Word/PDF, mail je poslat")
    void kreirajPlan_endToEnd() {
        Skola skola = seeder.kreirajSkolu("GodPlanIT-1");
        Korisnik nastavnik = seeder.kreirajKorisnika(skola, Uloga.NASTAVNIK);
        PredmetResponse predmet;
        GodisnjiPlanResponse plan;
        TenantContext.set(skola.getId());
        try {
            predmet = predmetService.kreiraj(new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));

            CustomUserDetails ja = principalOf(nastavnik);
            plan = godisnjiService.kreirajIliAzuriraj(ja, new KreirajGodisnjiPlanRequest(
                    predmet.id(), "2024/2025", (short) 3, List.of(),
                    "Ciljevi i zadaci", "Udzbenik X", "M. Markovic", "Literatura Y",
                    (short) 72, (short) 2, "Dopunski", "Dodatni", null,
                    List.of(
                            new KreirajGodisnjiPlanRequest.StavkaTemeRequest(
                                    null, "Uvod u mreze", (short) 1,
                                    (short) 8, (short) 4, (short) 0, (short) 12,
                                    List.of("IX", "X"),
                                    null,
                                    List.of("Ucenik razume mrezu", "Ucenik nabraja tipove")
                            ),
                            new KreirajGodisnjiPlanRequest.StavkaTemeRequest(
                                    null, "Mrezni protokoli", (short) 2,
                                    (short) 10, (short) 6, (short) 0, (short) 16,
                                    List.of("XI", "XII", "I"),
                                    null,
                                    List.of("Ucenik objasnjava TCP/IP")
                            )
                    )
            ));
        } finally {
            TenantContext.clear();
        }

        // 1. Plan je u bazi sa NACRT statusom — koristimo findByIdSaTemama za eager fetch
        var iz = planRepo.findByIdSaTemama(plan.id()).orElseThrow();
        assertThat(iz.getStatus()).isEqualTo(PlanStatus.NACRT);
        assertThat(iz.getNastavnik().getId()).isEqualTo(nastavnik.getId());
        assertThat(iz.getPredmet().getId()).isEqualTo(predmet.id());
        assertThat(iz.getTeme()).hasSize(2);

        // 2. Auto-save: katalog ima obe teme i sva 3 ishoda vezana za predmet
        var temeUKatalogu = temaRepo.findAllBySkolaIdAndPredmet_IdOrderByRedniBrojAscNazivAsc(
                skola.getId(), predmet.id());
        assertThat(temeUKatalogu).extracting("naziv")
                .containsExactlyInAnyOrder("Uvod u mreze", "Mrezni protokoli");
        long ukupnoIshoda = temeUKatalogu.stream()
                .mapToLong(t -> ishodRepo.findAllBySkolaIdAndTema_IdOrderByCreatedAtAsc(skola.getId(), t.getId()).size())
                .sum();
        assertThat(ukupnoIshoda).isEqualTo(3);

        // 3. Asinhroni pipeline: Word/PDF putanje su upisane u bazi
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var azurirano = planRepo.findById(plan.id()).orElseThrow();
            assertThat(azurirano.getWordFajlPutanja()).isNotNull();
            assertThat(azurirano.getPdfFajlPutanja()).isNotNull();
        });

        // 4. Mail je poslat tacno jednom
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                Mockito.verify(mailSender, Mockito.times(1)).send(Mockito.any(MimeMessage.class)));
    }

    @Test
    @DisplayName("Drugi POST sa istim (predmet, godina, nastavnik) → idempotent ažuriranje istog plana")
    void drugiPost_idempotent() {
        Skola skola = seeder.kreirajSkolu("GodPlanIT-Idem");
        Korisnik nastavnik = seeder.kreirajKorisnika(skola, Uloga.NASTAVNIK);

        TenantContext.set(skola.getId());
        try {
            PredmetResponse predmet = predmetService.kreiraj(new KreirajPredmetRequest("OOP", (short) 3, (short) 2));
            CustomUserDetails ja = principalOf(nastavnik);

            var req1 = new KreirajGodisnjiPlanRequest(
                    predmet.id(), "2024/2025", (short) 3, List.of(),
                    "Prva verzija", null, null, null, (short) 36, (short) 1, null, null, null,
                    List.of(new KreirajGodisnjiPlanRequest.StavkaTemeRequest(
                            null, "Uvod", (short) 1, (short) 4, (short) 2, (short) 0, (short) 6,
                            List.of("IX"), null, List.of("Razume osnove")
                    ))
            );
            var prvi = godisnjiService.kreirajIliAzuriraj(ja, req1);

            var req2 = new KreirajGodisnjiPlanRequest(
                    predmet.id(), "2024/2025", (short) 3, List.of(),
                    "Druga verzija", null, null, null, (short) 72, (short) 2, null, null, null,
                    List.of(new KreirajGodisnjiPlanRequest.StavkaTemeRequest(
                            null, "Uvod", (short) 1, (short) 8, (short) 4, (short) 0, (short) 12,
                            List.of("IX", "X"), null, List.of("Razume osnove")
                    ))
            );
            var drugi = godisnjiService.kreirajIliAzuriraj(ja, req2);

            assertThat(drugi.id()).isEqualTo(prvi.id());
            assertThat(drugi.ciljeviZadaci()).isEqualTo("Druga verzija");
            assertThat(drugi.godisnjiFond()).isEqualTo((short) 72);
        } finally {
            TenantContext.clear();
        }
    }

    private CustomUserDetails principalOf(Korisnik k) {
        return new CustomUserDetails(
                k.getId(),
                k.getSkola() == null ? null : k.getSkola().getId(),
                k.getUsername(),
                k.getLozinkaHash(),
                k.getEmail(),
                k.getUloga(),
                k.isAktivan()
        );
    }
}
