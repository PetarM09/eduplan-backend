package rs.skola.platforma.zamene.web;

import java.util.UUID;

public record KandidatZamenikResponse(
        UUID korisnikId,
        String username,
        String ime,
        String prezime,
        long brojZamena30d
) {}
