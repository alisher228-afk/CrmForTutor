package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.ChangePasswordRequest;
import org.akusher.crmfortutor.dto.request.UserProfileUpdateRequest;
import org.akusher.crmfortutor.dto.response.UserProfileResponse;
import org.akusher.crmfortutor.entity.Role;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private User tutorUser;
    private User studentUser;
    private StudentProfile studentProfile;

    @BeforeEach
    void setUp() {
        tutorUser = User.builder()
                .id(1L)
                .email("tutor@example.com")
                .password("encoded_tutor_pass")
                .role(Role.ROLE_TUTOR)
                .firstName("Иван")
                .lastName("Иванов")
                .phone("+77011111111")
                .specialization("Информатика")
                .createdAt(Instant.now())
                .build();

        studentUser = User.builder()
                .id(2L)
                .email("student@example.com")
                .password("encoded_student_pass")
                .role(Role.ROLE_STUDENT)
                .createdAt(Instant.now())
                .build();

        studentProfile = StudentProfile.builder()
                .id(10L)
                .user(studentUser)
                .firstName("Петр")
                .lastName("Петров")
                .phone("+77022222222")
                .build();
    }

    @Test
    @DisplayName("getProfile() - Tutor profile returns tutor details")
    void getProfile_Tutor() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));

        UserProfileResponse response = profileService.getProfile();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("tutor@example.com");
        assertThat(response.getFirstName()).isEqualTo("Иван");
        assertThat(response.getLastName()).isEqualTo("Иванов");
        assertThat(response.getSpecialization()).isEqualTo("Информатика");
    }

    @Test
    @DisplayName("getProfile() - Student profile falls back to StudentProfile entity")
    void getProfile_StudentFallback() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(2L)).thenReturn(Optional.of(studentProfile));

        UserProfileResponse response = profileService.getProfile();

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getFirstName()).isEqualTo("Петр");
        assertThat(response.getLastName()).isEqualTo("Петров");
        assertThat(response.getPhone()).isEqualTo("+77022222222");
    }

    @Test
    @DisplayName("updateProfile() - Tutor updates fields")
    void updateProfile_Tutor() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .firstName("Алексей")
                .lastName("Смирнов")
                .phone("+77033333333")
                .specialization("Математика и физика")
                .build();

        UserProfileResponse response = profileService.updateProfile(request);

        assertThat(response.getFirstName()).isEqualTo("Алексей");
        assertThat(response.getLastName()).isEqualTo("Смирнов");
        assertThat(response.getPhone()).isEqualTo("+77033333333");
        assertThat(response.getSpecialization()).isEqualTo("Математика и физика");
    }

    @Test
    @DisplayName("updateProfile() - Student updates sync to StudentProfile")
    void updateProfile_StudentSync() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(2L)).thenReturn(Optional.of(studentProfile));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .firstName("НовоеИмя")
                .lastName("НоваяФамилия")
                .phone("+77099999999")
                .build();

        UserProfileResponse response = profileService.updateProfile(request);

        assertThat(response.getFirstName()).isEqualTo("НовоеИмя");
        assertThat(studentProfile.getFirstName()).isEqualTo("НовоеИмя");
        verify(studentProfileRepository).save(studentProfile);
    }

    @Test
    @DisplayName("changePassword() - Success")
    void changePassword_Success() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("oldPass123", "encoded_tutor_pass")).thenReturn(true);
        when(passwordEncoder.encode("newPass456")).thenReturn("encoded_new_pass");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPass123")
                .newPassword("newPass456")
                .build();

        profileService.changePassword(request);

        assertThat(tutorUser.getPassword()).isEqualTo("encoded_new_pass");
        verify(userRepository).save(tutorUser);
    }

    @Test
    @DisplayName("changePassword() - Wrong current password throws BadRequestException")
    void changePassword_WrongCurrentPassword() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("wrongPass", "encoded_tutor_pass")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPass")
                .newPassword("newPass456")
                .build();

        assertThatThrownBy(() -> profileService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Текущий пароль указан неверно");
    }

    @Test
    @DisplayName("changePassword() - Same new password throws BadRequestException")
    void changePassword_SamePassword() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("samePassword123", "encoded_tutor_pass")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("samePassword123")
                .newPassword("samePassword123")
                .build();

        assertThatThrownBy(() -> profileService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Новый пароль должен отличаться от текущего");
    }
}
