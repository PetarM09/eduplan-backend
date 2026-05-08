package rs.skola.platforma.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Roditeljski entitet za sve tabele sa podacima koji pripadaju jednoj skoli.
 * skola_id je NOT NULL i postavlja se iz TenantContext-a u service sloju —
 * NIKADA se ne prihvata kao parametar od klijenta.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "skola_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID skolaId;
}
