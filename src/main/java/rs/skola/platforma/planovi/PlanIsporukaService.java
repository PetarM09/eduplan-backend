package rs.skola.platforma.planovi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanIsporukaService {

    private final PlanObradaService obradaService;

    @Async
    public void isporuciAsinhrono(UUID planId) {
        try {
            obradaService.obradi(planId);
        } catch (Exception ex) {
            log.error("Greska pri asinhronoj isporuci plana {}: {}", planId, ex.getMessage(), ex);
        }
    }
}
