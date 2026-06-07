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
@Table(name = "obrazovni_profil")
public class ObrazovniProfil extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tip_skole_id", nullable = false)
    private TipSkole tipSkole;

    @Column(nullable = false, unique = true, length = 120)
    private String kod;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(columnDefinition = "TEXT")
    private String opis;
}
