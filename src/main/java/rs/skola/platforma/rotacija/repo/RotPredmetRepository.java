package rs.skola.platforma.rotacija.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.rotacija.domain.RotPredmet;

import java.util.List;
import java.util.UUID;

@Repository
public interface RotPredmetRepository extends JpaRepository<RotPredmet, UUID> {

    @Query("""
            SELECT DISTINCT p.profesorLabel FROM RotPredmet p
            WHERE p.rotacija.skolaId = :skolaId
              AND p.profesor IS NULL
            """)
    List<String> distinctNemapiraneLabels(@Param("skolaId") UUID skolaId);

    @Modifying
    @Query("""
            UPDATE RotPredmet p
            SET p.profesor = :korisnik
            WHERE p.rotacija.skolaId = :skolaId
              AND p.profesor IS NULL
              AND p.profesorLabel = :label
            """)
    int mapirajPoLabelu(@Param("skolaId") UUID skolaId,
                         @Param("label") String label,
                         @Param("korisnik") Korisnik korisnik);
}
