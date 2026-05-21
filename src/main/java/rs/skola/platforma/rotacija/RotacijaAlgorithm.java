package rs.skola.platforma.rotacija;

import org.springframework.stereotype.Component;
import rs.skola.platforma.common.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generisanje i validacija rotacionog ciklusa za grupne casove (npr. vezbe).
 *
 * <p><b>Problem:</b> Nastavnik ima {@code N} odeljenja koja moraju da prodju kroz vezbe.
 * Nedeljno se odrzava {@code K} casova vezbi (K &lt; N), pa se odeljenja smenjuju
 * po nedeljama tako da svako odeljenje ucestvuje jednak broj puta.
 *
 * <p><b>Resenje:</b> generisemo svih {@code C(N,K)} jedinstvenih kombinacija K
 * odeljenja od N. Tih C(N,K) kombinacija cini jedan pun ciklus.
 *
 * <p><b>Matematicki dokaz balansa:</b> svako odeljenje se pojavljuje u tacno
 * {@code C(N-1, K-1)} kombinacija (broj nacina da se izabere preostalih K-1 odeljenja
 * od preostalih N-1). Posto je ovaj broj isti za svako odeljenje, ciklus je
 * uvek savrseno balansiran.
 *
 * <p>Primeri:
 * <pre>
 *   C(3,2) = 3 nedelje, svako odeljenje 2 puta  (npr. 3A/3B/3C → AB, AC, BC)
 *   C(4,2) = 6 nedelja, svako 3 puta
 *   C(5,3) = 10 nedelja, svako 6 puta
 *   C(6,2) = 15 nedelja, svako 5 puta
 * </pre>
 */
@Component
public class RotacijaAlgorithm {

    /**
     * Generise sve C(N,K) kombinacije. Redosled je leksikografski po indeksima
     * ulaznog niza — deterministicki, isti ulaz daje isti izlaz.
     */
    public List<List<UUID>> generisiCiklus(List<UUID> odeljenja, int k) {
        if (odeljenja == null || odeljenja.isEmpty()) {
            throw new ValidationException("Lista odeljenja je prazna");
        }
        int n = odeljenja.size();
        if (k <= 0) {
            throw new ValidationException("Velicina grupe (k) mora biti pozitivna");
        }
        if (k > n) {
            throw new ValidationException("Velicina grupe (%d) ne moze biti veca od broja odeljenja (%d)".formatted(k, n));
        }
        if (k == n) {
            // Trivijalan slucaj: jedan red sa svim odeljenjima.
            return List.of(List.copyOf(odeljenja));
        }

        List<List<UUID>> rezultat = new ArrayList<>();
        int[] indeksi = new int[k];
        for (int i = 0; i < k; i++) indeksi[i] = i;

        while (true) {
            List<UUID> kombinacija = new ArrayList<>(k);
            for (int idx : indeksi) kombinacija.add(odeljenja.get(idx));
            rezultat.add(kombinacija);

            int i = k - 1;
            while (i >= 0 && indeksi[i] == n - k + i) i--;
            if (i < 0) break;
            indeksi[i]++;
            for (int j = i + 1; j < k; j++) indeksi[j] = indeksi[j - 1] + 1;
        }
        return rezultat;
    }

    /**
     * Validira da svako odeljenje ucestvuje u istom broju kombinacija — tj. da je
     * ciklus savrseno balansiran. Vraca izvestaj koji se moze prikazati korisniku.
     */
    public IzvestajBalansa validirajBalans(List<UUID> svaOdeljenja, List<List<UUID>> nedelje) {
        Map<UUID, Integer> broj = new HashMap<>();
        for (UUID od : svaOdeljenja) broj.put(od, 0);
        for (List<UUID> nedelja : nedelje) {
            for (UUID od : nedelja) {
                broj.merge(od, 1, Integer::sum);
            }
        }
        int min = broj.values().stream().min(Integer::compareTo).orElse(0);
        int max = broj.values().stream().max(Integer::compareTo).orElse(0);
        boolean balansirano = min == max;
        return new IzvestajBalansa(balansirano, min, max, Collections.unmodifiableMap(broj));
    }

    /** {@code C(n, k)} = {@code n! / (k! * (n-k)!)} bez overflow-a za realistican N. */
    public static long binomijalniKoeficijent(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        if (k > n - k) k = n - k;
        long r = 1;
        for (int i = 1; i <= k; i++) {
            r = r * (n - i + 1) / i;
        }
        return r;
    }

    public record IzvestajBalansa(
            boolean balansirano,
            int minCasova,
            int maxCasova,
            Map<UUID, Integer> casoviPoOdeljenju
    ) {}
}
