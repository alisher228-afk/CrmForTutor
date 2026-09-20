package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.LoginRequest;
import org.akusher.crmfortutor.dto.request.StudentRegisterRequest;
import org.akusher.crmfortutor.dto.response.AuthResponse;
import org.akusher.crmfortutor.entity.Role;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.JwtTokenProvider;
import org.akusher.crmfortutor.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private StudentProfile student;

    @BeforeEach
    void setUp() {
        student = StudentProfile.builder()
                .id(10L)
                .firstName("Elena")
                .lastName("Kuznetsova")
                .inviteToken("valid-token-123")
                .inviteTokenExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("registerStudent - success")
    void registerStudent_Success() {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("valid-token-123")
                .email("student@example.com")
                .password("password123")
                .build();

        when(studentProfileRepository.findByInviteToken("valid-token-123")).thenReturn(Optional.of(student));
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(50L)
                .email("student@example.com")
                .password("encodedPassword")
                .role(Role.ROLE_STUDENT)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateAccessToken(50L, "student@example.com", Role.ROLE_STUDENT)).thenReturn("access-token-xyz");
        when(tokenProvider.generateRefreshToken("student@example.com")).thenReturn("refresh-token-xyz");

        AuthResponse response = authService.registerStudent(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-xyz");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-xyz");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_STUDENT);

        assertThat(student.getUser()).isEqualTo(savedUser);
        assertThat(student.getInviteToken()).isNull();
        assertThat(student.getInviteTokenExpiresAt()).isNull();

        verify(studentProfileRepository).save(student);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerStudent - invalid invite token")
    void registerStudent_InvalidToken() {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("non-existent-token")
                .email("student@example.com")
                .password("password123")
                .build();

        when(studentProfileRepository.findByInviteToken("non-existent-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerStudent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid invite token");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent - invite token already used")
    void registerStudent_AlreadyUsed() {
        student.setUser(User.builder().id(99L).build());

        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("valid-token-123")
                .email("student@example.com")
                .password("password123")
                .build();

        when(studentProfileRepository.findByInviteToken("valid-token-123")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> authService.registerStudent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invite token has already been used");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent - invite token expired")
    void registerStudent_ExpiredToken() {
        student.setInviteTokenExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("valid-token-123")
                .email("student@example.com")
                .password("password123")
                .build();

        when(studentProfileRepository.findByInviteToken("valid-token-123")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> authService.registerStudent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invite token has expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerStudent - email already in use")
    void registerStudent_EmailAlreadyExists() {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("valid-token-123")
                .email("existing@example.com")
                .password("password123")
                .build();

        when(studentProfileRepository.findByInviteToken("valid-token-123")).thenReturn(Optional.of(student));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStudent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User with email 'existing@example.com' already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - returns role in AuthResponse")
    void login_ReturnsRole() {
        LoginRequest request = LoginRequest.builder()
                .email("student@example.com")
                .password("password123")
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .email("student@example.com")
                .role(Role.ROLE_STUDENT)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateAccessToken(principal)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(principal)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_STUDENT);
    }
}
