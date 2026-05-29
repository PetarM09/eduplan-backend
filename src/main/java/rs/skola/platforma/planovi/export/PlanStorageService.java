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
        return sacuvaj(direktorijumGodisnjeg(skolaId, planId), "godisnji-plan.docx", bytes);
    }

    public String sacuvajPdf(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(direktorijumGodisnjeg(skolaId, planId), "godisnji-plan.pdf", bytes);
    }

    public String sacuvajOperativniWord(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(direktorijumOperativnog(skolaId, planId), "operativni-plan.docx", bytes);
    }

    public String sacuvajOperativniPdf(UUID skolaId, UUID planId, byte[] bytes) {
        return sacuvaj(direktorijumOperativnog(skolaId, planId), "operativni-plan.pdf", bytes);
    }

    private String sacuvaj(Path dir, String fileName, byte[] bytes) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return baseDir.relativize(file).toString();
        } catch (IOException ex) {
            throw new StorageException("Greska pri upisu fajla: " + ex.getMessage(), ex);
        }
    }

    public byte[] procitaj(UUID skolaId, UUID planId, String relativnaPutanja) {
        return procitajIz(direktorijumGodisnjeg(skolaId, planId), skolaId, relativnaPutanja);
    }

    public byte[] procitajOperativni(UUID skolaId, UUID planId, String relativnaPutanja) {
        return procitajIz(direktorijumOperativnog(skolaId, planId), skolaId, relativnaPutanja);
    }

    private byte[] procitajIz(Path dozvoljeniDir, UUID skolaId, String relativnaPutanja) {
        if (relativnaPutanja == null) {
            throw new ResourceNotFoundException("Fajl nije generisan");
        }
        Path file = baseDir.resolve(relativnaPutanja).normalize();
        Path dir = dozvoljeniDir.normalize();
        if (!file.startsWith(baseDir) || !file.startsWith(dir)) {
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

    private Path direktorijumGodisnjeg(UUID skolaId, UUID planId) {
        return baseDir.resolve("skole").resolve(skolaId.toString())
                .resolve("godisnji-planovi").resolve(planId.toString());
    }

    private Path direktorijumOperativnog(UUID skolaId, UUID planId) {
        return baseDir.resolve("skole").resolve(skolaId.toString())
                .resolve("operativni-planovi").resolve(planId.toString());
    }

    public static class StorageException extends BaseException {
        public StorageException(String message, Throwable cause) {
            super("STORAGE_GRESKA", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
        }
    }
}
