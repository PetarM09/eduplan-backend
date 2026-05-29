package rs.skola.platforma.rotacija.web;

import rs.skola.platforma.raspored.domain.Dan;

import java.util.List;
import java.util.UUID;

public record DetekcijaVezbiResponse(
        UUID odeljenjeId,
        String odeljenjeLabel,
        int ukupnoStavki,
        int ukupnoTerminaUkupno,
        List<ProfesorVezbi> profesori,
        List<TerminVezbi> termini,
        List<TerminVezbi> sviTermini
) {

    public record ProfesorVezbi(
            UUID profesorId,        // null ako profesor jos nije u sistemu
            String profesorIme,     // ime iz rasporeda (XML-a)
            boolean uSistemu,
            int brojCasovaVezbi
    ) {}

    public record TerminVezbi(
            Dan dan,
            Short cas,
            List<UUID> profesoriIds,    // null za one koji nisu mapirani u sistem
            List<String> profesoriImena
    ) {}
}
