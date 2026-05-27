package rs.skola.platforma.rotacija.web;

import rs.skola.platforma.raspored.domain.Dan;

import java.util.List;
import java.util.UUID;

/**
 * Rezultat detekcije vezbi za izabrano odeljenje iz aktivne verzije rasporeda.
 * Vraca po profesoru broj termina vezbi (vise profesora u istom terminu),
 * plus listu samih termina za kontekst.
 */
public record DetekcijaVezbiResponse(
        UUID odeljenjeId,
        String odeljenjeLabel,
        List<ProfesorVezbi> profesori,
        List<TerminVezbi> termini
) {

    public record ProfesorVezbi(
            UUID profesorId,
            String profesorIme,
            int brojCasovaVezbi
    ) {}

    public record TerminVezbi(
            Dan dan,
            Short cas,
            List<UUID> profesoriIds,
            List<String> profesoriImena
    ) {}
}
