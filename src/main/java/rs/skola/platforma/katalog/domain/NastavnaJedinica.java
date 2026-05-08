package rs.skola.platforma.katalog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nastavne_jedinice")
public class NastavnaJedinica extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tema_id", nullable = false)
    private Tema tema;

    @Column(name = "redni_broj")
    private Short redniBroj;

    @Column(nullable = false, length = 500)
    private String naziv;
}
