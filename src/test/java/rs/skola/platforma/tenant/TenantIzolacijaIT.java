package rs.skola.platforma.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rs.skola.platforma.AbstractIntegrationTest;
import rs.skola.platforma.TestAuthHelper;
import rs.skola.platforma.TestDataSeeder;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.predmeti.PredmetService;
import rs.skola.platforma.predmeti.web.KreirajPredmetRequest;
import rs.skola.platforma.predmeti.web.PredmetResponse;
import rs.skola.platforma.tenant.domain.Skola;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NAJVAZNIJI integracioni test diplomskog rada: dokazuje da korisnik skole A ne moze
 * da pristupi nijednom resursu skole B. Verifikuje da:
 *   1. Lista korisnika je filtrirana po skola_id iz TenantContext-a
 *   2. Pokusaj pristupa konkretnom resursu druge skole vraca 403 PRISTUP_ZABRANJEN
 *   3. Kreiranje resursa uvek koristi skola_id iz JWT-a, ne iz body-ja
 *   4. Kreirani predmet "procuri" u TenantContext A bi bio nevidljiv kontekstu B
 */
class TenantIzolacijaIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestDataSeeder seeder;
    @Autowired TestAuthHelper auth;
    @Autowired PredmetService predmetService;

    @Test
    @DisplayName("Lista korisnika za KOORDINATOR-a A ne sadrzi nijednog korisnika skole B")
    void listaKorisnika_neVidiTudje() throws Exception {
        Skola skolaA = seeder.kreirajSkolu("Skola-A-Lista");
        Skola skolaB = seeder.kreirajSkolu("Skola-B-Lista");
        Korisnik koordA = seeder.kreirajKorisnika(skolaA, Uloga.KOORDINATOR);
        Korisnik nastA = seeder.kreirajKorisnika(skolaA, Uloga.NASTAVNIK);
        Korisnik nastB = seeder.kreirajKorisnika(skolaB, Uloga.NASTAVNIK);

        mockMvc.perform(get("/api/v1/korisnici")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerHeader(koordA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '%s')]".formatted(nastA.getId())).exists())
                .andExpect(jsonPath("$.data[?(@.id == '%s')]".formatted(nastB.getId())).doesNotExist())
                .andExpect(jsonPath("$.data[?(@.id == '%s')]".formatted(koordA.getId())).exists());
    }

    @Test
    @DisplayName("Pregled korisnika druge skole vraca 403 PRISTUP_ZABRANJEN")
    void pregledTudjegKorisnika_vraca403() throws Exception {
        Skola skolaA = seeder.kreirajSkolu("Skola-A-Pregled");
        Skola skolaB = seeder.kreirajSkolu("Skola-B-Pregled");
        Korisnik koordA = seeder.kreirajKorisnika(skolaA, Uloga.KOORDINATOR);
        Korisnik nastB = seeder.kreirajKorisnika(skolaB, Uloga.NASTAVNIK);

        mockMvc.perform(get("/api/v1/korisnici/{id}", nastB.getId())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerHeader(koordA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PRISTUP_ZABRANJEN"));
    }

    @Test
    @DisplayName("Kreiranje rotacije za odeljenje druge skole vraca 403 PRISTUP_ZABRANJEN")
    void rotacijaSaTudjimOdeljenjem_vraca403() throws Exception {
        Skola skolaA = seeder.kreirajSkolu("Skola-A-Rot");
        Skola skolaB = seeder.kreirajSkolu("Skola-B-Rot");
        Korisnik koordA = seeder.kreirajKorisnika(skolaA, Uloga.KOORDINATOR);
        Korisnik nastB = seeder.kreirajKorisnika(skolaB, Uloga.NASTAVNIK);
        Odeljenje odB = seeder.kreirajOdeljenje(skolaB, (short) 3, "1", nastB);

        mockMvc.perform(post("/api/v1/rotacija")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerHeader(koordA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "naziv": "Test rotacija",
                                  "odeljenjeId": "%s",
                                  "skolskaGodina": "2024/2025",
                                  "brojGrupa": 2,
                                  "brojNedelja": 18,
                                  "predmeti": [
                                    {
                                      "profesorLabel": "Petar Petrovic",
                                      "naziv": "Racunarske mreze",
                                      "casovaNedeljno": 2
                                    }
                                  ]
                                }
                                """.formatted(odB.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PRISTUP_ZABRANJEN"));
    }

    @Test
    @DisplayName("Pristup predmetu druge skole je 403 — TenantContext razdvaja sve")
    void pristupPredmetuDrugeSkole_vraca403() throws Exception {
        Skola skolaA = seeder.kreirajSkolu("Skola-A-Predmet");
        Skola skolaB = seeder.kreirajSkolu("Skola-B-Predmet");
        Korisnik koordA = seeder.kreirajKorisnika(skolaA, Uloga.KOORDINATOR);
        Korisnik koordB = seeder.kreirajKorisnika(skolaB, Uloga.KOORDINATOR);

        // Predmet skole B kreiramo programski (kao koord B)
        TenantContext.set(skolaB.getId());
        PredmetResponse predmetB;
        try {
            predmetB = predmetService.kreiraj(new KreirajPredmetRequest("PredmetB", (short) 3, (short) 2));
        } finally {
            TenantContext.clear();
        }

        // Koord A pokusava da ga vidi → 403
        mockMvc.perform(get("/api/v1/predmeti/{id}", predmetB.id())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerHeader(koordA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PRISTUP_ZABRANJEN"));

        // Lista predmeta za koord A ga ne sadrzi
        mockMvc.perform(get("/api/v1/predmeti")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearerHeader(koordA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '%s')]".formatted(predmetB.id())).doesNotExist());
    }

    @Test
    @DisplayName("PredmetService.sviAktivni() vraca iskljucivo predmete iz TenantContext skole — programatski test")
    void predmetService_filtriraPoTenantContext() {
        Skola skolaA = seeder.kreirajSkolu("Skola-A-Prog");
        Skola skolaB = seeder.kreirajSkolu("Skola-B-Prog");

        TenantContext.set(skolaA.getId());
        try {
            predmetService.kreiraj(new KreirajPredmetRequest("PredmetA-1", (short) 3, (short) 2));
            predmetService.kreiraj(new KreirajPredmetRequest("PredmetA-2", (short) 3, (short) 2));
        } finally {
            TenantContext.clear();
        }
        TenantContext.set(skolaB.getId());
        try {
            predmetService.kreiraj(new KreirajPredmetRequest("PredmetB-1", (short) 3, (short) 2));
        } finally {
            TenantContext.clear();
        }

        TenantContext.set(skolaA.getId());
        try {
            var sviA = predmetService.sviAktivni();
            assertThat(sviA).extracting(PredmetResponse::naziv)
                    .containsExactlyInAnyOrder("PredmetA-1", "PredmetA-2");
        } finally {
            TenantContext.clear();
        }
        TenantContext.set(skolaB.getId());
        try {
            var sviB = predmetService.sviAktivni();
            assertThat(sviB).extracting(PredmetResponse::naziv)
                    .containsExactly("PredmetB-1");
        } finally {
            TenantContext.clear();
        }
    }
}
