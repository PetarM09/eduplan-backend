package rs.skola.platforma.zamene.web;

import java.util.UUID;

public record KandidatZamenikResponse(
        UUID korisnikId,
        String username,
        String ime,
        String prezime,
        /** Broj zamena u poslednjih 30 dana u kojima je nastavnik bio zamenik (manje = vise pozeljan). */
        long brojZamena30d
) {}
