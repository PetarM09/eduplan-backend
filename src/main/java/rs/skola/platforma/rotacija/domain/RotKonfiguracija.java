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
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.predmeti.domain.Predmet;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rot_konfiguracije")
public class RotKonfiguracija extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nastavnik_id", nullable = false)
    private Korisnik nastavnik;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predmet_id")
    private Predmet predmet;

    @Column(nullable = false, length = 255)
    private String naziv;

    /** UUID-ovi odeljenja iz iste skole koji ucestvuju u rotaciji. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "odeljenja_json", columnDefinition = "jsonb", nullable = false)
    private List<UUID> odeljenjaIds;

    @Column(name = "grupa_velicina", nullable = false)
    @Builder.Default
    private Short grupaVelicina = 2;

    @Column(name = "casova_nedeljno", nullable = false)
    private Short casovaNedeljno;

    @Column(name = "skolska_godina", nullable = false, length = 9)
    private String skolskaGodina;
}
