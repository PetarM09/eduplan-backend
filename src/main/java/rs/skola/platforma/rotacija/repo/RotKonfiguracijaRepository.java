package rs.skola.platforma.rotacija.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.rotacija.domain.RotKonfiguracija;

import java.util.List;
import java.util.UUID;

@Repository
public interface RotKonfiguracijaRepository extends JpaRepository<RotKonfiguracija, UUID> {

    @Query("""
            SELECT k FROM RotKonfiguracija k
            LEFT JOIN FETCH k.nastavnik
            LEFT JOIN FETCH k.predmet
            WHERE k.skolaId = :skolaId
            ORDER BY k.createdAt DESC
            """)
    List<RotKonfiguracija> sveZaSkolu(@Param("skolaId") UUID skolaId);

    @Query("""
            SELECT k FROM RotKonfiguracija k
            LEFT JOIN FETCH k.predmet
            WHERE k.skolaId = :skolaId AND k.nastavnik.id = :nastavnikId
            ORDER BY k.createdAt DESC
            """)
    List<RotKonfiguracija> mojeKonfiguracije(@Param("skolaId") UUID skolaId,
                                             @Param("nastavnikId") UUID nastavnikId);
}
