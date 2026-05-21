package rs.skola.platforma.pp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.skola.platforma.planovi.GodisnjiPlanService;
import rs.skola.platforma.planovi.OperativniPlanService;
import rs.skola.platforma.planovi.domain.PlanStatus;
import rs.skola.platforma.planovi.web.GodisnjiPlanResponse;
import rs.skola.platforma.planovi.web.OperativniPlanResponse;
import rs.skola.platforma.pp.web.PPDashboardResponse;
import rs.skola.platforma.pp.web.PPIzvestajResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PPDashboardService {

    private final GodisnjiPlanService godisnjiService;
    private final OperativniPlanService operativniService;
    private final PPService ppService;

    @Transactional(readOnly = true)
    public PPDashboardResponse dashboard(String skolskaGodina) {
        List<GodisnjiPlanResponse> godisnji = godisnjiService.sviZaSkolu(skolskaGodina, null);
        List<OperativniPlanResponse> operativni = operativniService.sviZaSkolu(
                skolskaGodina, null, null, null, null, null);
        List<PPIzvestajResponse> izvestaji = ppService.sviZaSkolu(skolskaGodina, null, null);

        return new PPDashboardResponse(
                skolskaGodina,
                godisnji.size(),
                operativni.size(),
                izvestaji.size(),
                grupisiPoStatusuG(godisnji),
                grupisiPoStatusuO(operativni),
                grupisiPoStatusuI(izvestaji),
                godisnji,
                operativni,
                izvestaji
        );
    }

    private Map<String, Integer> grupisiPoStatusuG(List<GodisnjiPlanResponse> planovi) {
        Map<String, Integer> mapa = praznaStatusMapa();
        for (GodisnjiPlanResponse p : planovi) {
            mapa.merge(p.status().name(), 1, Integer::sum);
        }
        return mapa;
    }

    private Map<String, Integer> grupisiPoStatusuO(List<OperativniPlanResponse> planovi) {
        Map<String, Integer> mapa = praznaStatusMapa();
        for (OperativniPlanResponse p : planovi) {
            mapa.merge(p.status().name(), 1, Integer::sum);
        }
        return mapa;
    }

    private Map<String, Integer> grupisiPoStatusuI(List<PPIzvestajResponse> izvestaji) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        mapa.put("NACRT", 0);
        mapa.put("PODNET", 0);
        mapa.put("PRIHVACEN", 0);
        mapa.put("VRACENO_NA_DORADU", 0);
        for (PPIzvestajResponse i : izvestaji) {
            mapa.merge(i.status().name(), 1, Integer::sum);
        }
        return mapa;
    }

    private Map<String, Integer> praznaStatusMapa() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (PlanStatus s : PlanStatus.values()) mapa.put(s.name(), 0);
        return mapa;
    }
}
