package rs.skola.platforma.pp.web;

import rs.skola.platforma.planovi.web.GodisnjiPlanResponse;
import rs.skola.platforma.planovi.web.OperativniPlanResponse;

import java.util.List;
import java.util.Map;

public record PPDashboardResponse(
        String skolskaGodina,
        int ukupnoGodisnjihPlanova,
        int ukupnoOperativnihPlanova,
        int ukupnoIzvestaja,
        Map<String, Integer> godisnjiPoStatusu,
        Map<String, Integer> operativniPoStatusu,
        Map<String, Integer> izvestajiPoStatusu,
        List<GodisnjiPlanResponse> godisnjiPlanovi,
        List<OperativniPlanResponse> operativniPlanovi,
        List<PPIzvestajResponse> izvestaji
) {}
