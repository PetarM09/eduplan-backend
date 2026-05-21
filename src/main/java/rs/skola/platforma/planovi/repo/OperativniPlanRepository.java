package rs.skola.platforma.planovi.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.planovi.domain.OperativniPlan;
import rs.skola.platforma.planovi.domain.PlanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperativniPlanRepository extends JpaRepository<OperativniPlan, UUID> {

    Optional<OperativniPlan> findBySkolaIdAndNastavnik_IdAndPredmet_IdAndOdeljenje_IdAndMesecAndSkolskaGodina(
            UUID skolaId, UUID nastavnikId, UUID predmetId, UUID odeljenjeId, Short mesec, String skolskaGodina);

    @Query("""
            SELECT p FROM OperativniPlan p
            LEFT JOIN FETCH p.predmet
            LEFT JOIN FETCH p.odeljenje
            LEFT JOIN FETCH p.nastavnik
            WHERE p.skolaId = :skolaId AND p.nastavnik.id = :nastavnikId
              AND (:mesec IS NULL OR p.mesec = :mesec)
              AND (:predmetId IS NULL OR p.predmet.id = :predmetId)
              AND (:skolskaGodina IS NULL OR p.skolskaGodina = :skolskaGodina)
            ORDER BY p.skolskaGodina DESC, p.mesec ASC, p.createdAt DESC
            """)
    List<OperativniPlan> mojiPlanovi(@Param("skolaId") UUID skolaId,
                                     @Param("nastavnikId") UUID nastavnikId,
                                     @Param("mesec") Short mesec,
                                     @Param("predmetId") UUID predmetId,
                                     @Param("skolskaGodina") String skolskaGodina);

    @Query("""
            SELECT p FROM OperativniPlan p
            LEFT JOIN FETCH p.predmet
            LEFT JOIN FETCH p.odeljenje
            LEFT JOIN FETCH p.nastavnik
            WHERE p.skolaId = :skolaId
              AND (:skolskaGodina IS NULL OR p.skolskaGodina = :skolskaGodina)
              AND (:mesec IS NULL OR p.mesec = :mesec)
              AND (:nastavnikId IS NULL OR p.nastavnik.id = :nastavnikId)
              AND (:predmetId IS NULL OR p.predmet.id = :predmetId)
              AND (:odeljenjeId IS NULL OR p.odeljenje.id = :odeljenjeId)
              AND (:status IS NULL OR p.status = :status)
            ORDER BY p.skolskaGodina DESC, p.mesec ASC, p.createdAt DESC
            """)
    List<OperativniPlan> sviZaSkolu(@Param("skolaId") UUID skolaId,
                                    @Param("skolskaGodina") String skolskaGodina,
                                    @Param("mesec") Short mesec,
                                    @Param("nastavnikId") UUID nastavnikId,
                                    @Param("predmetId") UUID predmetId,
                                    @Param("odeljenjeId") UUID odeljenjeId,
                                    @Param("status") PlanStatus status);

    @Query("""
            SELECT p FROM OperativniPlan p
            LEFT JOIN FETCH p.predmet
            LEFT JOIN FETCH p.odeljenje
            LEFT JOIN FETCH p.nastavnik
            LEFT JOIN FETCH p.stavke s
            LEFT JOIN FETCH s.tema
            LEFT JOIN FETCH s.nastavnaJedinica
            LEFT JOIN FETCH s.tipCasa
            LEFT JOIN FETCH s.metodaRada
            WHERE p.id = :id
            """)
    Optional<OperativniPlan> findByIdSaStavkama(@Param("id") UUID id);
}
