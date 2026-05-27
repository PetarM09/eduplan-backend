package rs.skola.platforma.rotacija.web;

import rs.skola.platforma.raspored.domain.Dan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RotacijaResponse(
        UUID id,
        String naziv,
        UUID nastavnikId,
        String nastavnikIme,
        UUID odeljenjeId,
        String odeljenjeLabel,
        Short brojGrupa,
        Short brojNedelja,
        String skolskaGodina,
        List<PredmetResponse> predmeti,
        List<NedeljaResponse> nedelje,
        OffsetDateTime createdAt
) {

    public record PredmetResponse(
            UUID id,
            UUID profesorId,
            String profesorIme,
            String naziv,
            Short casovaNedeljno,
            Short redniBroj
    ) {}

    public record NedeljaResponse(
            Short brojNedelje,
            List<TerminDodela> termini
    ) {}

    public record TerminDodela(
            Dan dan,
            Short cas,
            UUID profesorId,
            String profesorIme,
            String predmetNaziv,
            Short brojGrupe
    ) {}
}
