package rs.skola.platforma.planovi.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Konverzija .docx -> .pdf preko LibreOffice-a u headless modu.
 *
 * Resolver redosled za binary:
 *   1) app.libreoffice.binary (env / config)
 *   2) common paths: /Applications/LibreOffice.app/.../soffice (mac),
 *      /usr/bin/soffice, /usr/local/bin/soffice, /opt/homebrew/bin/soffice
 *   3) "soffice" (PATH lookup)
 *
 * Ako nista nije nadjeno, baca {@link PdfNotAvailableException} koji
 * pozivaoci hvataju i nastavljaju bez PDF-a.
 */
@Slf4j
@Component
public class WordToPdfConverter {

    private static final List<String> KANDIDATI = List.of(
            "/Applications/LibreOffice.app/Contents/MacOS/soffice",
            "/usr/bin/soffice",
            "/usr/local/bin/soffice",
            "/opt/homebrew/bin/soffice",
            "/snap/bin/libreoffice"
    );

    private final String konfigurisanaPutanja;
    private final long timeoutSec;
    private volatile String resolvedBinary;
    private volatile boolean resolveAttempted;

    public WordToPdfConverter(@Value("${app.libreoffice.binary:}") String binary,
                                @Value("${app.libreoffice.timeout-sec:90}") long timeoutSec) {
        this.konfigurisanaPutanja = binary == null ? "" : binary.trim();
        this.timeoutSec = timeoutSec;
    }

    public byte[] konvertuj(byte[] docxBytes) {
        String binary = pronadjiBinary();
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

    public boolean dostupan() {
        try {
            pronadjiBinary();
            return true;
        } catch (PdfNotAvailableException ex) {
            return false;
        }
    }

    private String pronadjiBinary() {
        if (resolvedBinary != null) return resolvedBinary;
        synchronized (this) {
            if (resolvedBinary != null) return resolvedBinary;
            String found = otkrij();
            resolveAttempted = true;
            if (found == null) {
                throw new PdfNotAvailableException(
                        "LibreOffice 'soffice' binary nije pronadjen. Probao sam: "
                                + (konfigurisanaPutanja.isEmpty() ? "(config prazan)" : konfigurisanaPutanja)
                                + ", " + KANDIDATI + ", PATH. "
                                + "Postavi app.libreoffice.binary ili instaliraj LibreOffice "
                                + "(macOS: brew install --cask libreoffice).");
            }
            resolvedBinary = found;
            log.info("LibreOffice binary: {}", resolvedBinary);
            return resolvedBinary;
        }
    }

    private String otkrij() {
        if (!konfigurisanaPutanja.isEmpty()) {
            if (Files.isExecutable(Paths.get(konfigurisanaPutanja))) return konfigurisanaPutanja;
            if (proveriPath(konfigurisanaPutanja)) return konfigurisanaPutanja;
        }
        for (String kand : KANDIDATI) {
            if (Files.isExecutable(Paths.get(kand))) return kand;
        }
        if (proveriPath("soffice")) return "soffice";
        if (proveriPath("libreoffice")) return "libreoffice";
        return null;
    }

    private boolean proveriPath(String ime) {
        try {
            Process p = new ProcessBuilder("which", ime).redirectErrorStream(true).start();
            byte[] out = p.getInputStream().readAllBytes();
            p.waitFor(5, TimeUnit.SECONDS);
            String line = new String(out).trim();
            return p.exitValue() == 0 && !line.isEmpty() && Files.isExecutable(Paths.get(line));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
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

    public static class PdfNotAvailableException extends RuntimeException {
        public PdfNotAvailableException(String message) { super(message); }
    }
}
