package rs.skola.platforma.katalog.web;

import java.util.UUID;

public record TemaResponse(
        UUID id,
        UUID predmetId,
        Short redniBroj,
        String naziv,
        Short casObrada,
        Short casUtvrd,
        Short casOstalo
) {}
