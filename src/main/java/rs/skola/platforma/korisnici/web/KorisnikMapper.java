package rs.skola.platforma.korisnici.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import rs.skola.platforma.korisnici.domain.Korisnik;

@Mapper(componentModel = "spring")
public interface KorisnikMapper {

    @Mapping(target = "skolaId", expression = "java(k.getSkola() == null ? null : k.getSkola().getId())")
    KorisnikResponse toResponse(Korisnik k);
}
