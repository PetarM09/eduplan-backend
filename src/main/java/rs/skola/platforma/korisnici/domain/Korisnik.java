package rs.skola.platforma.korisnici.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.BaseEntity;
import rs.skola.platforma.predmeti.domain.Predmet;
import rs.skola.platforma.tenant.domain.Skola;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    @Column(name = "lozinka_hash", length = 255)
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

    @Column(name = "pozivnica_token", unique = true)
    private UUID pozivnicaToken;

    @Column(name = "pozivnica_istice")
    private OffsetDateTime pozivnicaIstice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Poreklo poreklo = Poreklo.RUCNO;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "nastavnik_predmeti",
            joinColumns = @JoinColumn(name = "nastavnik_id"),
            inverseJoinColumns = @JoinColumn(name = "predmet_id")
    )
    @Builder.Default
    private Set<Predmet> predmeti = new HashSet<>();

    public boolean jePozvan() {
        return pozivnicaToken != null;
    }

    public String punoIme() {
        return ime + " " + prezime;
    }
}
