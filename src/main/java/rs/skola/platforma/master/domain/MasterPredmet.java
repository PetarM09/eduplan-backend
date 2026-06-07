package rs.skola.platforma.master.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "master_predmet")
public class MasterPredmet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obrazovni_profil_id", nullable = false)
    private ObrazovniProfil obrazovniProfil;

    @Column(nullable = false)
    private Short razred;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(name = "fond_teorija", nullable = false)
    @Builder.Default
    private Short fondTeorija = 0;

    @Column(name = "fond_vezbe", nullable = false)
    @Builder.Default
    private Short fondVezbe = 0;

    @Column(name = "fond_blok", nullable = false)
    @Builder.Default
    private Short fondBlok = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obavezan = true;

    private Short redosled;
}
