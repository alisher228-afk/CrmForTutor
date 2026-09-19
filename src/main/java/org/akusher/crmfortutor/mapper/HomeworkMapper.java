package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomeworkMapper {

    @Mapping(target = "lessonId", source = "lesson.id")
    HomeworkResponse toResponse(Homework entity);

    List<HomeworkResponse> toResponseList(List<Homework> entities);
}
