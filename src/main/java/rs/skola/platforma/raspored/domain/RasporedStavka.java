package rs.skola.platforma.raspored.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.odeljenja.domain.Odeljenje;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "raspored_stavke")
public class RasporedStavka extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verzija_id", nullable = false)
    private VerzijaRasporeda verzija;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "korisnik_id")
    private Korisnik korisnik;

    @Column(name = "nastavnik_label", nullable = false, length = 255)
    private String nastavnikLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odeljenje_id")
    private Odeljenje odeljenje;

    @Column(name = "predmet_label", length = 255)
    private String predmetLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Dan dan;

    @Column(nullable = false)
    private Short cas;
}
