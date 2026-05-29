package rs.skola.platforma.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "skola_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID skolaId;
}
