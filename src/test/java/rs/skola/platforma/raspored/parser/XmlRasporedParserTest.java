package rs.skola.platforma.raspored.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.skola.platforma.raspored.domain.Dan;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class XmlRasporedParserTest {

    /** Pojedinacno odeljenje ("4-1", "3A") ili spojeno za grupni cas ("1-1/1-5"). */
    private static final Pattern ODELJENJE_FORMAT =
            Pattern.compile("^\\d{1,2}[-/]?[A-Za-z0-9]{1,3}(/\\d{1,2}[-/]?[A-Za-z0-9]{1,3})*$");

    private final XmlRasporedParser parser = new XmlRasporedParser();

    @Test
    @DisplayName("Parsira raspored15.09.xml: dohvata nastavnike sa stavkama, preskace zaglavlje")
    void parsira_realniRaspored_pravilno() throws Exception {
        List<ParsedRasporedRed> redovi = parsirajResurs("raspored15.09.xml");

        // Bar 20 nastavnika ocekujemo u realnom rasporedu skole
        assertThat(redovi).hasSizeGreaterThan(20);

        // Svaki red mora da ima i ime nastavnika i bar jednu stavku
        assertThat(redovi).allSatisfy(red -> {
            assertThat(red.nastavnikLabel()).isNotBlank();
            assertThat(red.stavke()).isNotEmpty();
        });

        // Imena su normalizovana — nema visestrukih razmaka, nema linebreak-ova
        assertThat(redovi).allSatisfy(red ->
                assertThat(red.nastavnikLabel()).doesNotContain("\n", "\t", "  "));

        // Svaka stavka ima validan format odeljenja
        assertThat(redovi).allSatisfy(red ->
                assertThat(red.stavke()).allSatisfy(s -> {
                    assertThat(s.odeljenjeLabel()).matches(ODELJENJE_FORMAT);
                    assertThat(s.cas()).isBetween((short) 1, (short) 7);
                    assertThat(s.dan()).isIn(Dan.PONEDELJAK, Dan.UTORAK, Dan.SREDA, Dan.CETVRTAK, Dan.PETAK);
                }));
    }

    @Test
    @DisplayName("Prvi nastavnik u rasporedu je Kanlic Jelena sa stavkama na ocekivanim mestima")
    void prviNastavnik_imaOcekivaneStavke() throws Exception {
        List<ParsedRasporedRed> redovi = parsirajResurs("raspored15.09.xml");

        Optional<ParsedRasporedRed> kanlic = redovi.stream()
                .filter(r -> r.nastavnikLabel().contains("Kanlić Jelena"))
                .findFirst();

        assertThat(kanlic).isPresent();
        ParsedRasporedRed red = kanlic.get();

        // Iz XML-a: kolona 4 -> Pon cas 3 = "4-1"
        assertThat(red.stavke()).anyMatch(s ->
                s.dan() == Dan.PONEDELJAK && s.cas() == 3 && "4-1".equals(s.odeljenjeLabel()));
        // kolona 15 -> Uto cas 7 = "3-1"
        assertThat(red.stavke()).anyMatch(s ->
                s.dan() == Dan.UTORAK && s.cas() == 7 && "3-1".equals(s.odeljenjeLabel()));
        // kolona 16 -> Sre cas 1 = "4-1"
        assertThat(red.stavke()).anyMatch(s ->
                s.dan() == Dan.SREDA && s.cas() == 1 && "4-1".equals(s.odeljenjeLabel()));
        // kolona 24 -> Cet cas 2 = "1-4"
        assertThat(red.stavke()).anyMatch(s ->
                s.dan() == Dan.CETVRTAK && s.cas() == 2 && "1-4".equals(s.odeljenjeLabel()));
    }

    @Test
    @DisplayName("Imena nastavnika sa srpskim karakterima (c, c, s) se ocuvavaju kroz parser")
    void imenaSaCirilicnimSlovima_ocuvana() throws Exception {
        List<ParsedRasporedRed> redovi = parsirajResurs("raspored15.09.xml");

        // U fajlu postoji bar jedno ime sa srpskim slovima
        assertThat(redovi).anyMatch(r -> r.nastavnikLabel().matches(".*[čćžšđČĆŽŠĐ].*"));
    }

    private List<ParsedRasporedRed> parsirajResurs(String naziv) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(naziv)) {
            assertThat(in).as("Test resurs %s mora postojati", naziv).isNotNull();
            return parser.parse(in);
        }
    }
}
