package rs.skola.platforma.master.web;

import java.util.UUID;

public record ObrazovniProfilResponse(
        UUID id,
        UUID tipSkoleId,
        String tipSkoleNaziv,
        Short ukupnoRazreda,
        String kod,
        String naziv,
        String opis,
        long brojPredmeta
) {}
