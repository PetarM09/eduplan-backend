package rs.skola.platforma.korisnici.web;

import rs.skola.platforma.korisnici.domain.Uloga;

import java.util.UUID;

public record KorisnikResponse(
        UUID id,
        UUID skolaId,
        String username,
        String email,
        String ime,
        String prezime,
        Uloga uloga,
        boolean aktivan
) {}
