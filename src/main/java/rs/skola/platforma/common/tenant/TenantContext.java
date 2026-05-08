package rs.skola.platforma.common.tenant;

import java.util.UUID;

/**
 * ThreadLocal kontekst koji nosi UUID skole trenutnog korisnika tokom HTTP zahteva.
 * Postavlja se u JwtAuthenticationFilter-u nakon validacije tokena, a OBAVEZNO
 * se cisti u finally bloku istog filtera — bez toga bi sledeci zahtev koji
 * obradjuje isti thread (Tomcat pool ili virtual thread reuse) video pogresnu skolu.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID skolaId) {
        CURRENT.set(skolaId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException(
                    "TenantContext nije postavljen — endpoint zahteva autentifikovanog korisnika sa skolom");
        }
        return id;
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
