package rs.skola.platforma.odeljenja.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.odeljenja.domain.Odeljenje;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OdeljenjeRepository extends JpaRepository<Odeljenje, UUID> {

    Optional<Odeljenje> findBySkolaIdAndRazredAndOznakaAndSkolskaGodina(
            UUID skolaId, Short razred, String oznaka, String skolskaGodina);

    List<Odeljenje> findAllBySkolaIdAndSkolskaGodinaOrderByRazredAscOznakaAsc(
            UUID skolaId, String skolskaGodina);

    List<Odeljenje> findAllBySkolaIdOrderByRazredAscOznakaAsc(UUID skolaId);
}
