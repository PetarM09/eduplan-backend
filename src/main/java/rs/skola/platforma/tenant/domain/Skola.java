package rs.skola.platforma.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.skola.platforma.common.domain.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "mail_planovi", length = 255)
    private String mailPlanovi;

    @Column(nullable = false)
    @Builder.Default
    private boolean aktivan = true;

    @Column(name = "vazi_do")
    private LocalDate vaziDo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "obrazovni_profili_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> obrazovniProfiliIds = new ArrayList<>();

    public boolean jeAktivnaNa(LocalDate datum) {
        if (!aktivan) return false;
        return vaziDo == null || !datum.isAfter(vaziDo);
    }
}
