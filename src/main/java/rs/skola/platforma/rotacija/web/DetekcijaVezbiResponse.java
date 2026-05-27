package rs.skola.platforma.rotacija.web;

import rs.skola.platforma.raspored.domain.Dan;

import java.util.List;
import java.util.UUID;

/**
 * Rezultat detekcije vezbi za izabrano odeljenje iz aktivne verzije rasporeda.
 * Vraca po profesoru broj termina vezbi (vise profesora u istom terminu)
 * i pratecu dijagnostiku: ukupan broj stavki za odeljenje i raspored svih
 * termina (i onih sa jednim profesorom) da bi korisnik video sirovo stanje
 * rasporeda i odmah uocio gde je problem ako vezbi nema.
 */
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
