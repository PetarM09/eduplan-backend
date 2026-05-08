package rs.skola.platforma.planovi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.skola.platforma.common.domain.BaseEntity;
import rs.skola.platforma.katalog.domain.Tema;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "godisnji_plan_teme")
public class GodisnjiPlanTema extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "godisnji_plan_id", nullable = false)
    private GodisnjiPlan godisnjiPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tema_id", nullable = false)
    private Tema tema;

    @Column(name = "redni_broj", nullable = false)
    @Builder.Default
    private Short redniBroj = 0;

    /** Mapa meseci -> da li se predaje. Kljucevi: IX, X, XI, XII, I, II, III, IV, V, VI. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meseci_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Boolean> meseci = new LinkedHashMap<>();

    @Column(name = "cas_obrada")
    @Builder.Default
    private Short casObrada = 0;

    @Column(name = "cas_utvrd")
    @Builder.Default
    private Short casUtvrd = 0;

    @Column(name = "cas_ostalo")
    @Builder.Default
    private Short casOstalo = 0;

    @Column(name = "ukupno_casova")
    @Builder.Default
    private Short ukupnoCasova = 0;
}
