package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(target = "homeworkId", source = "homework.id")
    @Mapping(target = "uploadedByUserId", source = "uploadedByUser.id")
    AttachmentResponse toResponse(Attachment entity);

    List<AttachmentResponse> toResponseList(List<Attachment> entities);
}
