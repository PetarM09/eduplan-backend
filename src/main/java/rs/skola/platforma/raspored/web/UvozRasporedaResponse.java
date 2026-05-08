package rs.skola.platforma.raspored.web;

import java.util.List;
import java.util.UUID;

public record UvozRasporedaResponse(
        UUID verzijaId,
        String naziv,
        String skolskaGodina,
        int ukupnoRedova,
        int mapiranihNastavnika,
        int kreiranihStavki,
        int kreiranihOdeljenja,
        List<String> nemapiraniNastavnici
) {}
