package rs.skola.platforma.katalog.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.katalog.domain.Tema;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemaRepository extends JpaRepository<Tema, UUID> {

    Optional<Tema> findBySkolaIdAndPredmet_IdAndNazivIgnoreCase(UUID skolaId, UUID predmetId, String naziv);

    List<Tema> findAllBySkolaIdAndPredmet_IdOrderByRedniBrojAscNazivAsc(UUID skolaId, UUID predmetId);

    @Query("""
            SELECT t FROM Tema t
            WHERE t.skolaId = :skolaId
              AND t.predmet.id = :predmetId
              AND LOWER(t.naziv) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.redniBroj ASC, t.naziv ASC
            """)
    List<Tema> search(@Param("skolaId") UUID skolaId,
                      @Param("predmetId") UUID predmetId,
                      @Param("q") String q);
}
