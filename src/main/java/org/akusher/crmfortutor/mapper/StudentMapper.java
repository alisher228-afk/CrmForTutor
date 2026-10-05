package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.request.StudentCreateRequest;
import org.akusher.crmfortutor.dto.request.StudentUpdateRequest;
import org.akusher.crmfortutor.dto.response.StudentResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    StudentResponse toResponse(StudentProfile entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorEmail", source = "tutor.email")
    @Mapping(target = "tutorFirstName", source = "tutor.firstName")
    @Mapping(target = "tutorLastName", source = "tutor.lastName")
    @Mapping(target = "tutorPhone", source = "tutor.phone")
    @Mapping(target = "tutorName", expression = "java(resolveTutorName(entity.getTutor()))")
    StudentSelfResponse toSelfResponse(StudentProfile entity);

    default String resolveTutorName(User tutor) {
        if (tutor == null) {
            return null;
        }
        String first = tutor.getFirstName();
        String last = tutor.getLastName();
        boolean hasFirst = first != null && !first.trim().isBlank();
        boolean hasLast = last != null && !last.trim().isBlank();

        if (hasFirst && hasLast) {
            return first.trim() + " " + last.trim();
        } else if (hasFirst) {
            return first.trim();
        } else if (hasLast) {
            return last.trim();
        }
        return tutor.getEmail();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "inviteToken", ignore = true)
    @Mapping(target = "inviteTokenExpiresAt", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "telegramLinkCode", ignore = true)
    @Mapping(target = "telegramLinkCodeExpiresAt", ignore = true)
    StudentProfile toEntity(StudentCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tutor", ignore = true)
    @Mapping(target = "lessonBalance", ignore = true)
    @Mapping(target = "inviteToken", ignore = true)
    @Mapping(target = "inviteTokenExpiresAt", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "telegramLinkCode", ignore = true)
    @Mapping(target = "telegramLinkCodeExpiresAt", ignore = true)
    void updateEntityFromDto(StudentUpdateRequest request, @MappingTarget StudentProfile entity);
}
