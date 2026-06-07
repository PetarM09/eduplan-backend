package rs.skola.platforma.predmeti.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.odeljenja.domain.Odeljenje;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "predmeti")
public class Predmet extends TenantAwareEntity {

    @Column(nullable = false, length = 255)
    private String naziv;

    private Short razred;

    @Column(name = "fond_casova")
    private Short fondCasova;

    @Column(name = "fond_teorija", nullable = false)
    @Builder.Default
    private Short fondTeorija = 0;

    @Column(name = "fond_vezbe", nullable = false)
    @Builder.Default
    private Short fondVezbe = 0;

    @Column(name = "fond_blok", nullable = false)
    @Builder.Default
    private Short fondBlok = 0;

    @Column(name = "master_predmet_id")
    private UUID masterPredmetId;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "predmet_odeljenja",
            joinColumns = @JoinColumn(name = "predmet_id"),
            inverseJoinColumns = @JoinColumn(name = "odeljenje_id")
    )
    @Builder.Default
    private Set<Odeljenje> odeljenja = new HashSet<>();
}
