package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentFirstName", source = "student.firstName")
    @Mapping(target = "studentLastName", source = "student.lastName")
    LessonResponse toResponse(Lesson entity);

    List<LessonResponse> toResponseList(List<Lesson> entities);
}
