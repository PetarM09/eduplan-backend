package rs.skola.platforma.rotacija.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.odeljenja.domain.Odeljenje;

import java.util.ArrayList;
import java.util.List;

/**
 * Rotacioni raspored za grupe ucenika jednog odeljenja koji prati casove vezbi
 * (termini gde 2+ profesora istovremeno predaju istom odeljenju). Sastoji se od
 * liste predmeta po profesoru i generisanih dodela grupa po terminima i nedeljama.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rotacije")
public class Rotacija extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nastavnik_id", nullable = false)
    private Korisnik nastavnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odeljenje_id", nullable = false)
    private Odeljenje odeljenje;

    @Column(nullable = false, length = 255)
    private String naziv;

    @Column(name = "broj_grupa", nullable = false)
    private Short brojGrupa;

    @Column(name = "broj_nedelja", nullable = false)
    private Short brojNedelja;

    @Column(name = "skolska_godina", nullable = false, length = 9)
    private String skolskaGodina;

    @OneToMany(mappedBy = "rotacija", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RotPredmet> predmeti = new ArrayList<>();

    @OneToMany(mappedBy = "rotacija", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RotDodela> dodele = new ArrayList<>();
}
