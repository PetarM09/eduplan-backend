package rs.skola.platforma.rotacija.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.rotacija.domain.RotNedelja;

import java.util.List;
import java.util.UUID;

@Repository
public interface RotNedeljaRepository extends JpaRepository<RotNedelja, UUID> {

    List<RotNedelja> findAllByKonfiguracijaIdOrderByBrojNedeljeAsc(UUID konfiguracijaId);

    void deleteAllByKonfiguracijaId(UUID konfiguracijaId);
}
