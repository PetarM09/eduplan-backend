package rs.skola.platforma.katalog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.predmeti.domain.Predmet;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teme")
public class Tema extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predmet_id", nullable = false)
    private Predmet predmet;

    @Column(name = "redni_broj", nullable = false)
    @Builder.Default
    private Short redniBroj = 0;

    @Column(nullable = false, length = 500)
    private String naziv;

    @Column(name = "cas_obrada", nullable = false)
    @Builder.Default
    private Short casObrada = 0;

    @Column(name = "cas_utvrd", nullable = false)
    @Builder.Default
    private Short casUtvrd = 0;

    @Column(name = "cas_ostalo", nullable = false)
    @Builder.Default
    private Short casOstalo = 0;
}
