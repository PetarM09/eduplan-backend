package rs.skola.platforma.rotacija.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.rotacija.domain.Rotacija;

import java.util.List;
import java.util.UUID;

@Repository
public interface RotacijaRepository extends JpaRepository<Rotacija, UUID> {

    @Query("""
            SELECT r FROM Rotacija r
            LEFT JOIN FETCH r.nastavnik
            LEFT JOIN FETCH r.odeljenje
            WHERE r.skolaId = :skolaId
            ORDER BY r.createdAt DESC
            """)
    List<Rotacija> sveZaSkolu(@Param("skolaId") UUID skolaId);

    @Query("""
            SELECT DISTINCT r FROM Rotacija r
            LEFT JOIN FETCH r.nastavnik
            LEFT JOIN FETCH r.odeljenje
            WHERE r.skolaId = :skolaId
              AND EXISTS (
                  SELECT 1 FROM RotPredmet p
                  WHERE p.rotacija = r AND p.profesor.id = :profesorId
              )
            ORDER BY r.createdAt DESC
            """)
    List<Rotacija> rotacijeNastavnika(@Param("skolaId") UUID skolaId,
                                       @Param("profesorId") UUID profesorId);
}
