package rs.skola.platforma.raspored.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.raspored.domain.RasporedStavka;

import java.util.List;
import java.util.UUID;

@Repository
public interface RasporedStavkaRepository extends JpaRepository<RasporedStavka, UUID> {

    @Query("""
            SELECT rs FROM RasporedStavka rs
            LEFT JOIN FETCH rs.odeljenje
            WHERE rs.skolaId = :skolaId
              AND rs.verzija.id = :verzijaId
              AND rs.korisnik.id = :korisnikId
            ORDER BY rs.dan ASC, rs.cas ASC
            """)
    List<RasporedStavka> mojRaspored(@Param("skolaId") UUID skolaId,
                                     @Param("verzijaId") UUID verzijaId,
                                     @Param("korisnikId") UUID korisnikId);

    @Query("""
            SELECT rs FROM RasporedStavka rs
            LEFT JOIN FETCH rs.korisnik
            LEFT JOIN FETCH rs.odeljenje
            WHERE rs.skolaId = :skolaId
              AND rs.verzija.id = :verzijaId
              AND rs.dan = :dan
            ORDER BY rs.cas ASC
            """)
    List<RasporedStavka> rasporedDana(@Param("skolaId") UUID skolaId,
                                      @Param("verzijaId") UUID verzijaId,
                                      @Param("dan") rs.skola.platforma.raspored.domain.Dan dan);

    /** UUID-ovi nastavnika koji imaju cas u datom dan/cas iz aktivne verzije rasporeda. */
    @Query("""
            SELECT DISTINCT rs.korisnik.id FROM RasporedStavka rs
            WHERE rs.skolaId = :skolaId
              AND rs.verzija.id = :verzijaId
              AND rs.dan = :dan
              AND rs.cas = :cas
            """)
    List<UUID> zauzetiNastavnici(@Param("skolaId") UUID skolaId,
                                 @Param("verzijaId") UUID verzijaId,
                                 @Param("dan") rs.skola.platforma.raspored.domain.Dan dan,
                                 @Param("cas") Short cas);

    /** Sve stavke nastavnika u datom danu (za prijavu odsustva) iz aktivne verzije. */
    @Query("""
            SELECT rs FROM RasporedStavka rs
            LEFT JOIN FETCH rs.odeljenje
            WHERE rs.skolaId = :skolaId
              AND rs.verzija.id = :verzijaId
              AND rs.korisnik.id = :korisnikId
              AND rs.dan = :dan
            ORDER BY rs.cas ASC
            """)
    List<RasporedStavka> casoviNastavnikaPoDanu(@Param("skolaId") UUID skolaId,
                                                @Param("verzijaId") UUID verzijaId,
                                                @Param("korisnikId") UUID korisnikId,
                                                @Param("dan") rs.skola.platforma.raspored.domain.Dan dan);

    /** Sve stavke jednog odeljenja iz aktivne verzije rasporeda — za detekciju termina vezbi. */
    @Query("""
            SELECT rs FROM RasporedStavka rs
            LEFT JOIN FETCH rs.korisnik
            WHERE rs.skolaId = :skolaId
              AND rs.verzija.id = :verzijaId
              AND rs.odeljenje.id = :odeljenjeId
            ORDER BY rs.dan ASC, rs.cas ASC
            """)
    List<RasporedStavka> sveZaOdeljenje(@Param("skolaId") UUID skolaId,
                                         @Param("verzijaId") UUID verzijaId,
                                         @Param("odeljenjeId") UUID odeljenjeId);

    long countBySkolaIdAndVerzija_Id(UUID skolaId, UUID verzijaId);
}
