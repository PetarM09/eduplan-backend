package rs.skola.platforma.raspored.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.skola.platforma.common.exception.ValidationException;
import rs.skola.platforma.raspored.domain.Dan;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StAX parser SpreadsheetML 2003 (.xml) rasporeda.
 *
 * <p>Format: tabela sa nastavnicima u redovima i kolonama 2..36 koje predstavljaju
 * dane i casove (5 dana x 7 casova). Vrednost u celiji je oznaka odeljenja
 * (npr. "4-1", "3A"). Prvi red obicno sadrzi zaglavlje (preskace se).
 *
 * <p>Sigurnost: parser eksplicitno onemogucava DTD i externe entitete (XXE) i
 * koalescira karaktere kako bi se izbegli "split text" napadi.
 */
@Slf4j
@Component
public class XmlRasporedParser {

    private static final String NS_SS = "urn:schemas-microsoft-com:office:spreadsheet";

    /** Kolone 2..36: 35 slotova, 5 dana x 7 casova (Pon 1..7 | Uto 1..7 | ... | Pet 1..7). */
    private static final Map<Integer, DanCas> MAPA_KOLONA = mapaKolona();

    public List<ParsedRasporedRed> parse(InputStream in) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);

            XMLEventReader reader = factory.createXMLEventReader(in);
            try {
                return parseRows(reader);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException ex) {
            log.warn("Greska pri parsiranju XML rasporeda: {}", ex.getMessage());
            throw new ValidationException("XML_GRESKA", "Nevalidan XML raspored: " + ex.getMessage());
        }
    }

    private List<ParsedRasporedRed> parseRows(XMLEventReader reader) throws XMLStreamException {
        List<ParsedRasporedRed> redovi = new ArrayList<>();
        boolean preskocenoZaglavlje = false;

        while (reader.hasNext()) {
            XMLEvent ev = reader.nextEvent();
            if (!ev.isStartElement()) continue;
            StartElement start = ev.asStartElement();
            if (!isLocal(start, "Row")) continue;

            ParsedRasporedRed red = parseRow(reader);
            if (red == null) continue;

            // Prvi red sa nepopunjenim stavkama tretiramo kao zaglavlje.
            if (!preskocenoZaglavlje && red.stavke().isEmpty()
                    && (red.nastavnikLabel() == null || red.nastavnikLabel().isBlank()
                    || izgledaKaoZaglavlje(red.nastavnikLabel()))) {
                preskocenoZaglavlje = true;
                continue;
            }
            preskocenoZaglavlje = true;

            if (red.nastavnikLabel() == null || red.nastavnikLabel().isBlank()) {
                continue;
            }
            redovi.add(red);
        }
        return redovi;
    }

    /** Citamo Row dok ne dodjemo do njegovog kraja. */
    private ParsedRasporedRed parseRow(XMLEventReader reader) throws XMLStreamException {
        String nastavnik = null;
        List<ParsedRasporedRed.ParsedStavka> stavke = new ArrayList<>();
        int trenutnaKolona = 0;

        while (reader.hasNext()) {
            XMLEvent ev = reader.nextEvent();
            if (ev.isEndElement() && isLocal(ev.asEndElement(), "Row")) {
                return new ParsedRasporedRed(normalizuj(nastavnik), stavke);
            }
            if (!ev.isStartElement()) continue;
            StartElement cellStart = ev.asStartElement();
            if (!isLocal(cellStart, "Cell")) continue;

            Integer eksplicitanIndex = atrIntNS(cellStart, "Index");
            trenutnaKolona = eksplicitanIndex != null ? eksplicitanIndex : trenutnaKolona + 1;

            String vrednost = procitajData(reader);
            if (vrednost == null || vrednost.isBlank()) continue;

            if (trenutnaKolona == 1) {
                nastavnik = vrednost;
                continue;
            }
            DanCas dc = MAPA_KOLONA.get(trenutnaKolona);
            if (dc == null) continue;
            stavke.add(new ParsedRasporedRed.ParsedStavka(dc.dan(), dc.cas(), vrednost.trim()));
        }
        return null;
    }

    /** U Cell-u procita prvi <Data> sadrzaj (ako postoji) i pozicionira se na </Cell>. */
    private String procitajData(XMLEventReader reader) throws XMLStreamException {
        String vrednost = null;
        while (reader.hasNext()) {
            XMLEvent ev = reader.nextEvent();
            if (ev.isEndElement() && isLocal(ev.asEndElement(), "Cell")) {
                return vrednost;
            }
            if (ev.isStartElement() && isLocal(ev.asStartElement(), "Data")) {
                XMLEvent next = reader.nextEvent();
                if (next.isCharacters()) {
                    Characters chars = next.asCharacters();
                    vrednost = chars.getData();
                }
                // konzumiraj </Data>
                while (reader.hasNext()) {
                    XMLEvent eo = reader.nextEvent();
                    if (eo.isEndElement() && isLocal(eo.asEndElement(), "Data")) break;
                }
            }
        }
        return vrednost;
    }

    private boolean isLocal(StartElement start, String localName) {
        return localName.equals(start.getName().getLocalPart());
    }

    private boolean isLocal(EndElement end, String localName) {
        return localName.equals(end.getName().getLocalPart());
    }

    private Integer atrIntNS(StartElement start, String localName) {
        Attribute attr = start.getAttributeByName(new javax.xml.namespace.QName(NS_SS, localName));
        if (attr == null) attr = start.getAttributeByName(new javax.xml.namespace.QName(localName));
        if (attr == null) return null;
        try {
            return Integer.parseInt(attr.getValue());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizuj(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    private static boolean izgledaKaoZaglavlje(String s) {
        if (s == null) return false;
        String n = s.trim().toLowerCase();
        return n.equals("nastavnik") || n.equals("predmet") || n.equals("ime") || n.equals("prezime")
                || n.contains("ponedeljak") || n.contains("utorak");
    }

    private static Map<Integer, DanCas> mapaKolona() {
        Map<Integer, DanCas> m = new HashMap<>();
        Dan[] dani = {Dan.PONEDELJAK, Dan.UTORAK, Dan.SREDA, Dan.CETVRTAK, Dan.PETAK};
        int kolona = 2;
        for (Dan d : dani) {
            for (short cas = 1; cas <= 7; cas++) {
                m.put(kolona++, new DanCas(d, cas));
            }
        }
        return Map.copyOf(m);
    }

    private record DanCas(Dan dan, short cas) {}
}
