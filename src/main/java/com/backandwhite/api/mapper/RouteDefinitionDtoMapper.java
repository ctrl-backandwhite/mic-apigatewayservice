package com.backandwhite.api.mapper;

import com.backandwhite.api.dto.in.RouteDefinitionDtoIn;
import com.backandwhite.api.dto.out.RouteDefinitionDtoOut;
import com.backandwhite.domain.model.GatewayRoute;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RouteDefinitionDtoMapper {

    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GatewayRoute toDomain(RouteDefinitionDtoIn dto);

    RouteDefinitionDtoOut toDtoOut(GatewayRoute route);

    List<RouteDefinitionDtoOut> toDtoOutList(List<GatewayRoute> routes);
}
