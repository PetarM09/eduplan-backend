package rs.skola.platforma.common.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditDateTimeProvider")
public class AuditingConfig {

    private static final String SISTEM = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(SISTEM);
            }
            return Optional.of(auth.getName());
        };
    }

    /** Spring Data podrazumevano vraca LocalDateTime — nasi @CreatedDate/@LastModifiedDate su OffsetDateTime. */
    @Bean
    public DateTimeProvider auditDateTimeProvider() {
        return () -> Optional.<TemporalAccessor>of(OffsetDateTime.now());
    }
}
