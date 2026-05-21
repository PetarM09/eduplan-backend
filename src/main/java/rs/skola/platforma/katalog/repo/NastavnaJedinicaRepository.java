package rs.skola.platforma.katalog.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.katalog.domain.NastavnaJedinica;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NastavnaJedinicaRepository extends JpaRepository<NastavnaJedinica, UUID> {

    Optional<NastavnaJedinica> findBySkolaIdAndTema_IdAndNazivIgnoreCase(UUID skolaId, UUID temaId, String naziv);

    List<NastavnaJedinica> findAllBySkolaIdAndTema_IdOrderByRedniBrojAscNazivAsc(UUID skolaId, UUID temaId);

    @Query("""
            SELECT j FROM NastavnaJedinica j
            WHERE j.skolaId = :skolaId
              AND j.tema.id = :temaId
              AND LOWER(j.naziv) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY j.redniBroj ASC, j.naziv ASC
            """)
    List<NastavnaJedinica> search(@Param("skolaId") UUID skolaId,
                                  @Param("temaId") UUID temaId,
                                  @Param("q") String q);
}
