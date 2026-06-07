package rs.skola.platforma.master.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.skola.platforma.master.domain.TipSkole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipSkoleRepository extends JpaRepository<TipSkole, UUID> {
    Optional<TipSkole> findByKod(String kod);
    List<TipSkole> findAllByOrderByNazivAsc();
}
