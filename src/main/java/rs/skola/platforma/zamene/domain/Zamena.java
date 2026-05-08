package rs.skola.platforma.zamene.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.skola.platforma.common.domain.TenantAwareEntity;
import rs.skola.platforma.korisnici.domain.Korisnik;
import rs.skola.platforma.odeljenja.domain.Odeljenje;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zamene")
public class Zamena extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odsutni_id", nullable = false)
    private Korisnik odsutni;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zamenik_id")
    private Korisnik zamenik;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private Short cas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odeljenje_id")
    private Odeljenje odeljenje;

    @Column(name = "predmet_label", length = 255)
    private String predmetLabel;

    @Column(length = 500)
    private String razlog;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ZamenaStatus status = ZamenaStatus.PREDLOZENA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odobrio_id")
    private Korisnik odobrio;

    @Column(name = "odobrio_at")
    private OffsetDateTime odobrioAt;
}
