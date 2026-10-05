package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.TestSubmissionResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.TestSubmissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TestSubmissionMapper {

    @Mapping(target = "testId", source = "test.id")
    @Mapping(target = "testTitle", source = "test.title")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student", qualifiedByName = "formatStudentName")
    TestSubmissionResponse toResponse(TestSubmissionEntity entity);

    List<TestSubmissionResponse> toResponseList(List<TestSubmissionEntity> entities);

    @Named("formatStudentName")
    default String formatStudentName(StudentProfile student) {
        if (student == null) {
            return "Ученик";
        }
        String first = student.getFirstName();
        String last = student.getLastName();
        if (first != null && !first.isBlank()) {
            return (last != null && !last.isBlank()) ? (first.trim() + " " + last.trim()) : first.trim();
        }
        if (student.getUser() != null && student.getUser().getEmail() != null) {
            return student.getUser().getEmail();
        }
        return "Ученик #" + student.getId();
    }
}
