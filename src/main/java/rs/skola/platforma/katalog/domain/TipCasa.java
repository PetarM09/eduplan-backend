package rs.skola.platforma.katalog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.BaseEntity;
import rs.skola.platforma.tenant.domain.Skola;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipovi_casa")
public class TipCasa extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skola_id")
    private Skola skola;

    @Column(nullable = false, length = 100)
    private String naziv;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = true;

    public boolean jeSistemski() {
        return skola == null;
    }
}
