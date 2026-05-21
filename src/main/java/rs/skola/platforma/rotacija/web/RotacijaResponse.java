package rs.skola.platforma.rotacija.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RotacijaResponse(
        UUID id,
        String naziv,
        UUID nastavnikId,
        String nastavnikIme,
        UUID predmetId,
        String predmetNaziv,
        Short grupaVelicina,
        Short casovaNedeljno,
        String skolskaGodina,
        List<OdeljenjeKratko> odeljenja,
        List<RotNedeljaResponse> nedelje,
        Statistika statistika
) {

    public record Statistika(
            boolean balansirano,
            int minCasovaPoOdeljenju,
            int maxCasovaPoOdeljenju,
            Map<UUID, Integer> casoviPoOdeljenju,
            int ukupnoNedelja
    ) {}
}
