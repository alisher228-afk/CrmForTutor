package org.akusher.crmfortutor.security;

import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentStudentProviderTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private CurrentStudentProvider currentStudentProvider;

    @Test
    @DisplayName("getCurrentStudentProfile - success when student profile is linked")
    void getCurrentStudentProfile_Success() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
        StudentProfile profile = StudentProfile.builder().id(7L).build();
        when(studentProfileRepository.findByUserId(42L)).thenReturn(Optional.of(profile));

        StudentProfile result = currentStudentProvider.getCurrentStudentProfile();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("getCurrentStudentProfile - throws AccessDeniedException (403) when no student profile linked")
    void getCurrentStudentProfile_NotFoundThrowsAccessDenied() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
        when(studentProfileRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentStudentProvider.getCurrentStudentProfile())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No student profile found for current user");
    }

    @Test
    @DisplayName("getCurrentStudentProfileId - returns student ID")
    void getCurrentStudentProfileId_Success() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
        StudentProfile profile = StudentProfile.builder().id(7L).build();
        when(studentProfileRepository.findByUserId(42L)).thenReturn(Optional.of(profile));

        Long id = currentStudentProvider.getCurrentStudentProfileId();

        assertThat(id).isEqualTo(7L);
    }
}
