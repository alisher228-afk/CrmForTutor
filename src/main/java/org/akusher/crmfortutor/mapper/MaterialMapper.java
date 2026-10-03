package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.request.MaterialUpdateRequest;
import org.akusher.crmfortutor.dto.response.MaterialResponse;
import org.akusher.crmfortutor.entity.TeachingMaterial;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MaterialMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    MaterialResponse toResponse(TeachingMaterial entity);

    List<MaterialResponse> toResponseList(List<TeachingMaterial> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "originalFileName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "sizeBytes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(MaterialUpdateRequest request, @MappingTarget TeachingMaterial entity);
}
