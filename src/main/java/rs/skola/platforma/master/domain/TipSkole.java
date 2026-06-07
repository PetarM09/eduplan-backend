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
@Table(name = "tip_skole")
public class TipSkole extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String kod;

    @Column(nullable = false, length = 100)
    private String naziv;

    @Column(name = "ukupno_razreda", nullable = false)
    private Short ukupnoRazreda;
}
