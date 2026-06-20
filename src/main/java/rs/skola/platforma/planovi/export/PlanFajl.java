package rs.skola.platforma.planovi.export;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "plan_fajl",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_plan_fajl",
                columnNames = {"plan_tip", "plan_id", "fajl_tip"}
        )
)
public class PlanFajl extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tip", nullable = false, length = 20)
    private PlanTip planTip;

    @Column(name = "plan_id", nullable = false, columnDefinition = "uuid")
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fajl_tip", nullable = false, length = 10)
    private FajlTip fajlTip;

    @Column(name = "sadrzaj", nullable = false)
    private byte[] sadrzaj;
}
