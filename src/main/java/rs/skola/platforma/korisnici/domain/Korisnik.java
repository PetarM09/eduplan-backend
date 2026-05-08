package rs.skola.platforma.korisnici.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.BaseEntity;
import rs.skola.platforma.tenant.domain.Skola;

import java.time.OffsetDateTime;

/**
 * SUPER_ADMIN nema povezanu skolu (skola IS NULL); sve ostale uloge je obavezno imaju.
 * Ova invarijanta je dodatno potvrdjena CHECK ogranicenjem u V1 migraciji.
 * Korisnik nije TenantAwareEntity zato sto SUPER_ADMIN egzistira van skola.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "korisnici")
public class Korisnik extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skola_id")
    private Skola skola;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "lozinka_hash", nullable = false, length = 255)
    private String lozinkaHash;

    @Column(nullable = false, length = 100)
    private String ime;

    @Column(nullable = false, length = 100)
    private String prezime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Uloga uloga;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = true;

    @Column(name = "poslednji_login")
    private OffsetDateTime poslednjiLogin;

    public String punoIme() {
        return ime + " " + prezime;
    }
}
