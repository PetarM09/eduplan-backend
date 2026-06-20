package rs.skola.platforma.planovi.export;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanFajlRepository extends JpaRepository<PlanFajl, UUID> {

    Optional<PlanFajl> findByPlanTipAndPlanIdAndFajlTip(PlanTip planTip, UUID planId, FajlTip fajlTip);
}
