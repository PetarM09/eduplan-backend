package rs.skola.platforma.planovi.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.skola.platforma.common.exception.BaseException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Cuva i cita Word/PDF fajlove sa lokalnog file-systema.
 *
 * <p>Putanja:
 * <pre>${app.storage.base-dir}/skole/{skola_id}/godisnji-planovi/{plan_id}/{plan|operativni}.{docx|pdf}</pre>
 *
 * <p>Citanje fajla obavezno proverava da {@code skola_id} u putanji odgovara onome
 * sto je trazeno — sprecava path-traversal i cross-tenant pristup.
 */
@Slf4j
@Service
public class PlanStorageService {

    private final Path baseDir;

    public PlanStorageService(@Value("${app.storage.base-dir:./storage}") String baseDirStr) {
        this.baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
            log.info("Storage base dir: {}", this.baseDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Ne mogu da kreiram storage dir: " + this.baseDir, ex);
        }
    }

    public String sacuvajWord(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(skolaId, planId, "godisnji-plan.docx", bytes);
    }

    public String sacuvajPdf(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(skolaId, planId, "godisnji-plan.pdf", bytes);
    }

    private String sacuvaj(UUID skolaId, UUID planId, String fileName, byte[] bytes) {
        try {
            Path dir = direktorijumPlana(skolaId, planId);
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return baseDir.relativize(file).toString();
        } catch (IOException ex) {
            throw new StorageException("Greska pri upisu fajla: " + ex.getMessage(), ex);
        }
    }

    public byte[] procitaj(UUID skolaId, UUID planId, String relativnaPutanja) {
        if (relativnaPutanja == null) {
            throw new ResourceNotFoundException("Fajl nije generisan");
        }
        Path file = baseDir.resolve(relativnaPutanja).normalize();
        // Sigurnosna provera: putanja MORA da bude unutar baseDir-a i unutar
        // direktorijuma za tu skolu (sprecava path traversal preko "../").
        Path skolaDir = direktorijumPlana(skolaId, planId).normalize();
        if (!file.startsWith(baseDir) || !file.startsWith(skolaDir)) {
            log.warn("Pokusaj pristupa fajlu izvan dozvoljene putanje: {} (skola {})", file, skolaId);
            throw new TenantViolationException("Pristup fajlu nije dozvoljen");
        }
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Fajl ne postoji: " + relativnaPutanja);
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new StorageException("Greska pri citanju fajla: " + ex.getMessage(), ex);
        }
    }

    private Path direktorijumPlana(UUID skolaId, UUID planId) {
        return baseDir.resolve("skole").resolve(skolaId.toString())
                .resolve("godisnji-planovi").resolve(planId.toString());
    }

    public static class StorageException extends BaseException {
        public StorageException(String message, Throwable cause) {
            super("STORAGE_GRESKA", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        }
    }
}
