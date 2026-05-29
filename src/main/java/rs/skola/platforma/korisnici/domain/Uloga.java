package rs.skola.platforma.korisnici.domain;

public enum Uloga {

    SUPER_ADMIN,

    KOORDINATOR,

    DIREKTOR,

    ADMIN,

    PP_SLUZBA,

    NASTAVNIK;

    public String authority() {
        return "ROLE_" + name();
    }
}
