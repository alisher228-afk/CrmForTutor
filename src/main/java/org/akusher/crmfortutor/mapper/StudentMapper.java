package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.request.StudentCreateRequest;
import org.akusher.crmfortutor.dto.request.StudentUpdateRequest;
import org.akusher.crmfortutor.dto.response.StudentResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    StudentResponse toResponse(StudentProfile entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    StudentProfile toEntity(StudentCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "lessonBalance", ignore = true)
    void updateEntityFromDto(StudentUpdateRequest request, @MappingTarget StudentProfile entity);
}
