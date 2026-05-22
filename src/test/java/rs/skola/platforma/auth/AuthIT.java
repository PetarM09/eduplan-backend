package rs.skola.platforma.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rs.skola.platforma.AbstractIntegrationTest;
import rs.skola.platforma.TestDataSeeder;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.tenant.domain.Skola;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TestDataSeeder seeder;

    @Test
    @DisplayName("Login validnim kredencijalima vraca access + refresh token + skola_id u response-u")
    void login_validniKredencijali_vracaTokene() throws Exception {
        Skola skola = seeder.kreirajSkolu("Skola-AuthIT-1");
        Korisnik nastavnik = seeder.kreirajKorisnika(skola, Uloga.NASTAVNIK);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(nastavnik.getUsername(), TestDataSeeder.DEFAULT_LOZINKA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.skolaId").value(skola.getId().toString()))
                .andExpect(jsonPath("$.data.uloga").value("NASTAVNIK"));
    }

    @Test
    @DisplayName("Login pogresnom lozinkom vraca 401 NEISPRAVNI_KREDENCIJALI")
    void login_pogresnaLozinka_vraca401() throws Exception {
        Skola skola = seeder.kreirajSkolu("Skola-AuthIT-2");
        Korisnik n = seeder.kreirajKorisnika(skola, Uloga.NASTAVNIK);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"PogresnaLozinka"}
                                """.formatted(n.getUsername())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NEISPRAVNI_KREDENCIJALI"));
    }

    @Test
    @DisplayName("Login nepostojecim username-om vraca 401")
    void login_nepostojeciUser_vraca401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ne-postoji","password":"x"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bez auth header-a, zasticeni endpoint vraca 401")
    void bezTokena_zasticenaRuta_vraca401() throws Exception {
        mockMvc.perform(post("/api/v1/korisnici")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("NEAUTORIZOVANO"));
    }
}
