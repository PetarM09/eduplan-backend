package rs.skola.platforma.planovi.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.planovi.domain.GodisnjiPlan;
import rs.skola.platforma.planovi.domain.PlanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GodisnjiPlanRepository extends JpaRepository<GodisnjiPlan, UUID> {

    Optional<GodisnjiPlan> findBySkolaIdAndNastavnik_IdAndPredmet_IdAndSkolskaGodina(
            UUID skolaId, UUID nastavnikId, UUID predmetId, String skolskaGodina);

    @Query("""
            SELECT p FROM GodisnjiPlan p
            LEFT JOIN FETCH p.predmet
            LEFT JOIN FETCH p.nastavnik
            WHERE p.skolaId = :skolaId AND p.nastavnik.id = :nastavnikId
            ORDER BY p.skolskaGodina DESC, p.createdAt DESC
            """)
    List<GodisnjiPlan> mojiPlanovi(@Param("skolaId") UUID skolaId,
                                   @Param("nastavnikId") UUID nastavnikId);

    @Query("""
            SELECT p FROM GodisnjiPlan p
            LEFT JOIN FETCH p.predmet
            LEFT JOIN FETCH p.nastavnik
            WHERE p.skolaId = :skolaId
              AND (:skolskaGodina IS NULL OR p.skolskaGodina = :skolskaGodina)
              AND (:status IS NULL OR p.status = :status)
            ORDER BY p.skolskaGodina DESC, p.createdAt DESC
            """)
    List<GodisnjiPlan> sviZaSkolu(@Param("skolaId") UUID skolaId,
                                  @Param("skolskaGodina") String skolskaGodina,
                                  @Param("status") PlanStatus status);

    @Query("""
            SELECT p FROM GodisnjiPlan p
            LEFT JOIN FETCH p.predmet
            LEFT JOIN FETCH p.nastavnik
            LEFT JOIN FETCH p.teme t
            LEFT JOIN FETCH t.tema
            WHERE p.id = :id
            """)
    Optional<GodisnjiPlan> findByIdSaTemama(@Param("id") UUID id);
}
