package rs.skola.platforma.pp.domain;

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
import rs.skola.platforma.odeljenja.domain.Odeljenje;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pp_izvestaji")
public class PPIzvestaj extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staresina_id", nullable = false)
    private Korisnik staresina;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odeljenje_id", nullable = false)
    private Odeljenje odeljenje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PPPeriod period;

    @Column(name = "skolska_godina", nullable = false, length = 9)
    private String skolskaGodina;

    /** Slobodno strukturiran JSONB sa svim sekcijama izvestaja. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "podaci_json", columnDefinition = "jsonb")
    private Map<String, Object> podaci;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PPStatus status = PPStatus.NACRT;

    @Column(name = "podnet_at")
    private OffsetDateTime podnetAt;
}
