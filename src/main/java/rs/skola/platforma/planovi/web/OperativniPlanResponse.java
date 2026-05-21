package rs.skola.platforma.planovi.web;

import rs.skola.platforma.planovi.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OperativniPlanResponse(
        UUID id,
        UUID nastavnikId,
        String nastavnikIme,
        UUID predmetId,
        String predmetNaziv,
        UUID odeljenjeId,
        String odeljenjeLabel,
        Short mesec,
        String skolskaGodina,
        Short nedeljniFond,
        String samoprocenaIshoda,
        String napomene,
        PlanStatus status,
        OffsetDateTime podnetAt,
        boolean imaWord,
        boolean imaPdf,
        List<StavkaResponse> stavke,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record StavkaResponse(
            UUID id,
            Short redniBrojCasa,
            UUID temaId,
            String nazivTeme,
            UUID nastavnaJedinicaId,
            String nazivJedinice,
            UUID tipCasaId,
            String tipCasa,
            UUID metodaRadaId,
            String metodaRada,
            String evaluacija,
            List<IshodKratko> ishodi,
            List<MedjupredmetnoKratko> medjupredmetno
    ) {}

    public record IshodKratko(UUID id, String opis) {}

    public record MedjupredmetnoKratko(UUID id, UUID predmetId, String predmetNaziv, String opisKompetencije) {}
}
