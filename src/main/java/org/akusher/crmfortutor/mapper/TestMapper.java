package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.request.TestCreateRequest;
import org.akusher.crmfortutor.dto.request.TestUpdateRequest;
import org.akusher.crmfortutor.dto.response.TestResponse;
import org.akusher.crmfortutor.entity.TestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TestMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", expression = "java(entity.getStudent() != null ? (entity.getStudent().getFirstName() + (entity.getStudent().getLastName() != null && !entity.getStudent().getLastName().isBlank() ? \" \" + entity.getStudent().getLastName() : \"\")) : null)")
    @Mapping(target = "submissionsCount", ignore = true)
    @Mapping(target = "averageScore", ignore = true)
    @Mapping(target = "mySubmission", ignore = true)
    TestResponse toResponse(TestEntity entity);

    List<TestResponse> toResponseList(List<TestEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TestEntity toEntity(TestCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(TestUpdateRequest request, @MappingTarget TestEntity entity);
}
