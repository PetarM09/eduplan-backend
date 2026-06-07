package rs.skola.platforma.master.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.skola.platforma.master.domain.MasterPredmet;

import java.util.List;
import java.util.UUID;

public interface MasterPredmetRepository extends JpaRepository<MasterPredmet, UUID> {
    List<MasterPredmet> findAllByObrazovniProfil_IdOrderByRazredAscRedosledAscNazivAsc(UUID profilId);
    boolean existsByObrazovniProfil_IdAndRazredAndNaziv(UUID profilId, Short razred, String naziv);
    long countByObrazovniProfil_Id(UUID profilId);
}
