package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.akusher.crmfortutor.dto.request.LessonUpdateRequest;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentFirstName", source = "student.firstName")
    @Mapping(target = "studentLastName", source = "student.lastName")
    LessonResponse toResponse(Lesson entity);

    List<LessonResponse> toResponseList(List<Lesson> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reminderSentAt", ignore = true)
    void updateEntityFromDto(LessonUpdateRequest request, @MappingTarget Lesson entity);
}
