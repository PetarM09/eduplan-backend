package rs.skola.platforma.rotacija.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.raspored.domain.Dan;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rot_dodele")
public class RotDodela {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rotacija_id", nullable = false)
    private Rotacija rotacija;

    @Column(name = "broj_nedelje", nullable = false)
    private Short brojNedelje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Dan dan;

    @Column(nullable = false)
    private Short cas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Korisnik profesor;

    @Column(name = "predmet_naziv", nullable = false, length = 255)
    private String predmetNaziv;

    @Column(name = "broj_grupe", nullable = false)
    private Short brojGrupe;
}
