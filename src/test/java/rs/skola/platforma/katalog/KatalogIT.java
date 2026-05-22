package rs.skola.platforma.katalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import rs.skola.platforma.AbstractIntegrationTest;
import rs.skola.platforma.TestDataSeeder;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.katalog.domain.Tema;
import rs.skola.platforma.katalog.repo.TemaRepository;
import rs.skola.platforma.predmeti.PredmetService;
import rs.skola.platforma.predmeti.web.KreirajPredmetRequest;
import rs.skola.platforma.predmeti.web.PredmetResponse;
import rs.skola.platforma.tenant.domain.Skola;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifikuje {@code findOrCreate} pattern KatalogService-a — kljucan deo Sprint 3
 * koji eliminise duplikate u katalogu i automatski izgradjuje "biblioteku znanja" skole.
 */
class KatalogIT extends AbstractIntegrationTest {

    @Autowired KatalogService katalogService;
    @Autowired PredmetService predmetService;
    @Autowired TemaRepository temaRepo;
    @Autowired TestDataSeeder seeder;

    @Test
    @DisplayName("findOrCreateTema: dva poziva sa istim nazivom vracaju ISTI entitet (UNIQUE radi)")
    void findOrCreateTema_drugiPoziv_reuse() {
        Skola skola = seeder.kreirajSkolu("KatalogIT-Reuse");
        TenantContext.set(skola.getId());
        try {
            PredmetResponse predmet = predmetService.kreiraj(
                    new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));
            String naziv = "Uvod u racunarske mreze";

            Tema prva = katalogService.findOrCreateTema(predmet.id(), naziv, (short) 1, (short) 4, (short) 2, (short) 0);
            Tema druga = katalogService.findOrCreateTema(predmet.id(), naziv, (short) 1, (short) 4, (short) 2, (short) 0);

            assertThat(druga.getId()).isEqualTo(prva.getId());
            // U bazi je samo jedan zapis za tu kombinaciju
            assertThat(temaRepo.findAllBySkolaIdAndPredmet_IdOrderByRedniBrojAscNazivAsc(
                    skola.getId(), predmet.id()))
                    .hasSize(1)
                    .extracting(Tema::getNaziv)
                    .containsExactly(naziv);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("findOrCreateTema: ista skola, isti naziv, ali DRUGI predmet → kreira novu temu")
    void findOrCreateTema_drugiPredmet_kreiraNovu() {
        Skola skola = seeder.kreirajSkolu("KatalogIT-2Pred");
        TenantContext.set(skola.getId());
        try {
            PredmetResponse predmet1 = predmetService.kreiraj(
                    new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));
            PredmetResponse predmet2 = predmetService.kreiraj(
                    new KreirajPredmetRequest("OOP", (short) 3, (short) 2));
            String naziv = "Uvod";

            Tema t1 = katalogService.findOrCreateTema(predmet1.id(), naziv, (short) 1, null, null, null);
            Tema t2 = katalogService.findOrCreateTema(predmet2.id(), naziv, (short) 1, null, null, null);

            assertThat(t1.getId()).isNotEqualTo(t2.getId());
            assertThat(t1.getPredmet().getId()).isEqualTo(predmet1.id());
            assertThat(t2.getPredmet().getId()).isEqualTo(predmet2.id());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("findOrCreate je case-insensitive — 'UVOD' i 'uvod' su ista tema")
    void findOrCreateTema_caseInsensitive() {
        Skola skola = seeder.kreirajSkolu("KatalogIT-Case");
        TenantContext.set(skola.getId());
        try {
            PredmetResponse p = predmetService.kreiraj(
                    new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));

            Tema mala = katalogService.findOrCreateTema(p.id(), "uvod u mreze", (short) 1, null, null, null);
            Tema velika = katalogService.findOrCreateTema(p.id(), "UVOD u MREZE", (short) 1, null, null, null);

            assertThat(velika.getId()).isEqualTo(mala.getId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("findOrCreateJedinica: reuse za istu temu + naziv")
    void findOrCreateJedinica_reuse() {
        Skola skola = seeder.kreirajSkolu("KatalogIT-Jed");
        TenantContext.set(skola.getId());
        try {
            PredmetResponse p = predmetService.kreiraj(
                    new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));
            Tema tema = katalogService.findOrCreateTema(p.id(), "Tema 1", (short) 1, null, null, null);

            var j1 = katalogService.findOrCreateJedinica(tema.getId(), "Sta je mreza", (short) 1);
            var j2 = katalogService.findOrCreateJedinica(tema.getId(), "Sta je mreza", (short) 1);
            var j3 = katalogService.findOrCreateJedinica(tema.getId(), "Tipovi mreza", (short) 2);

            assertThat(j1.getId()).isEqualTo(j2.getId());
            assertThat(j3.getId()).isNotEqualTo(j1.getId());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Tipovi casa: lista za skolu vraca sistemske (skola_id IS NULL) iz V7 seed-a")
    void tipoviCasa_vracajuSistemske() {
        Skola skola = seeder.kreirajSkolu("KatalogIT-Tipovi");
        TenantContext.set(skola.getId());
        try {
            var tipovi = katalogService.tipoviCasa();

            assertThat(tipovi).isNotEmpty();
            assertThat(tipovi).anyMatch(t -> t.naziv().equals("Obrada") && t.sistemski());
            assertThat(tipovi).anyMatch(t -> t.naziv().equals("Utvrdjivanje") && t.sistemski());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Pretraga tema: case-insensitive LIKE")
    void searchTema_caseInsensitive() {
        Skola skola = seeder.kreirajSkolu("KatalogIT-Search");
        TenantContext.set(skola.getId());
        try {
            PredmetResponse p = predmetService.kreiraj(
                    new KreirajPredmetRequest("Mreze", (short) 3, (short) 2));
            katalogService.findOrCreateTema(p.id(), "Uvod u mreze", (short) 1, null, null, null);
            katalogService.findOrCreateTema(p.id(), "Mrezni protokoli", (short) 2, null, null, null);
            katalogService.findOrCreateTema(p.id(), "Bezbednost", (short) 3, null, null, null);

            var nadji = katalogService.pretragaTema(p.id(), "MREZ");
            assertThat(nadji).extracting("naziv")
                    .contains("Uvod u mreze", "Mrezni protokoli")
                    .doesNotContain("Bezbednost");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Katalog skole A nije vidljiv kroz TenantContext skole B (cross-tenant izolacija)")
    void katalog_izolacijaPoSkoli() {
        Skola skolaA = seeder.kreirajSkolu("KatalogIT-A");
        Skola skolaB = seeder.kreirajSkolu("KatalogIT-B");

        UUID predmetAId;
        TenantContext.set(skolaA.getId());
        try {
            predmetAId = predmetService.kreiraj(
                    new KreirajPredmetRequest("Mreze", (short) 3, (short) 2)).id();
            katalogService.findOrCreateTema(predmetAId, "Tema-A", (short) 1, null, null, null);
        } finally {
            TenantContext.clear();
        }

        TenantContext.set(skolaB.getId());
        try {
            // Predmet skole A je iz tudje skole — pristup mora baciti exception
            org.junit.jupiter.api.Assertions.assertThrows(
                    rs.skola.platforma.common.exception.TenantViolationException.class,
                    () -> katalogService.temePredmeta(predmetAId));
        } finally {
            TenantContext.clear();
        }
    }
}
