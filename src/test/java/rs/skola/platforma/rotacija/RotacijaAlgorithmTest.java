package rs.skola.platforma.rotacija;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import rs.skola.platforma.common.exception.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RotacijaAlgorithmTest {

    private final RotacijaAlgorithm algoritam = new RotacijaAlgorithm();

    @ParameterizedTest(name = "C({0},{1}) = {2} nedelje, svako odeljenje {3} puta")
    @CsvSource({
            "3, 2, 3, 2",   // 3 odeljenja, 2 po nedelji -> 3 nedelje, svako 2 puta
            "4, 2, 6, 3",
            "4, 3, 4, 3",
            "5, 2, 10, 4",
            "5, 3, 10, 6",
            "6, 2, 15, 5",
            "6, 3, 20, 10"
    })
    @DisplayName("Ciklus C(N,K): broj nedelja i broj pojavljivanja je teorijski tacan")
    void ciklus_brojNedelja_iPojavljivanjaJeTacan(int n, int k, int ocekivanoNedelja, int ocekivanoPoOdeljenju) {
        List<UUID> odeljenja = nasumicnaOdeljenja(n);

        List<List<UUID>> nedelje = algoritam.generisiCiklus(odeljenja, k);

        assertThat(nedelje).hasSize(ocekivanoNedelja);
        var izvestaj = algoritam.validirajBalans(odeljenja, nedelje);
        assertThat(izvestaj.balansirano()).isTrue();
        assertThat(izvestaj.minCasova()).isEqualTo(ocekivanoPoOdeljenju);
        assertThat(izvestaj.maxCasova()).isEqualTo(ocekivanoPoOdeljenju);
        assertThat(izvestaj.casoviPoOdeljenju()).allSatisfy(
                (id, broj) -> assertThat(broj).isEqualTo(ocekivanoPoOdeljenju));
    }

    @Test
    @DisplayName("Sve generisane kombinacije su jedinstvene")
    void kombinacije_suJedinstvene() {
        List<UUID> odeljenja = nasumicnaOdeljenja(6);

        List<List<UUID>> nedelje = algoritam.generisiCiklus(odeljenja, 3);

        Set<Set<UUID>> kaoSetovi = new HashSet<>();
        for (List<UUID> n : nedelje) kaoSetovi.add(new HashSet<>(n));
        assertThat(kaoSetovi).hasSize(nedelje.size());
    }

    @Test
    @DisplayName("Svaka nedelja ima tacno K odeljenja")
    void svakaNedelja_imaTacnoK_odeljenja() {
        List<UUID> odeljenja = nasumicnaOdeljenja(5);
        int k = 3;

        List<List<UUID>> nedelje = algoritam.generisiCiklus(odeljenja, k);

        assertThat(nedelje).allSatisfy(n -> assertThat(n).hasSize(k));
    }

    @Test
    @DisplayName("Granicni slucaj: K == N daje jednu nedelju sa svim odeljenjima")
    void k_jednako_n_dajeJednuNedelju() {
        List<UUID> odeljenja = nasumicnaOdeljenja(4);

        List<List<UUID>> nedelje = algoritam.generisiCiklus(odeljenja, 4);

        assertThat(nedelje).hasSize(1);
        assertThat(nedelje.get(0)).containsExactlyElementsOf(odeljenja);
    }

    @Test
    @DisplayName("K vece od N baca validacionu gresku")
    void k_veceOd_n_bacaGresku() {
        List<UUID> odeljenja = nasumicnaOdeljenja(2);

        assertThatThrownBy(() -> algoritam.generisiCiklus(odeljenja, 3))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Determinizam: dva poziva sa istim ulazom daju identican izlaz")
    void determinizam_istiUlaz_istiIzlaz() {
        List<UUID> odeljenja = nasumicnaOdeljenja(5);

        List<List<UUID>> a = algoritam.generisiCiklus(odeljenja, 2);
        List<List<UUID>> b = algoritam.generisiCiklus(odeljenja, 2);

        assertThat(a).isEqualTo(b);
    }

    @ParameterizedTest(name = "C({0},{1}) = {2}")
    @CsvSource({
            "3, 2, 3",
            "4, 2, 6",
            "10, 3, 120",
            "12, 6, 924"
    })
    @DisplayName("binomijalniKoeficijent vraca tacan broj")
    void binomijalni_vracaTacnoBroj(int n, int k, long ocekivano) {
        assertThat(RotacijaAlgorithm.binomijalniKoeficijent(n, k)).isEqualTo(ocekivano);
    }

    private static List<UUID> nasumicnaOdeljenja(int n) {
        return IntStream.range(0, n).mapToObj(i -> UUID.randomUUID()).toList();
    }
}
