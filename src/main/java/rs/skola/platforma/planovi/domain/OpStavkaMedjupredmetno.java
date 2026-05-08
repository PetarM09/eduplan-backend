package rs.skola.platforma.planovi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.BaseEntity;
import rs.skola.platforma.predmeti.domain.Predmet;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "op_stavka_medjupredmetno")
public class OpStavkaMedjupredmetno extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "op_stavka_id", nullable = false)
    private OpStavka opStavka;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predmet_id", nullable = false)
    private Predmet predmet;

    @Column(name = "opis_kompetencije", columnDefinition = "TEXT")
    private String opisKompetencije;
}
