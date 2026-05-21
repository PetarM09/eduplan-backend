package rs.skola.platforma.katalog.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.katalog.domain.Ishod;

import java.util.List;
import java.util.UUID;

@Repository
public interface IshodRepository extends JpaRepository<Ishod, UUID> {

    List<Ishod> findAllBySkolaIdAndTema_IdOrderByCreatedAtAsc(UUID skolaId, UUID temaId);
}
