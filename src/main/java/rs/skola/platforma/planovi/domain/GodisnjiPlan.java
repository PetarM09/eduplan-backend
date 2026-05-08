package rs.skola.platforma.planovi.domain;

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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "godisnji_planovi")
public class GodisnjiPlan extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nastavnik_id", nullable = false)
    private Korisnik nastavnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predmet_id", nullable = false)
    private Predmet predmet;

    @Column(name = "skolska_godina", nullable = false, length = 9)
    private String skolskaGodina;

    private Short razred;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "odeljenja_json", columnDefinition = "jsonb")
    private List<UUID> odeljenjaIds;

    @Column(name = "ciljevi_zadaci", columnDefinition = "TEXT")
    private String ciljeviZadaci;

    @Column(length = 500)
    private String udzebenik;

    @Column(length = 500)
    private String autori;

    @Column(columnDefinition = "TEXT")
    private String literatura;

    @Column(name = "godisnji_fond")
    private Short godisnjiFond;

    @Column(name = "nedeljni_fond")
    private Short nedeljniFond;

    @Column(name = "dopunski_rad", columnDefinition = "TEXT")
    private String dopunskiRad;

    @Column(name = "dodatni_rad", columnDefinition = "TEXT")
    private String dodatniRad;

    @Column(columnDefinition = "TEXT")
    private String napomene;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlanStatus status = PlanStatus.NACRT;

    @Column(name = "word_fajl_putanja", length = 500)
    private String wordFajlPutanja;

    @Column(name = "pdf_fajl_putanja", length = 500)
    private String pdfFajlPutanja;

    @Column(name = "podnet_at")
    private OffsetDateTime podnetAt;

    @OneToMany(mappedBy = "godisnjiPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GodisnjiPlanTema> teme = new ArrayList<>();
}
