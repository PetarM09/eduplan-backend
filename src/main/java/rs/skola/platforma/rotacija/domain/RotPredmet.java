package rs.skola.platforma.rotacija.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.korisnici.domain.Korisnik;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rot_predmeti")
public class RotPredmet {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rotacija_id", nullable = false)
    private Rotacija rotacija;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id")
    private Korisnik profesor;

    @Column(name = "profesor_label", nullable = false, length = 255)
    private String profesorLabel;

    @Column(nullable = false, length = 255)
    private String naziv;

    @Column(name = "casova_nedeljno", nullable = false)
    private Short casovaNedeljno;

    @Column(name = "redni_broj", nullable = false)
    @Builder.Default
    private Short redniBroj = 1;
}
