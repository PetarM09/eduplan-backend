package rs.skola.platforma.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "skole")
public class Skola extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String naziv;

    @Column(length = 100)
    private String grad;

    @Column(length = 255)
    private String adresa;

    /** Mail adresa skole na koju se salju generisani planovi. */
    @Column(name = "mail_planovi", length = 255)
    private String mailPlanovi;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = true;
}
