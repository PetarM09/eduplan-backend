package rs.skola.platforma.zamene.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.skola.platforma.zamene.domain.Zamena;
import rs.skola.platforma.zamene.domain.ZamenaStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ZamenaRepository extends JpaRepository<Zamena, UUID> {

    @Query("""
            SELECT z FROM Zamena z
            LEFT JOIN FETCH z.odsutni
            LEFT JOIN FETCH z.zamenik
            LEFT JOIN FETCH z.odeljenje
            WHERE z.skolaId = :skolaId AND z.datum = :datum
            ORDER BY z.cas ASC
            """)
    List<Zamena> zameneDana(@Param("skolaId") UUID skolaId, @Param("datum") LocalDate datum);

    @Query("""
            SELECT z FROM Zamena z
            LEFT JOIN FETCH z.zamenik
            LEFT JOIN FETCH z.odeljenje
            WHERE z.skolaId = :skolaId AND z.odsutni.id = :korisnikId
            ORDER BY z.datum DESC, z.cas ASC
            """)
    List<Zamena> mojeKaoOdsutni(@Param("skolaId") UUID skolaId, @Param("korisnikId") UUID korisnikId);

    @Query("""
            SELECT z FROM Zamena z
            LEFT JOIN FETCH z.odsutni
            LEFT JOIN FETCH z.odeljenje
            WHERE z.skolaId = :skolaId AND z.zamenik.id = :korisnikId
            ORDER BY z.datum DESC, z.cas ASC
            """)
    List<Zamena> mojeKaoZamenik(@Param("skolaId") UUID skolaId, @Param("korisnikId") UUID korisnikId);

    @Query("""
            SELECT z FROM Zamena z
            LEFT JOIN FETCH z.odsutni
            LEFT JOIN FETCH z.zamenik
            LEFT JOIN FETCH z.odeljenje
            WHERE z.skolaId = :skolaId AND z.status = :status
            ORDER BY z.datum ASC, z.cas ASC
            """)
    List<Zamena> poStatusu(@Param("skolaId") UUID skolaId, @Param("status") ZamenaStatus status);

    /**
     * Broj zamena u kojima je nastavnik bio ZAMENIK u poslednjih {@code odDatuma} dana.
     * Sluzi za sortiranje kandidata — manje zamena = bolji kandidat (ravnomerno opterecenje).
     */
    @Query("""
            SELECT z.zamenik.id, COUNT(z) FROM Zamena z
            WHERE z.skolaId = :skolaId
              AND z.zamenik IS NOT NULL
              AND z.status IN (rs.skola.platforma.zamene.domain.ZamenaStatus.ODOBRENA,
                                rs.skola.platforma.zamene.domain.ZamenaStatus.PREDLOZENA)
              AND z.datum >= :odDatuma
              AND z.zamenik.id IN :korisnikIds
            GROUP BY z.zamenik.id
            """)
    List<Object[]> brojZamenaPoZameniku(@Param("skolaId") UUID skolaId,
                                        @Param("odDatuma") LocalDate odDatuma,
                                        @Param("korisnikIds") List<UUID> korisnikIds);

    /**
     * Da li nastavnik vec ima drugu zamenu (kao zamenik) za isti dan i cas?
     * Sprecava da jedan nastavnik bude predlozen dva puta za isti termin.
     */
    @Query("""
            SELECT z.zamenik.id FROM Zamena z
            WHERE z.skolaId = :skolaId
              AND z.datum = :datum AND z.cas = :cas
              AND z.zamenik IS NOT NULL
              AND z.status IN (rs.skola.platforma.zamene.domain.ZamenaStatus.ODOBRENA,
                                rs.skola.platforma.zamene.domain.ZamenaStatus.PREDLOZENA)
            """)
    List<UUID> zameniciVecPredlozeniZa(@Param("skolaId") UUID skolaId,
                                       @Param("datum") LocalDate datum,
                                       @Param("cas") Short cas);
}
