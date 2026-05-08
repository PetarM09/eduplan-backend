package rs.skola.platforma.tenant.web;

import org.mapstruct.Mapper;
import rs.skola.platforma.tenant.domain.Skola;

@Mapper(componentModel = "spring")
public interface SkolaMapper {

    SkolaResponse toResponse(Skola s);
}
