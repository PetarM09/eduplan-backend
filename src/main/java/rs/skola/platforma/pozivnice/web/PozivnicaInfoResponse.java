package rs.skola.platforma.pozivnice.web;

public record PozivnicaInfoResponse(
        String ime,
        String prezime,
        String email,
        String skolaNaziv,
        boolean istekla
) {}
