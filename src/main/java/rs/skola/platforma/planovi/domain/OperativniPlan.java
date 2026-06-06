package rs.skola.platforma.planovi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.odeljenja.domain.Odeljenje;
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
@Table(name = "operativni_planovi")
public class OperativniPlan extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nastavnik_id", nullable = false)
    private Korisnik nastavnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predmet_id", nullable = false)
    private Predmet predmet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odeljenje_id", nullable = false)
    private Odeljenje odeljenje;

    @Column(nullable = false)
    private Short mesec;

    @Column(name = "skolska_godina", nullable = false, length = 9)
    private String skolskaGodina;

    @Column(name = "nedeljni_fond")
    private Short nedeljniFond;

    @Column(name = "samoprocena_ishoda", columnDefinition = "TEXT")
    private String samoprocenaIshoda;

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

    @Column(name = "razlog_vracanja", columnDefinition = "TEXT")
    private String razlogVracanja;

    @Column(name = "odobren_at")
    private OffsetDateTime odobrenAt;

    @Column(name = "odobrio_id")
    private UUID odobrioId;

    @Column(name = "vracen_at")
    private OffsetDateTime vracenAt;

    @Column(name = "vratio_id")
    private UUID vratioId;

    @OneToMany(mappedBy = "operativniPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("redniBrojCasa ASC")
    @Builder.Default
    private List<OpStavka> stavke = new ArrayList<>();
}
