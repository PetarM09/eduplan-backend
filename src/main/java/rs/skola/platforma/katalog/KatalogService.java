package rs.skola.platforma.katalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.exception.ResourceNotFoundException;
import rs.skola.platforma.common.exception.TenantViolationException;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.katalog.domain.Ishod;
import rs.skola.platforma.katalog.domain.MetodaRada;
import rs.skola.platforma.katalog.domain.NastavnaJedinica;
import rs.skola.platforma.katalog.domain.Tema;
import rs.skola.platforma.katalog.domain.TipCasa;
import rs.skola.platforma.katalog.repo.IshodRepository;
import rs.skola.platforma.katalog.repo.MetodaRadaRepository;
import rs.skola.platforma.katalog.repo.NastavnaJedinicaRepository;
import rs.skola.platforma.katalog.repo.TemaRepository;
import rs.skola.platforma.katalog.repo.TipCasaRepository;
import rs.skola.platforma.katalog.web.IshodResponse;
import rs.skola.platforma.katalog.web.NastavnaJedinicaResponse;
import rs.skola.platforma.katalog.web.PadajuciMeniResponse;
import rs.skola.platforma.katalog.web.TemaResponse;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.predmeti.repo.PredmetRepository;

import java.util.List;
import java.util.UUID;

/**
 * Centralni "pametni katalog" za teme, nastavne jedinice, ishode i padajuce menije.
 *
 * <p><b>findOrCreate pattern</b> — kljuc Sprint 3:
 * pri svakom upisu plana, sistem provari da li tema/jedinica vec postoji u katalogu
 * (po nazivu, vezana za isti predmet/temu i skolu). Ako da — reuse-uje postojeci zapis;
 * ako ne — kreira novi i automatski cuva za buducu upotrebu. Eliminise duplikate i
 * gradi "biblioteku znanja" skole bez dodatnih radnji nastavnika.
 *
 * <p>Sva poredjenja su case-insensitive zbog UNIQUE constraint-a na (skola, predmet, LOWER(naziv)).
 */
@Service
@RequiredArgsConstructor
public class KatalogService {

    private final TemaRepository temaRepo;
    private final NastavnaJedinicaRepository jedinicaRepo;
    private final IshodRepository ishodRepo;
    private final TipCasaRepository tipCasaRepo;
    private final MetodaRadaRepository metodaRepo;
    private final PredmetRepository predmetRepo;

    // -------- TEME --------

