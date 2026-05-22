package rs.skola.platforma;

import org.junit.jupiter.api.Test;

/**
 * Smoke test — proverava da li ApplicationContext uopste moze da se podigne
 * uz validne migracije i sve auto-konfiguracije. Pokrece se kroz Testcontainers
 * preko {@link AbstractIntegrationTest}.
 */
class SkolskaPlatformaApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
