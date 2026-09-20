package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.response.StudentInviteResponse;
import org.akusher.crmfortutor.dto.response.TelegramLinkCodeResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.StudentMapper;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private TelegramProperties telegramProperties;

    @InjectMocks
    private StudentService studentService;

    private Long tutorId;
    private StudentProfile student;

    @BeforeEach
    void setUp() {
        tutorId = 1L;
        User tutor = User.builder().id(tutorId).email("tutor@example.com").build();

        student = StudentProfile.builder()
                .id(10L)
                .tutor(tutor)
                .firstName("Dmitry")
                .lastName("Sidorov")
                .build();
    }

    @Test
    @DisplayName("createInviteToken - success")
    void createInviteToken_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(student));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentInviteResponse response = studentService.createInviteToken(10L);

        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(10L);
        assertThat(response.getInviteToken()).isNotBlank();
        assertThat(response.getExpiresAt()).isAfter(Instant.now());

        assertThat(student.getInviteToken()).isEqualTo(response.getInviteToken());
        assertThat(student.getInviteTokenExpiresAt()).isEqualTo(response.getExpiresAt());
        verify(studentProfileRepository).save(student);
    }

    @Test
    @DisplayName("createInviteToken - student not found or belongs to another tutor")
    void createInviteToken_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.createInviteToken(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found with id: 999");

        verify(studentProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInviteToken - student already linked to a user account")
    void createInviteToken_AlreadyLinked() {
        User existingUser = User.builder().id(55L).email("student@example.com").build();
        student.setUser(existingUser);

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studentService.createInviteToken(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Student is already linked to a user account");

        verify(studentProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateTelegramLinkCode - success")
    void generateTelegramLinkCode_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(student));
        when(studentProfileRepository.findByTelegramLinkCode(any())).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(telegramProperties.getBotUsername()).thenReturn("TestTutorBot");

        TelegramLinkCodeResponse response = studentService.generateTelegramLinkCode(10L);

        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(10L);
        assertThat(response.getLinkCode()).isNotNull().hasSize(6).containsOnlyDigits();
        assertThat(response.getCode()).isEqualTo(response.getLinkCode());
        assertThat(response.getExpiresAt()).isAfter(Instant.now());
        assertThat(response.getBotUsername()).isEqualTo("TestTutorBot");

        assertThat(student.getTelegramLinkCode()).isEqualTo(response.getLinkCode());
        assertThat(student.getTelegramLinkCodeExpiresAt()).isEqualTo(response.getExpiresAt());
        verify(studentProfileRepository).save(student);
    }

    @Test
    @DisplayName("generateTelegramLinkCode - student not found")
    void generateTelegramLinkCode_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.generateTelegramLinkCode(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found with id: 999");

        verify(studentProfileRepository, never()).save(any());
    }
}
