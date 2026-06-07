package rs.skola.platforma.pozivnice.web;

import rs.skola.platforma.korisnici.domain.Poreklo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PozvaniKorisnikResponse(
        UUID id,
        String ime,
        String prezime,
        String username,
        String email,
        Poreklo poreklo,
        boolean imaPozivnicu,
        OffsetDateTime pozivnicaIstice,
        List<UUID> predmetiIds,
        List<String> predmetiNazivi,
        List<String> odeljenjaIzRasporeda
) {}
