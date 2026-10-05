package org.akusher.crmfortutor.mapper;

import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperTest {

    private final StudentMapper studentMapper = Mappers.getMapper(StudentMapper.class);

    @Test
    @DisplayName("toSelfResponse() - Tutor with first and last name resolves full tutorName")
    void toSelfResponse_TutorWithFullName() {
        User tutor = User.builder()
                .id(1L)
                .email("tutor@example.com")
                .firstName("Иван")
                .lastName("Иванов")
                .phone("+77011112233")
                .build();

        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .firstName("Петр")
                .lastName("Сидоров")
                .tutor(tutor)
                .build();

        StudentSelfResponse response = studentMapper.toSelfResponse(profile);

        assertThat(response.getTutorName()).isEqualTo("Иван Иванов");
        assertThat(response.getTutorFirstName()).isEqualTo("Иван");
        assertThat(response.getTutorLastName()).isEqualTo("Иванов");
        assertThat(response.getTutorPhone()).isEqualTo("+77011112233");
        assertThat(response.getTutorEmail()).isEqualTo("tutor@example.com");
    }

    @Test
    @DisplayName("toSelfResponse() - Tutor with only first name resolves first name")
    void toSelfResponse_TutorWithOnlyFirstName() {
        User tutor = User.builder()
                .id(1L)
                .email("tutor@example.com")
                .firstName("Алексей")
                .build();

        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .firstName("Петр")
                .tutor(tutor)
                .build();

        StudentSelfResponse response = studentMapper.toSelfResponse(profile);

        assertThat(response.getTutorName()).isEqualTo("Алексей");
        assertThat(response.getTutorEmail()).isEqualTo("tutor@example.com");
    }

    @Test
    @DisplayName("toSelfResponse() - Tutor without name falls back to email")
    void toSelfResponse_TutorWithoutName_FallsBackToEmail() {
        User tutor = User.builder()
                .id(1L)
                .email("tutor@example.com")
                .build();

        StudentProfile profile = StudentProfile.builder()
                .id(10L)
                .firstName("Петр")
                .tutor(tutor)
                .build();

        StudentSelfResponse response = studentMapper.toSelfResponse(profile);

        assertThat(response.getTutorName()).isEqualTo("tutor@example.com");
        assertThat(response.getTutorEmail()).isEqualTo("tutor@example.com");
    }
}
