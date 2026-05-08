package rs.skola.platforma.raspored.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "verzije_rasporeda")
public class VerzijaRasporeda extends TenantAwareEntity {

    @Column(length = 255)
    private String naziv;

    @Column(name = "skolska_godina", length = 9)
    private String skolskaGodina;

    @Column(name = "datum_od")
    private LocalDate datumOd;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = false;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "xml_original", columnDefinition = "TEXT")
    private String xmlOriginal;
}
