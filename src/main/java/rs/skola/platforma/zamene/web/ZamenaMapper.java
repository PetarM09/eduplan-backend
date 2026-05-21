package rs.skola.platforma.zamene.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import rs.skola.platforma.zamene.domain.Zamena;

@Mapper(componentModel = "spring")
public interface ZamenaMapper {

    @Mapping(target = "odsutniId", expression = "java(z.getOdsutni() == null ? null : z.getOdsutni().getId())")
    @Mapping(target = "odsutniIme", expression = "java(z.getOdsutni() == null ? null : z.getOdsutni().punoIme())")
    @Mapping(target = "zamenikId", expression = "java(z.getZamenik() == null ? null : z.getZamenik().getId())")
    @Mapping(target = "zamenikIme", expression = "java(z.getZamenik() == null ? null : z.getZamenik().punoIme())")
    @Mapping(target = "odeljenjeId", expression = "java(z.getOdeljenje() == null ? null : z.getOdeljenje().getId())")
    @Mapping(target = "odeljenjeLabel", expression = "java(z.getOdeljenje() == null ? null : z.getOdeljenje().label())")
    @Mapping(target = "odobrioId", expression = "java(z.getOdobrio() == null ? null : z.getOdobrio().getId())")
    @Mapping(target = "odobrioIme", expression = "java(z.getOdobrio() == null ? null : z.getOdobrio().punoIme())")
    ZamenaResponse toResponse(Zamena z);
}
