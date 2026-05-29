package rs.skola.platforma.katalog.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.katalog.domain.TipCasa;

import java.util.List;
import java.util.UUID;

@Repository
public interface TipCasaRepository extends JpaRepository<TipCasa, UUID> {

    @Query("""
            SELECT t FROM TipCasa t
            WHERE t.aktivan = true
              AND (t.skola IS NULL OR t.skola.id = :skolaId)
            ORDER BY t.skola NULLS FIRST, t.naziv ASC
            """)
    List<TipCasa> dostupniZaSkolu(@Param("skolaId") UUID skolaId);
}
