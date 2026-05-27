package rs.skola.platforma.rotacija.web;

import rs.skola.platforma.raspored.domain.Dan;

import java.util.List;
import java.util.UUID;

/**
 * Rezultat detekcije vezbi za izabrano odeljenje iz aktivne verzije rasporeda.
 *
 * <p>Profesori se identifikuju po imenu iz XML-a (label), bez obzira na to da li
 * postoji odgovarajuci korisnicki nalog u sistemu. {@code profesorId} je null
 * ako jos nije mapiran. To omogucava da koordinator vidi sve potencijalne
 * profesore vezbi i odluci koji nedostaju u sistemu pre nego sto pravi rotaciju.
 *
 * <p>{@code sviTermini} sadrzi sve termine za odeljenje (sa 1+ profesora) radi
 * dijagnostike kad rotacija ne vidi vezbe; {@code termini} samo one sa 2+
 * profesora (kandidati za rotaciju).
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
