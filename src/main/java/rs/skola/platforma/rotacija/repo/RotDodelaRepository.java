package rs.skola.platforma.rotacija.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.rotacija.domain.RotDodela;

import java.util.UUID;

@Repository
public interface RotDodelaRepository extends JpaRepository<RotDodela, UUID> {

    @Modifying
    @Query("""
            UPDATE RotDodela d
            SET d.profesor = :korisnik
            WHERE d.rotacija.skolaId = :skolaId
              AND d.profesor IS NULL
              AND d.profesorLabel = :label
            """)
    int mapirajPoLabelu(@Param("skolaId") UUID skolaId,
                         @Param("label") String label,
                         @Param("korisnik") Korisnik korisnik);
}
