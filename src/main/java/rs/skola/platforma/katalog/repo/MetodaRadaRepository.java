package rs.skola.platforma.katalog.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.katalog.domain.MetodaRada;

import java.util.List;
import java.util.UUID;

@Repository
public interface MetodaRadaRepository extends JpaRepository<MetodaRada, UUID> {

    @Query("""
            SELECT m FROM MetodaRada m
            WHERE m.aktivan = true
              AND (m.skola IS NULL OR m.skola.id = :skolaId)
            ORDER BY m.skola NULLS FIRST, m.naziv ASC
            """)
    List<MetodaRada> dostupneZaSkolu(@Param("skolaId") UUID skolaId);
}
