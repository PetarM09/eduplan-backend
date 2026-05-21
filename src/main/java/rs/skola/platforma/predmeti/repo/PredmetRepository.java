package rs.skola.platforma.predmeti.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.predmeti.domain.Predmet;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredmetRepository extends JpaRepository<Predmet, UUID> {

    List<Predmet> findAllBySkolaIdAndAktivanTrueOrderByRazredAscNazivAsc(UUID skolaId);

    List<Predmet> findAllBySkolaIdOrderByRazredAscNazivAsc(UUID skolaId);

    boolean existsBySkolaIdAndNazivIgnoreCaseAndRazred(UUID skolaId, String naziv, Short razred);
}
