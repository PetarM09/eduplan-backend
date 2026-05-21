package rs.skola.platforma.odeljenja.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.korisnici.domain.Korisnik;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "odeljenja")
public class Odeljenje extends TenantAwareEntity {

    @Column(nullable = false)
    private Short razred;

    @Column(nullable = false, length = 5)
    private String oznaka;

    @Column(name = "skolska_godina", nullable = false, length = 9)
    private String skolskaGodina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staresina_id")
    private Korisnik staresina;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = true;

    /** Ljudski-citljiv label, npr. "4-1" za numericku oznaku, "3A" za slovnu. */
    public String label() {
        if (oznaka == null || oznaka.isBlank()) return String.valueOf(razred);
        boolean numerickaOznaka = oznaka.chars().allMatch(Character::isDigit);
        return numerickaOznaka ? razred + "-" + oznaka : razred + oznaka;
    }
}
