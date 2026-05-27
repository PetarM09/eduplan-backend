package rs.skola.platforma.raspored;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.skola.platforma.auth.security.CustomUserDetails;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;
import rs.skola.platforma.korisnici.repo.KorisnikRepository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
import rs.skola.platforma.odeljenja.repo.OdeljenjeRepository;
import rs.skola.platforma.raspored.domain.RasporedStavka;
import rs.skola.platforma.raspored.domain.VerzijaRasporeda;
import rs.skola.platforma.raspored.parser.ParsedRasporedRed;
import rs.skola.platforma.raspored.parser.XmlRasporedParser;
import rs.skola.platforma.raspored.repo.RasporedStavkaRepository;
import rs.skola.platforma.raspored.repo.VerzijaRasporedaRepository;
import rs.skola.platforma.raspored.web.RasporedStavkaResponse;
import rs.skola.platforma.raspored.web.UvozRasporedaResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RasporedService {

    /** "4-1" ili "4 1" ili "4/1" ili "3A" */
    private static final Pattern ODELJENJE = Pattern.compile(
            "^\\s*(?<razred>\\d{1,2})\\s*[-/\\s]?\\s*(?<oznaka>[A-Za-z0-9]{1,3})\\s*$");

    private final XmlRasporedParser parser;
    private final VerzijaRasporedaRepository verzijaRepo;
    private final RasporedStavkaRepository stavkaRepo;
    private final OdeljenjeRepository odeljenjeRepo;
    private final KorisnikRepository korisnikRepo;

    @Transactional
    public UvozRasporedaResponse uvezi(MultipartFile file, String skolskaGodina, String naziv, boolean aktivan) {
        UUID skolaId = TenantContext.require();
        if (file == null || file.isEmpty()) {
            throw new ValidationException("FAJL_PRAZAN", "Nije priložen XML fajl");
        }
        String xmlOriginal;
        List<ParsedRasporedRed> redovi;
        try (InputStream in = file.getInputStream()) {
            redovi = parser.parse(in);
        } catch (IOException ex) {
            throw new ValidationException("FAJL_GRESKA", "Ne mogu da procitam fajl: " + ex.getMessage());
        }
        try {
            xmlOriginal = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            xmlOriginal = null;
        }
        if (redovi.isEmpty()) {
            throw new ValidationException("PRAZAN_RASPORED", "XML ne sadrzi redove rasporeda");
        }

        if (aktivan) {
            verzijaRepo.deaktivirajSve(skolaId);
        }

        VerzijaRasporeda verzija = VerzijaRasporeda.builder()
                .naziv(naziv == null || naziv.isBlank() ? "Uvoz " + LocalDate.now() : naziv)
                .skolskaGodina(skolskaGodina)
                .datumOd(LocalDate.now())
                .aktivan(aktivan)
                .xmlOriginal(xmlOriginal)
                .build();
        verzija.setSkolaId(skolaId);
        verzija = verzijaRepo.save(verzija);

        // Index korisnika za brzi lookup
        Map<String, Korisnik> indeksKorisnika = indeksKorisnikaSkole(skolaId);
        Map<String, Odeljenje> indeksOdeljenja = indeksOdeljenjaSkole(skolaId, skolskaGodina);
        int odeljenjaPreUvoza = indeksOdeljenja.size();

        List<String> nemapirani = new ArrayList<>();
        int mapirano = 0;
        int kreiranihStavki = 0;

        for (ParsedRasporedRed red : redovi) {
            Korisnik nastavnik = pronadjiKorisnika(indeksKorisnika, red.nastavnikLabel());
            if (nastavnik == null) {
                nemapirani.add(red.nastavnikLabel());
            } else {
                mapirano++;
            }

            for (ParsedRasporedRed.ParsedStavka s : red.stavke()) {
                // Format "1-1/1-5" znaci da nastavnik u istom casu predaje dva odeljenja
                // (spojeni grupni cas). Kreiramo jednu RasporedStavka po odeljenju.
                String[] delovi = s.odeljenjeLabel().split("/");
                for (String deo : delovi) {
                    Odeljenje od = nadjiIliKreirajOdeljenje(indeksOdeljenja, skolaId, skolskaGodina, deo.trim());
                    if (od == null) continue;
                    kreiranihStavki += sacuvajStavku(verzija, nastavnik, red.nastavnikLabel(), od, s, skolaId) ? 1 : 0;
                }
            }
        }

        int kreiranihOdeljenja = indeksOdeljenja.size() - odeljenjaPreUvoza;
        return new UvozRasporedaResponse(
                verzija.getId(),
                verzija.getNaziv(),
                skolskaGodina,
                redovi.size(),
                mapirano,
                kreiranihStavki,
                kreiranihOdeljenja,
                nemapirani
        );
    }

    private boolean sacuvajStavku(VerzijaRasporeda verzija, Korisnik nastavnik, String nastavnikLabel,
                                   Odeljenje od, ParsedRasporedRed.ParsedStavka s, UUID skolaId) {
        RasporedStavka stavka = RasporedStavka.builder()
                .verzija(verzija)
                .korisnik(nastavnik) // moze biti null kad korisnik nije u sistemu
                .nastavnikLabel(nastavnikLabel)
                .odeljenje(od)
                .predmetLabel(null)
                .dan(s.dan())
                .cas(s.cas())
                .build();
        stavka.setSkolaId(skolaId);
        stavkaRepo.save(stavka);
        return true;
    }

    @Transactional(readOnly = true)
    public List<RasporedStavkaResponse> mojRaspored(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        VerzijaRasporeda aktivna = verzijaRepo.findFirstBySkolaIdAndAktivanTrue(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktivna verzija rasporeda za skolu", skolaId));
        return stavkaRepo.mojRaspored(skolaId, aktivna.getId(), ja.id()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<rs.skola.platforma.raspored.web.VerzijaResponse> sveVerzije() {
        UUID skolaId = TenantContext.require();
        return verzijaRepo.findAllBySkolaIdOrderByCreatedAtDesc(skolaId).stream()
                .map(v -> new rs.skola.platforma.raspored.web.VerzijaResponse(
                        v.getId(),
                        v.getNaziv(),
                        v.getSkolskaGodina(),
                        v.getDatumOd(),
                        v.isAktivan(),
                        stavkaRepo.countBySkolaIdAndVerzija_Id(skolaId, v.getId()),
                        v.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public rs.skola.platforma.raspored.web.VerzijaResponse aktivirajVerziju(UUID verzijaId) {
        UUID skolaId = TenantContext.require();
        VerzijaRasporeda v = verzijaRepo.findById(verzijaId)
                .orElseThrow(() -> new ResourceNotFoundException("Verzija rasporeda", verzijaId));
        if (!skolaId.equals(v.getSkolaId())) {
            throw new rs.skola.platforma.common.exception.TenantViolationException();
        }
        verzijaRepo.deaktivirajSve(skolaId);
        v.setAktivan(true);
        return new rs.skola.platforma.raspored.web.VerzijaResponse(
                v.getId(), v.getNaziv(), v.getSkolskaGodina(), v.getDatumOd(),
                true, stavkaRepo.countBySkolaIdAndVerzija_Id(skolaId, v.getId()), v.getCreatedAt());
    }

    @Transactional
    public void obrisiVerziju(UUID verzijaId) {
        UUID skolaId = TenantContext.require();
        VerzijaRasporeda v = verzijaRepo.findById(verzijaId)
                .orElseThrow(() -> new ResourceNotFoundException("Verzija rasporeda", verzijaId));
        if (!skolaId.equals(v.getSkolaId())) {
            throw new rs.skola.platforma.common.exception.TenantViolationException();
        }
        verzijaRepo.delete(v);
    }

    private RasporedStavkaResponse toResponse(RasporedStavka rs) {
        Korisnik k = rs.getKorisnik();
        Odeljenje od = rs.getOdeljenje();
        return new RasporedStavkaResponse(
                rs.getId(),
                rs.getDan(),
                rs.getCas(),
                k == null ? null : k.getId(),
                // ime: ako je korisnik mapiran, koristi pun nalog; inace label iz XML-a
                k != null ? k.punoIme() : rs.getNastavnikLabel(),
                od == null ? null : od.getId(),
                od == null ? null : od.label(),
                rs.getPredmetLabel()
        );
    }

    private Map<String, Korisnik> indeksKorisnikaSkole(UUID skolaId) {
        Map<String, Korisnik> mapa = new HashMap<>();
        for (Korisnik k : korisnikRepo.findAllBySkolaIdAndUlogaOrderByPrezimeAscImeAsc(skolaId, Uloga.NASTAVNIK)) {
            for (String klj : kljuceviKorisnika(k)) {
                mapa.putIfAbsent(klj, k);
            }
        }
        return mapa;
    }

    private List<String> kljuceviKorisnika(Korisnik k) {
        String ime = normalizujZaPoredjenje(k.getIme());
        String prez = normalizujZaPoredjenje(k.getPrezime());
        return List.of(
                (ime + " " + prez).trim(),
                (prez + " " + ime).trim(),
                (prez + ", " + ime).trim(),
                normalizujZaPoredjenje(k.getUsername())
        );
    }

    private Korisnik pronadjiKorisnika(Map<String, Korisnik> indeks, String labelaIzXml) {
        if (labelaIzXml == null) return null;
        String norm = normalizujZaPoredjenje(labelaIzXml);
        Korisnik direktan = indeks.get(norm);
        if (direktan != null) return direktan;
        // Pokusaj da svedemo na "ime prezime" — uklanjamo zarez i visestruke razmake
        String pokusaj = norm.replace(",", " ").replaceAll("\\s+", " ");
        return indeks.get(pokusaj);
    }

    /**
     * Pripreman tekst za fuzzy poredjenje: lowercase, trim, transliteracija srpskih
     * latinickih dijakritika (č/ć/š/ž → c/c/s/z, đ → dj). Bez ovoga, "Kanlić Jelena"
     * iz XML-a ne moze da se sparu sa korisnickim profilom "Kanlic Jelena" u bazi.
     */
    private static String normalizujZaPoredjenje(String s) {
        if (s == null) return "";
        return s.toLowerCase().trim()
                .replace("č", "c").replace("ć", "c")
                .replace("š", "s").replace("ž", "z")
                .replace("đ", "dj");
    }

    private Map<String, Odeljenje> indeksOdeljenjaSkole(UUID skolaId, String skolskaGodina) {
        Map<String, Odeljenje> mapa = new HashMap<>();
        for (Odeljenje o : odeljenjeRepo.findAllBySkolaIdAndSkolskaGodinaOrderByRazredAscOznakaAsc(skolaId, skolskaGodina)) {
            mapa.put(kljucOdeljenja(o.getRazred(), o.getOznaka()), o);
        }
        return mapa;
    }

    private Odeljenje nadjiIliKreirajOdeljenje(Map<String, Odeljenje> indeks, UUID skolaId,
                                               String skolskaGodina, String labela) {
        Matcher m = ODELJENJE.matcher(labela);
        if (!m.matches()) {
            log.debug("Nepoznata oznaka odeljenja: '{}'", labela);
            return null;
        }
        Short razred = Short.valueOf(m.group("razred"));
        String oznaka = m.group("oznaka").toUpperCase();
        String klj = kljucOdeljenja(razred, oznaka);

        Odeljenje postojece = indeks.get(klj);
        if (postojece != null) return postojece;

        Odeljenje novo = Odeljenje.builder()
                .razred(razred)
                .oznaka(oznaka)
                .skolskaGodina(skolskaGodina)
                .aktivan(true)
                .build();
        novo.setSkolaId(skolaId);
        novo = odeljenjeRepo.save(novo);
        indeks.put(klj, novo);
        return novo;
    }

    private static String kljucOdeljenja(Short razred, String oznaka) {
        return razred + "-" + (oznaka == null ? "" : oznaka.toUpperCase());
    }
}
