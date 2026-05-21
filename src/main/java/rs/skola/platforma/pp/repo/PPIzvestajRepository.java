package rs.skola.platforma.pp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.pp.domain.PPIzvestaj;
import rs.skola.platforma.pp.domain.PPPeriod;
import rs.skola.platforma.pp.domain.PPStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PPIzvestajRepository extends JpaRepository<PPIzvestaj, UUID> {

    Optional<PPIzvestaj> findBySkolaIdAndOdeljenje_IdAndPeriodAndSkolskaGodina(
            UUID skolaId, UUID odeljenjeId, PPPeriod period, String skolskaGodina);

    @Query("""
            SELECT i FROM PPIzvestaj i
            LEFT JOIN FETCH i.odeljenje
            LEFT JOIN FETCH i.staresina
            WHERE i.skolaId = :skolaId AND i.staresina.id = :staresinaId
              AND (:skolskaGodina IS NULL OR i.skolskaGodina = :skolskaGodina)
            ORDER BY i.skolskaGodina DESC, i.period ASC
            """)
    List<PPIzvestaj> mojiIzvestaji(@Param("skolaId") UUID skolaId,
                                    @Param("staresinaId") UUID staresinaId,
                                    @Param("skolskaGodina") String skolskaGodina);

    @Query("""
            SELECT i FROM PPIzvestaj i
            LEFT JOIN FETCH i.odeljenje
            LEFT JOIN FETCH i.staresina
            WHERE i.skolaId = :skolaId
              AND (:skolskaGodina IS NULL OR i.skolskaGodina = :skolskaGodina)
              AND (:period IS NULL OR i.period = :period)
              AND (:status IS NULL OR i.status = :status)
            ORDER BY i.skolskaGodina DESC, i.period ASC
            """)
    List<PPIzvestaj> sviZaSkolu(@Param("skolaId") UUID skolaId,
                                @Param("skolskaGodina") String skolskaGodina,
                                @Param("period") PPPeriod period,
                                @Param("status") PPStatus status);
}
