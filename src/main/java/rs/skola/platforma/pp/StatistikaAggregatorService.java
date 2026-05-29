package rs.skola.platforma.pp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.common.tenant.TenantContext;
import rs.skola.platforma.pp.domain.PPIzvestaj;
import rs.skola.platforma.pp.domain.PPPeriod;
import rs.skola.platforma.pp.repo.PPIzvestajRepository;
import rs.skola.platforma.pp.web.StatistikaResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatistikaAggregatorService {

    private static final List<String> VLADANJE_KEYS =
            List.of("primerno", "vrloDobro", "dobro", "zadovoljavajuce", "nezadovoljavajuce");
    private static final List<String> USPEH_KEYS =
            List.of("odlican", "vrloDobar", "dobar", "dovoljan", "nedovoljan");

    private final PPIzvestajRepository repo;

    @Transactional(readOnly = true)
    public StatistikaResponse agregiraj(String skolskaGodina, PPPeriod period) {
        UUID skolaId = TenantContext.require();
        List<PPIzvestaj> izvestaji = repo.sviZaSkolu(skolaId, skolskaGodina, period, null);

        long ukupnoUcenika = 0, muski = 0, zenski = 0;
        long opravdane = 0, neopravdane = 0;
        Map<String, Long> vladanje = praznaDistribucija(VLADANJE_KEYS);
        Map<String, Long> uspeh = praznaDistribucija(USPEH_KEYS);
        int brojIzvestaja = izvestaji.size();

        for (PPIzvestaj i : izvestaji) {
            Map<String, Object> d = i.getPodaci();
            if (d == null) continue;
            ukupnoUcenika += longSafe(d.get("ukupnoUcenika"));
            muski += longSafe(d.get("ucenikaMuski"));
            zenski += longSafe(d.get("ucenikaZenski"));

            Map<String, Object> pris = mapa(d.get("prisustvo"));
            opravdane += longSafe(pris.get("opravdana"));
            neopravdane += longSafe(pris.get("neopravdana"));

            sabiraj(mapa(d.get("vladanje")), vladanje, VLADANJE_KEYS);
            sabiraj(mapa(d.get("uspeh")), uspeh, USPEH_KEYS);
        }

        return new StatistikaResponse(
                skolskaGodina, period, brojIzvestaja,
                ukupnoUcenika, muski, zenski,
                new StatistikaResponse.Prisustvo(opravdane, neopravdane),
                vladanje, uspeh
        );
    }

    private void sabiraj(Map<String, Object> src, Map<String, Long> dest, List<String> keys) {
        for (String k : keys) {
            dest.merge(k, longSafe(src.get(k)), Long::sum);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    private long longSafe(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }

    private Map<String, Long> praznaDistribucija(List<String> keys) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String k : keys) m.put(k, 0L);
        return m;
    }
}
