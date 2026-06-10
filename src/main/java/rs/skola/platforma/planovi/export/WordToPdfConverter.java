package rs.skola.platforma.planovi.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Konverzija .docx -> .pdf preko LibreOffice-a u headless modu.
 *
 * Zahteva da je `soffice` (ili `libreoffice`) instaliran na sistemu.
 * Putanja je konfigurabilna kroz `app.libreoffice.binary` (default
 * "soffice" pa pretrazuje PATH).
 *
 * Svaka konverzija dobija privatni `user-profile` direktorijum i izlazni
 * direktorijum koji se brisu posle. Time se sprecava zaglavljivanje
 * kad vise zahteva dolazi paralelno (LO sa default profilom drzi lock).
 */
@Slf4j
@Component
public class WordToPdfConverter {

    private final String binary;
    private final long timeoutSec;

    public WordToPdfConverter(@Value("${app.libreoffice.binary:soffice}") String binary,
                                @Value("${app.libreoffice.timeout-sec:90}") long timeoutSec) {
        this.binary = binary;
        this.timeoutSec = timeoutSec;
    }

    public byte[] konvertuj(byte[] docxBytes) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("plan-pdf-");
            Path profileDir = workDir.resolve("lo-profile");
            Path outDir = workDir.resolve("out");
            Files.createDirectories(profileDir);
            Files.createDirectories(outDir);

            String baseName = "in-" + UUID.randomUUID();
            Path inputFile = workDir.resolve(baseName + ".docx");
            Files.write(inputFile, docxBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    binary,
                    "--headless",
                    "--norestore",
                    "--nofirststartwizard",
                    "--nolockcheck",
                    "-env:UserInstallation=file://" + profileDir.toAbsolutePath(),
                    "--convert-to", "pdf",
                    "--outdir", outDir.toAbsolutePath().toString(),
                    inputFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Drenirano da bi proces mogao da se zatvori (LO ume da blokira na stdout)
            byte[] logBytes = proc.getInputStream().readAllBytes();
            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new IllegalStateException("LibreOffice konverzija je istekla (timeout)");
            }
            if (proc.exitValue() != 0) {
                String log = new String(logBytes);
                throw new IllegalStateException(
                        "LibreOffice exit " + proc.exitValue() + ": " + log);
            }

            Path pdfFile = outDir.resolve(baseName + ".pdf");
            if (!Files.exists(pdfFile)) {
                throw new IllegalStateException("PDF nije generisan na ocekivanoj putanji: " + pdfFile);
            }
            return Files.readAllBytes(pdfFile);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("Greska pri PDF konverziji", ex);
        } finally {
            if (workDir != null) obrisiRekurzivno(workDir);
        }
    }

    private void obrisiRekurzivno(Path dir) {
        try {
            if (!Files.exists(dir)) return;
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> b.toString().length() - a.toString().length())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); }
                            catch (IOException e) { log.warn("Ne mogu obrisati {}: {}", p, e.getMessage()); }
                        });
            }
        } catch (IOException e) {
            log.warn("Ne mogu obrisati radni dir {}: {}", dir, e.getMessage());
        }
    }
}
