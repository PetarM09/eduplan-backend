package rs.skola.platforma.master.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.skola.platforma.master.domain.ObrazovniProfil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObrazovniProfilRepository extends JpaRepository<ObrazovniProfil, UUID> {
    List<ObrazovniProfil> findAllByTipSkole_IdOrderByNazivAsc(UUID tipSkoleId);
    List<ObrazovniProfil> findAllByOrderByNazivAsc();
    Optional<ObrazovniProfil> findByKod(String kod);
}
