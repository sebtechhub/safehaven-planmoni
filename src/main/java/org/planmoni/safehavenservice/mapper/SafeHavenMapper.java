package org.planmoni.safehavenservice.mapper;

import org.planmoni.safehavenservice.dto.request.SafeHavenCreateRequest;
import org.planmoni.safehavenservice.dto.request.SafeHavenUpdateRequest;
import org.planmoni.safehavenservice.dto.response.SafeHavenResponse;
import org.planmoni.safehavenservice.entity.SafeHaven;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SafeHavenMapper {

    SafeHavenResponse toResponse(SafeHaven safeHaven);

    SafeHaven toEntity(SafeHavenCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(SafeHavenUpdateRequest request, @MappingTarget SafeHaven safeHaven);
}
