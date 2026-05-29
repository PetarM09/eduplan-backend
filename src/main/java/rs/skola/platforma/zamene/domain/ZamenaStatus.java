package rs.skola.platforma.zamene.domain;

import java.util.Set;

public enum ZamenaStatus {

    PREDLOZENA(Set.of()),  // popunjeno u static-init bloku ispod
    ODOBRENA(Set.of()),
    ODBIJENA(Set.of()),
    OTKAZANA(Set.of());

    private Set<ZamenaStatus> dozvoljeneTranzicije;

    ZamenaStatus(Set<ZamenaStatus> dozvoljeneTranzicije) {
        this.dozvoljeneTranzicije = dozvoljeneTranzicije;
    }

    static {
        PREDLOZENA.dozvoljeneTranzicije = Set.of(ODOBRENA, ODBIJENA, OTKAZANA);
        ODOBRENA.dozvoljeneTranzicije = Set.of(OTKAZANA);
        ODBIJENA.dozvoljeneTranzicije = Set.of();
        OTKAZANA.dozvoljeneTranzicije = Set.of();
    }

    public boolean mozePreci(ZamenaStatus noviStatus) {
        return dozvoljeneTranzicije.contains(noviStatus);
    }

    public boolean jeTerminalan() {
        return dozvoljeneTranzicije.isEmpty();
    }
}
