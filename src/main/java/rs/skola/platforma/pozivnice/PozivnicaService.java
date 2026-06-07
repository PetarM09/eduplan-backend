package rs.skola.platforma.pozivnice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ConflictException;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Poreklo;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.pozivnice.web.AktivirajPozivnicuRequest;
import rs.skola.platforma.pozivnice.web.AzurirajEmailRequest;
import rs.skola.platforma.pozivnice.web.BootstrapRezultat;
import rs.skola.platforma.pozivnice.web.PostaviPredmeteRequest;
import rs.skola.platforma.pozivnice.web.PozivnicaInfoResponse;
import rs.skola.platforma.pozivnice.web.PozvaniKorisnikResponse;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;
import rs.skola.platforma.raspored.repo.RasporedStavkaRepository;
import rs.skola.platforma.tenant.domain.Skola;
import rs.skola.platforma.tenant.repo.SkolaRepository;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PozivnicaService {

    private static final int POZIVNICA_VAZI_DANA = 30;

    private final KorisnikRepository korisnikRepo;
    private final PredmetRepository predmetRepo;
    private final RasporedStavkaRepository rasporedStavkaRepo;
    private final SkolaRepository skolaRepo;
    private final PozivnicaMailService mailService;
    private final PasswordEncoder passwordEncoder;

    // -------- Lista pozvanih (KOORDINATOR) --------

    @Transactional(readOnly = true)
    public List<PozvaniKorisnikResponse> svi() {
        UUID skolaId = TenantContext.require();
        return korisnikRepo.findPozvaniBySkolaId(skolaId).stream()
                .map(this::toResponse)
                .toList();
    }

    // -------- Bootstrap iz aktivne XML verzije rasporeda --------

    @Transactional
    public BootstrapRezultat bootstrapIzRasporeda() {
        UUID skolaId = TenantContext.require();
        List<String> labelovi = rasporedStavkaRepo.distinctNemapiraneLabels(skolaId);
        int nov = 0, presk = 0;
        List<String> upozorenja = new ArrayList<>();
        for (String label : labelovi) {
            String trimmed = label == null ? "" : label.trim();
            if (trimmed.isEmpty()) continue;
            try {
                String[] delovi = trimmed.split("\\s+", 2);
                String ime = delovi[0];
                String prezime = delovi.length > 1 ? delovi[1] : "";
                if (korisnikRepo.findBySkolaIdAndImeIgnoreCaseAndPrezimeIgnoreCase(
                        skolaId, ime, prezime).isPresent()) {
                    presk++;
                    continue;
                }
                Korisnik k = napraviPozvanog(skolaId, ime, prezime, null, Poreklo.RASPORED);
                korisnikRepo.save(k);
                // Auto-link raspored stavki za ovaj label
                rasporedStavkaRepo.mapirajPoLabelu(skolaId, label, k);
                nov++;
            } catch (Exception ex) {
                log.warn("Greska pri kreiranju pozvanog za label '{}': {}", label, ex.getMessage());
                upozorenja.add("Nije uspelo kreiranje za \"" + label + "\": " + ex.getMessage());
            }
        }
        return new BootstrapRezultat(nov, presk, upozorenja);
    }

    // -------- Postavljanje predmeta i emaila --------

    @Transactional
    public PozvaniKorisnikResponse postaviPredmete(UUID korisnikId, PostaviPredmeteRequest req) {
        UUID skolaId = TenantContext.require();
        Korisnik k = nadji(korisnikId, skolaId);
        Set<Predmet> predmeti = new HashSet<>();
        for (UUID pid : req.predmetiIds()) {
            Predmet p = predmetRepo.findById(pid)
                    .orElseThrow(() -> new ResourceNotFoundException("Predmet", pid));
            if (!skolaId.equals(p.getSkolaId())) {
                throw new TenantViolationException("Predmet ne pripada vasoj skoli");
            }
            predmeti.add(p);
        }
        k.getPredmeti().clear();
        k.getPredmeti().addAll(predmeti);
        return toResponse(k);
    }

    @Transactional
    public PozvaniKorisnikResponse azurirajEmail(UUID korisnikId, AzurirajEmailRequest req) {
        UUID skolaId = TenantContext.require();
        Korisnik k = nadji(korisnikId, skolaId);
        k.setEmail(req.email().trim());
        return toResponse(k);
    }

    // -------- Slanje pozivnice --------

    @Transactional
    public void posaljiPozivnicu(UUID korisnikId) {
        UUID skolaId = TenantContext.require();
        Korisnik k = nadji(korisnikId, skolaId);
        if (k.getEmail() == null || k.getEmail().isBlank() || k.getEmail().endsWith("@placeholder.local")) {
            throw new ValidationException("Postavi pravi email pre slanja pozivnice");
        }
        if (k.getPozivnicaToken() == null) {
            k.setPozivnicaToken(UUID.randomUUID());
        }
        k.setPozivnicaIstice(OffsetDateTime.now().plusDays(POZIVNICA_VAZI_DANA));
        Skola skola = k.getSkola() == null ? null : skolaRepo.findById(k.getSkola().getId()).orElse(null);
        mailService.posaljiPozivnicu(k, skola, k.getPozivnicaToken());
    }

    // -------- Aktivacija tokenom (PUBLIC) --------

    @Transactional(readOnly = true)
    public PozivnicaInfoResponse info(UUID token) {
        Korisnik k = korisnikRepo.findByPozivnicaToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Pozivnica nije pronadjena"));
        boolean istekla = k.getPozivnicaIstice() == null
                || k.getPozivnicaIstice().isBefore(OffsetDateTime.now());
        return new PozivnicaInfoResponse(
                k.getIme(),
                k.getPrezime(),
                k.getEmail(),
                k.getSkola() == null ? "" : k.getSkola().getNaziv(),
                istekla
        );
    }

    @Transactional
    public void aktiviraj(UUID token, AktivirajPozivnicuRequest req) {
        Korisnik k = korisnikRepo.findByPozivnicaToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Pozivnica nije pronadjena"));
        if (k.getPozivnicaIstice() == null || k.getPozivnicaIstice().isBefore(OffsetDateTime.now())) {
            throw new ConflictException("Pozivnica je istekla — zatrazi novu od koordinatora");
        }
        k.setLozinkaHash(passwordEncoder.encode(req.lozinka()));
        k.setAktivan(true);
        k.setPozivnicaToken(null);
        k.setPozivnicaIstice(null);
        log.info("Korisnik {} ({}) aktivirao nalog kroz pozivnicu", k.getId(), k.getUsername());
    }

    // -------- Pomoc: kreiranje POZVAN korisnika --------

    public Korisnik napraviPozvanog(UUID skolaId, String ime, String prezime, String email, Poreklo poreklo) {
        String imeT = ime == null ? "" : ime.trim();
        String prezimeT = prezime == null ? "" : prezime.trim();
        if (imeT.isEmpty()) throw new ValidationException("Ime je obavezno");
        String username = generisanjeUsername(imeT, prezimeT);
        String stvarniEmail = (email == null || email.isBlank())
                ? username + "@placeholder.local"
                : email.trim();
        if (korisnikRepo.existsByEmailIgnoreCase(stvarniEmail) && !stvarniEmail.endsWith("@placeholder.local")) {
            throw new ConflictException("Email " + stvarniEmail + " je vec u sistemu");
        }
        Skola skola = skolaRepo.findById(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Skola", skolaId));
        Korisnik k = Korisnik.builder()
                .skola(skola)
                .username(username)
                .email(stvarniEmail)
                .ime(imeT)
                .prezime(prezimeT)
                .uloga(Uloga.NASTAVNIK)
                .aktivan(false)
                .lozinkaHash(null)
                .pozivnicaToken(UUID.randomUUID())
                .pozivnicaIstice(OffsetDateTime.now().plusDays(POZIVNICA_VAZI_DANA))
                .poreklo(poreklo)
                .build();
        return k;
    }

    private String generisanjeUsername(String ime, String prezime) {
        String baza = normalizuj(ime) + (prezime.isBlank() ? "" : "." + normalizuj(prezime));
        if (baza.isBlank()) baza = "nastavnik";
        String kandidat = baza;
        int i = 1;
        while (korisnikRepo.existsByUsernameIgnoreCase(kandidat)) {
            i++;
            kandidat = baza + i;
        }
        return kandidat;
    }

    private static String normalizuj(String s) {
        String dec = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return dec.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private Korisnik nadji(UUID id, UUID skolaId) {
        Korisnik k = korisnikRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik", id));
        if (k.getSkola() == null || !skolaId.equals(k.getSkola().getId())) {
            throw new TenantViolationException();
        }
        if (k.getPozivnicaToken() == null) {
            throw new ConflictException("Korisnik nije u stanju POZVAN");
        }
        return k;
    }

    private PozvaniKorisnikResponse toResponse(Korisnik k) {
        UUID skolaId = k.getSkola().getId();
        List<String> odeljenja = rasporedStavkaRepo
                .distinctOdeljenjaZaKorisnika(skolaId, k.getId());
        return new PozvaniKorisnikResponse(
                k.getId(),
                k.getIme(),
                k.getPrezime(),
                k.getUsername(),
                k.getEmail(),
                k.getPoreklo(),
                k.getPozivnicaToken() != null,
                k.getPozivnicaIstice(),
                k.getPredmeti().stream().map(Predmet::getId).toList(),
                k.getPredmeti().stream().map(Predmet::getNaziv).toList(),
                odeljenja
        );
    }
}
