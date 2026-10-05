package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomeworkMapper {

    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "studentId", source = "lesson.student.id")
    @Mapping(target = "studentName", expression = "java(entity.getLesson() != null && entity.getLesson().getStudent() != null ? (entity.getLesson().getStudent().getFirstName() + (entity.getLesson().getStudent().getLastName() != null && !entity.getLesson().getStudent().getLastName().isBlank() ? \" \" + entity.getLesson().getStudent().getLastName() : \"\")) : null)")
    @Mapping(target = "groupName", expression = "java(entity.getLesson() != null ? (entity.getLesson().getGroupName() != null && !entity.getLesson().getGroupName().isBlank() ? entity.getLesson().getGroupName() : (entity.getLesson().getStudent() != null ? entity.getLesson().getStudent().getGroupName() : null)) : null)")
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "attachmentsCount", ignore = true)
    HomeworkResponse toResponse(Homework entity);

    List<HomeworkResponse> toResponseList(List<Homework> entities);
}
