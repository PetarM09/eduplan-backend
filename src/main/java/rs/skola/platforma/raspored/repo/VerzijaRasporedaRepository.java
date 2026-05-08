package rs.skola.platforma.raspored.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.raspored.domain.VerzijaRasporeda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerzijaRasporedaRepository extends JpaRepository<VerzijaRasporeda, UUID> {

    Optional<VerzijaRasporeda> findFirstBySkolaIdAndAktivanTrue(UUID skolaId);

    List<VerzijaRasporeda> findAllBySkolaIdOrderByCreatedAtDesc(UUID skolaId);

    @Modifying
    @Query("UPDATE VerzijaRasporeda v SET v.aktivan = false WHERE v.skolaId = :skolaId AND v.aktivan = true")
    int deaktivirajSve(@Param("skolaId") UUID skolaId);
}
