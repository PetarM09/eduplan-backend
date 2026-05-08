package rs.skola.platforma.tenant.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.tenant.domain.Skola;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkolaRepository extends JpaRepository<Skola, UUID> {

    boolean existsByNazivIgnoreCaseAndGradIgnoreCase(String naziv, String grad);

    List<Skola> findAllByOrderByNazivAsc();
}
