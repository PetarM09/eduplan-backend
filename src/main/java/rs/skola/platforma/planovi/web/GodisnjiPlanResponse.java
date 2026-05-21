package rs.skola.platforma.planovi.web;

import rs.skola.platforma.planovi.domain.PlanStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GodisnjiPlanResponse(
        UUID id,
        UUID nastavnikId,
        String nastavnikIme,
        UUID predmetId,
        String predmetNaziv,
        Short razred,
        String skolskaGodina,
        List<UUID> odeljenjaIds,
        String ciljeviZadaci,
        String udzebenik,
        String autori,
        String literatura,
        Short godisnjiFond,
        Short nedeljniFond,
        String dopunskiRad,
        String dodatniRad,
        String napomene,
        PlanStatus status,
        OffsetDateTime podnetAt,
        boolean imaWord,
        boolean imaPdf,
        List<TemaResponse> teme,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record TemaResponse(
            UUID id,
            UUID temaId,
            String nazivTeme,
            Short redniBroj,
            Map<String, Boolean> meseci,
            Short casObrada,
            Short casUtvrd,
            Short casOstalo,
            Short ukupnoCasova,
            List<IshodKratko> ishodi
    ) {}

    public record IshodKratko(UUID id, String opis) {}
}
