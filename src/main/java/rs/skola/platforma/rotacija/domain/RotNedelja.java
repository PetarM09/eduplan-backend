package rs.skola.platforma.rotacija.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.skola.platforma.common.domain.TenantAwareEntity;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rot_nedelje")
public class RotNedelja extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "konfiguracija_id", nullable = false)
    private RotKonfiguracija konfiguracija;

    @Column(name = "broj_nedelje", nullable = false)
    private Short brojNedelje;

    /** UUID-ovi odeljenja koji ulaze u tu nedelju. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "odeljenja_json", columnDefinition = "jsonb", nullable = false)
    private List<UUID> odeljenjaIds;
}
