package rs.skola.platforma.planovi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.BaseEntity;
import rs.skola.platforma.katalog.domain.Ishod;
import rs.skola.platforma.katalog.domain.MetodaRada;
import rs.skola.platforma.katalog.domain.NastavnaJedinica;
import rs.skola.platforma.katalog.domain.Tema;
import rs.skola.platforma.katalog.domain.TipCasa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "op_stavke")
public class OpStavka extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operativni_plan_id", nullable = false)
    private OperativniPlan operativniPlan;

    @Column(name = "redni_broj_casa", nullable = false)
    private Short redniBrojCasa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tema_id")
    private Tema tema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nastavna_jedinica_id")
    private NastavnaJedinica nastavnaJedinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tip_casa_id")
    private TipCasa tipCasa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metoda_rada_id")
    private MetodaRada metodaRada;

    @Column(columnDefinition = "TEXT")
    private String evaluacija;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "op_stavka_ishodi",
            joinColumns = @JoinColumn(name = "op_stavka_id"),
            inverseJoinColumns = @JoinColumn(name = "ishod_id")
    )
    @Builder.Default
    private Set<Ishod> ishodi = new HashSet<>();

    @OneToMany(mappedBy = "opStavka", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OpStavkaMedjupredmetno> medjupredmetno = new ArrayList<>();
}
