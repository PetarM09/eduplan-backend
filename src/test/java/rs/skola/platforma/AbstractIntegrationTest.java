package rs.skola.platforma;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Bazni razred za sve integracione testove. Pokrece jedan PostgreSQL 16 kontejner
 * deljen izmedju svih testova (statika + reuse) — Flyway izvrsi sve migracije pri startu
 * konteksta, pa svaki test ima realnu sšemu.
 *
 * <p>Testovi koji nasleđuju ovaj razred treba da resetuju bazu na pocetku
 * (npr. {@code @Sql("/test-reset.sql")} ili manualno kroz repository.deleteAll() / TRUNCATE).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** Singleton kontejner — pokrenut samo jednom kroz ceo JVM, deljen izmedju test razreda. */
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skolska_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * Globalno mockiramo JavaMailSender da bi svi testovi delili isti ApplicationContext
     * (Spring inace cache-uje context po setu @MockitoBean bean-ova). Mail testovi mogu
     * da koriste {@code Mockito.verify(mailSender, ...)} preko ovog istog mock-a.
     */
    @MockitoBean
    protected JavaMailSender mailSender;

    @BeforeEach
    void resetMailSenderDefaults() {
        Mockito.reset(mailSender);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
    }
}
