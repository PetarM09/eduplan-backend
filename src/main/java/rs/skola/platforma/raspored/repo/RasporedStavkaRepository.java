package rs.skola.platforma.raspored.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.korisnici.domain.Korisnik;
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

    @Query("""
            SELECT COUNT(DISTINCT rs.nastavnikLabel) FROM RasporedStavka rs
            WHERE rs.skolaId = :skolaId AND rs.verzija.id = :verzijaId
            """)
    long brojProfesoraPoVerziji(@Param("skolaId") UUID skolaId, @Param("verzijaId") UUID verzijaId);

    @Query("""
            SELECT rs.nastavnikLabel, COUNT(rs)
            FROM RasporedStavka rs
            WHERE rs.skolaId = :skolaId
              AND rs.korisnik IS NULL
            GROUP BY rs.nastavnikLabel
            ORDER BY rs.nastavnikLabel ASC
            """)
    List<Object[]> nemapiraniSaCount(@Param("skolaId") UUID skolaId);

    @Modifying
    @Query("""
            UPDATE RasporedStavka rs
            SET rs.korisnik = :korisnik
            WHERE rs.skolaId = :skolaId
              AND rs.korisnik IS NULL
              AND rs.nastavnikLabel = :label
            """)
    int mapirajPoLabelu(@Param("skolaId") UUID skolaId,
                         @Param("label") String label,
                         @Param("korisnik") Korisnik korisnik);

    @Query("SELECT DISTINCT rs.nastavnikLabel FROM RasporedStavka rs " +
            "WHERE rs.skolaId = :skolaId AND rs.korisnik IS NULL")
    List<String> distinctNemapiraneLabels(@Param("skolaId") UUID skolaId);
}