    @Transactional(readOnly = true)
    public List<TemaResponse> temePredmeta(UUID predmetId) {
        UUID skolaId = TenantContext.require();
        proveriPredmet(predmetId, skolaId);
        return temaRepo.findAllBySkolaIdAndPredmet_IdOrderByRedniBrojAscNazivAsc(skolaId, predmetId).stream()
                .map(this::toResp)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemaResponse> pretragaTema(UUID predmetId, String q) {
        UUID skolaId = TenantContext.require();
        proveriPredmet(predmetId, skolaId);
        String upit = q == null ? "" : q.trim();
        if (upit.isEmpty()) {
            return temePredmeta(predmetId);
        }
        return temaRepo.search(skolaId, predmetId, upit).stream().map(this::toResp).toList();
    }

    /**
     * Vraca postojecu temu po nazivu ili kreira novu. Auto-save u katalog kao
     * posledica izrade plana.
     */
    @Transactional
    public Tema findOrCreateTema(UUID predmetId, String naziv, Short redniBroj,
                                  Short casObrada, Short casUtvrd, Short casOstalo) {
        UUID skolaId = TenantContext.require();
        Predmet predmet = proveriPredmet(predmetId, skolaId);
        String normNaziv = naziv == null ? "" : naziv.trim();

        return temaRepo.findBySkolaIdAndPredmet_IdAndNazivIgnoreCase(skolaId, predmetId, normNaziv)
                .orElseGet(() -> {
                    Tema nova = Tema.builder()
                            .predmet(predmet)
                            .naziv(normNaziv)
                            .redniBroj(redniBroj == null ? 0 : redniBroj)
                            .casObrada(casObrada == null ? 0 : casObrada)
                            .casUtvrd(casUtvrd == null ? 0 : casUtvrd)
                            .casOstalo(casOstalo == null ? 0 : casOstalo)
                            .build();
                    nova.setSkolaId(skolaId);
                    return temaRepo.save(nova);
                });
    }

    // -------- NASTAVNE JEDINICE --------

    @Transactional(readOnly = true)
    public List<NastavnaJedinicaResponse> jediniceTeme(UUID temaId) {
        UUID skolaId = TenantContext.require();
        proveriTemu(temaId, skolaId);
        return jedinicaRepo.findAllBySkolaIdAndTema_IdOrderByRedniBrojAscNazivAsc(skolaId, temaId).stream()
                .map(this::toResp)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NastavnaJedinicaResponse> pretragaJedinica(UUID temaId, String q) {
        UUID skolaId = TenantContext.require();
        proveriTemu(temaId, skolaId);
        String upit = q == null ? "" : q.trim();
        if (upit.isEmpty()) return jediniceTeme(temaId);
        return jedinicaRepo.search(skolaId, temaId, upit).stream().map(this::toResp).toList();
    }

    @Transactional
    public NastavnaJedinica findOrCreateJedinica(UUID temaId, String naziv, Short redniBroj) {
        UUID skolaId = TenantContext.require();
        Tema tema = proveriTemu(temaId, skolaId);
        String normNaziv = naziv == null ? "" : naziv.trim();

        return jedinicaRepo.findBySkolaIdAndTema_IdAndNazivIgnoreCase(skolaId, temaId, normNaziv)
                .orElseGet(() -> {
                    NastavnaJedinica nova = NastavnaJedinica.builder()
                            .tema(tema)
                            .naziv(normNaziv)
                            .redniBroj(redniBroj)
                            .build();
                    nova.setSkolaId(skolaId);
                    return jedinicaRepo.save(nova);
                });
    }

    // -------- ISHODI --------

    @Transactional(readOnly = true)
    public List<IshodResponse> ishodiTeme(UUID temaId) {
        UUID skolaId = TenantContext.require();
        proveriTemu(temaId, skolaId);
        return ishodRepo.findAllBySkolaIdAndTema_IdOrderByCreatedAtAsc(skolaId, temaId).stream()
                .map(this::toResp)
                .toList();
    }

    /**
     * Ishodi su free-text, pa za njih nema UNIQUE constraint-a. Da bismo izbegli
     * trivijalne duplikate, normalizujemo whitespace i tek onda upisujemo.
     */
    @Transactional
    public Ishod kreirajIshod(UUID temaId, String opis) {
        UUID skolaId = TenantContext.require();
        Tema tema = proveriTemu(temaId, skolaId);
        String norm = opis == null ? "" : opis.trim().replaceAll("\\s+", " ");
        Ishod novi = Ishod.builder()
                .tema(tema)
                .opis(norm)
                .build();
        novi.setSkolaId(skolaId);
        return ishodRepo.save(novi);
    }

    // -------- PADAJUCI MENIJI --------

    @Transactional(readOnly = true)
    public List<PadajuciMeniResponse> tipoviCasa() {
        UUID skolaId = TenantContext.require();
        return tipCasaRepo.dostupniZaSkolu(skolaId).stream()
                .map(t -> new PadajuciMeniResponse(t.getId(), t.getNaziv(), t.jeSistemski()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PadajuciMeniResponse> metodeRada() {
        UUID skolaId = TenantContext.require();
        return metodaRepo.dostupneZaSkolu(skolaId).stream()
                .map(m -> new PadajuciMeniResponse(m.getId(), m.getNaziv(), m.jeSistemska()))
                .toList();
    }

    public TipCasa nadjiTipCasa(UUID id) {
        UUID skolaId = TenantContext.require();
        TipCasa t = tipCasaRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tip casa", id));
        if (t.getSkola() != null && !skolaId.equals(t.getSkola().getId())) {
            throw new TenantViolationException();
        }
        return t;
    }

    public MetodaRada nadjiMetodu(UUID id) {
        UUID skolaId = TenantContext.require();
        MetodaRada m = metodaRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metoda rada", id));
        if (m.getSkola() != null && !skolaId.equals(m.getSkola().getId())) {
            throw new TenantViolationException();
        }
        return m;
    }

    public Tema nadjiTemu(UUID temaId) {
        return proveriTemu(temaId, TenantContext.require());
    }

    public Ishod nadjiIshod(UUID id) {
        UUID skolaId = TenantContext.require();
        Ishod i = ishodRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ishod", id));
        if (!skolaId.equals(i.getSkolaId())) {
            throw new TenantViolationException();
        }
        return i;
    }

    // -------- helpers --------

    private Predmet proveriPredmet(UUID predmetId, UUID skolaId) {
        Predmet p = predmetRepo.findById(predmetId)
                .orElseThrow(() -> new ResourceNotFoundException("Predmet", predmetId));
        if (!skolaId.equals(p.getSkolaId())) {
            throw new TenantViolationException();
        }
        return p;
    }

    private Tema proveriTemu(UUID temaId, UUID skolaId) {
        Tema t = temaRepo.findById(temaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tema", temaId));
        if (!skolaId.equals(t.getSkolaId())) {
            throw new TenantViolationException();
        }
        return t;
    }

    private TemaResponse toResp(Tema t) {
        return new TemaResponse(t.getId(), t.getPredmet().getId(), t.getRedniBroj(), t.getNaziv(),
                t.getCasObrada(), t.getCasUtvrd(), t.getCasOstalo());
    }

    private NastavnaJedinicaResponse toResp(NastavnaJedinica j) {
        return new NastavnaJedinicaResponse(j.getId(), j.getTema().getId(), j.getRedniBroj(), j.getNaziv());
    }

    private IshodResponse toResp(Ishod i) {
        return new IshodResponse(i.getId(), i.getTema().getId(), i.getOpis());
    }
}
