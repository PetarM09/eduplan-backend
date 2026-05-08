package rs.skola.platforma.korisnici.domain;

/**
 * Hijerarhija uloga u sistemu. SUPER_ADMIN je globalna (skola_id NULL),
 * sve ostale su uvek vezane za jednu skolu.
 */
public enum Uloga {

    /** Globalni administrator. Kreira skole i koordinatore za njih. */
    SUPER_ADMIN,

    /** Admin skole. Kreira sve ostale naloge u svojoj skoli. */
    KOORDINATOR,

    /** Direktor skole. Pregled svega u skoli, odobravanje zamena. */
    DIREKTOR,

    /** Operativni admin. Upravlja zamenama i uvozom rasporeda. */
    ADMIN,

    /** Pedagosko-psiholoska sluzba. Pregled planova i izvestaja. */
    PP_SLUZBA,

    /** Nastavnik. Sopstveni raspored, zamene, planovi, PP izvestaji za odeljenja gde je staresina. */
    NASTAVNIK;

    /** Vraca Spring Security ROLE_* string. */
    public String authority() {
        return "ROLE_" + name();
    }
}
