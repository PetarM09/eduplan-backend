package rs.skola.platforma.korisnici.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.korisnici.domain.Uloga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KorisnikRepository extends JpaRepository<Korisnik, UUID> {

    @Query("SELECT k FROM Korisnik k LEFT JOIN FETCH k.skola WHERE LOWER(k.username) = LOWER(:username)")
    Optional<Korisnik> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<Korisnik> findAllBySkolaIdAndUlogaOrderByPrezimeAscImeAsc(UUID skolaId, Uloga uloga);

    List<Korisnik> findAllBySkolaIdOrderByPrezimeAscImeAsc(UUID skolaId);
}
