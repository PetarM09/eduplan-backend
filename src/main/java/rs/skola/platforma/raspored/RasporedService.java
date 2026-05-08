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

        List<String> nemapirani = new ArrayList<>();
        int mapirano = 0;
        int kreiranihStavki = 0;
        int kreiranihOdeljenja = 0;

        for (ParsedRasporedRed red : redovi) {
            Korisnik nastavnik = pronadjiKorisnika(indeksKorisnika, red.nastavnikLabel());
            if (nastavnik == null) {
                nemapirani.add(red.nastavnikLabel());
                continue;
            }
            mapirano++;

            for (ParsedRasporedRed.ParsedStavka s : red.stavke()) {
                Odeljenje od = nadjiIliKreirajOdeljenje(indeksOdeljenja, skolaId, skolskaGodina, s.odeljenjeLabel());
                if (od == null) continue;
                if (od.getId() == null) {
                    // novokreirano i sacuvano u nadjiIliKreirajOdeljenje
                    kreiranihOdeljenja++;
                }
                RasporedStavka stavka = RasporedStavka.builder()
                        .verzija(verzija)
                        .korisnik(nastavnik)
                        .odeljenje(od)
                        .predmetLabel(null)
                        .dan(s.dan())
                        .cas(s.cas())
                        .build();
                stavka.setSkolaId(skolaId);
                stavkaRepo.save(stavka);
                kreiranihStavki++;
            }
        }

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

    @Transactional(readOnly = true)
    public List<RasporedStavkaResponse> mojRaspored(CustomUserDetails ja) {
        UUID skolaId = TenantContext.require();
        VerzijaRasporeda aktivna = verzijaRepo.findFirstBySkolaIdAndAktivanTrue(skolaId)
                .orElseThrow(() -> new ResourceNotFoundException("Aktivna verzija rasporeda za skolu", skolaId));
        return stavkaRepo.mojRaspored(skolaId, aktivna.getId(), ja.id()).stream()
                .map(this::toResponse)
                .toList();
    }

    private RasporedStavkaResponse toResponse(RasporedStavka rs) {
        Korisnik k = rs.getKorisnik();
        Odeljenje od = rs.getOdeljenje();
        return new RasporedStavkaResponse(
                rs.getId(),
                rs.getDan(),
                rs.getCas(),
                k == null ? null : k.getId(),
                k == null ? null : k.punoIme(),
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
        String ime = (k.getIme() == null ? "" : k.getIme()).toLowerCase().trim();
        String prez = (k.getPrezime() == null ? "" : k.getPrezime()).toLowerCase().trim();
        return List.of(
                (ime + " " + prez).trim(),
                (prez + " " + ime).trim(),
                (prez + ", " + ime).trim(),
                k.getUsername().toLowerCase()
        );
    }

    private Korisnik pronadjiKorisnika(Map<String, Korisnik> indeks, String labelaIzXml) {
        if (labelaIzXml == null) return null;
        String norm = labelaIzXml.toLowerCase().trim();
        Korisnik direktan = indeks.get(norm);
        if (direktan != null) return direktan;
        // Pokusaj da svedemo na "ime prezime" — uklanjamo zarez i visestruke razmake
        String pokusaj = norm.replace(",", " ").replaceAll("\\s+", " ");
        return indeks.get(pokusaj);
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
