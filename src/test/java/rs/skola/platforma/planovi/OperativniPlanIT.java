package rs.skola.platforma.planovi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import rs.skola.platforma.AbstractIntegrationTest;
import rs.skola.platforma.TestDataSeeder;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.katalog.KatalogService;
import rs.skola.platforma.katalog.repo.NastavnaJedinicaRepository;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.planovi.repo.OperativniPlanRepository;
import rs.skola.platforma.planovi.web.KreirajOperativniPlanRequest;
import rs.skola.platforma.planovi.web.OperativniPlanResponse;
import rs.skola.platforma.predmeti.PredmetService;
import rs.skola.platforma.predmeti.web.KreirajPredmetRequest;
import rs.skola.platforma.predmeti.web.PredmetResponse;
import rs.skola.platforma.tenant.domain.Skola;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
class OperativniPlanIT extends AbstractIntegrationTest {

    @Autowired OperativniPlanService operativniService;
    @Autowired OperativniPlanRepository planRepo;
    @Autowired PredmetService predmetService;
    @Autowired KatalogService katalogService;
    @Autowired NastavnaJedinicaRepository jedinicaRepo;
    @Autowired TestDataSeeder seeder;
    // mailSender mock dolazi iz AbstractIntegrationTest

    @Test
    @DisplayName("Operativni plan: 2 časa, ista tema, dve različite jedinice → 1 tema + 2 jedinice u katalogu")
    void kreirajPlan_autoSaveJedinica() {
        Skola skola = seeder.kreirajSkolu("OperIT-1");
        Korisnik nastavnik = seeder.kreirajKorisnika(skola, Uloga.NASTAVNIK);
        Odeljenje odelj = seeder.kreirajOdeljenje(skola, (short) 3, "1", nastavnik);

        PredmetResponse predmet;
        OperativniPlanResponse plan;
        TenantContext.set(skola.getId());
        try {
            predmet = predmetService.kreiraj(new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));
            var tipovi = katalogService.tipoviCasa();
            var metode = katalogService.metodeRada();
            var obrada = tipovi.stream().filter(t -> t.naziv().equals("Obrada")).findFirst().orElseThrow();
            var predavanje = metode.stream().filter(m -> m.naziv().equals("Predavanje")).findFirst().orElseThrow();

            CustomUserDetails ja = principalOf(nastavnik);
            plan = operativniService.kreirajIliAzuriraj(ja, new KreirajOperativniPlanRequest(
                    predmet.id(), odelj.getId(), (short) 9, "2024/2025", (short) 2,
                    "Samoprocena", null,
                    List.of(
                            new KreirajOperativniPlanRequest.StavkaCasaRequest(
                                    (short) 1, null, "Uvod u mreze",
                                    null, "Sta je mreza",
                                    obrada.id(), predavanje.id(),
                                    null, List.of("Razume mrezu"), null, "Usmena provera"),
                            new KreirajOperativniPlanRequest.StavkaCasaRequest(
                                    (short) 2, null, "Uvod u mreze",
                                    null, "Tipovi mreza",
                                    obrada.id(), predavanje.id(),
                                    null, List.of("Razlikuje LAN/WAN"), null, "")
                    )
            ));
        } finally {
            TenantContext.clear();
        }

        // 1. Plan ima 2 stavke
        assertThat(plan.stavke()).hasSize(2);

        // 2. Auto-save: ista tema reuse-ovana, 2 jedinice kreirane
        TenantContext.set(skola.getId());
        try {
            var teme = katalogService.temePredmeta(predmet.id());
            assertThat(teme).hasSize(1); // findOrCreate je reuse-ovao istu temu
            var jedinice = jedinicaRepo.findAllBySkolaIdAndTema_IdOrderByRedniBrojAscNazivAsc(
                    skola.getId(), teme.get(0).id());
            assertThat(jedinice).extracting("naziv")
                    .containsExactlyInAnyOrder("Sta je mreza", "Tipovi mreza");
        } finally {
            TenantContext.clear();
        }

        // 3. Async pipeline: Word/PDF generisani
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var azurirano = planRepo.findById(plan.id()).orElseThrow();
            assertThat(azurirano.getWordFajlPutanja()).isNotNull();
            assertThat(azurirano.getPdfFajlPutanja()).isNotNull();
        });
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
