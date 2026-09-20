package org.akusher.crmfortutor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akusher.crmfortutor.dto.request.LoginRequest;
import org.akusher.crmfortutor.dto.request.StudentRegisterRequest;
import org.akusher.crmfortutor.dto.response.AuthResponse;
import org.akusher.crmfortutor.entity.Role;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-student - 201 Created")
    void registerStudent_Success() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("token-123")
                .email("student@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("token-xyz")
                .refreshToken("refresh-xyz")
                .role(Role.ROLE_STUDENT)
                .build();

        when(authService.registerStudent(any(StudentRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register-student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token-xyz"))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-student - 400 Bad Request on validation failure")
    void registerStudent_ValidationFailure() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("")
                .email("invalid-email")
                .password("123") // too short
                .build();

        mockMvc.perform(post("/api/v1/auth/register-student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register-student - 400 Bad Request on expired/invalid token")
    void registerStudent_ExpiredToken() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .inviteToken("expired-token")
                .email("student@example.com")
                .password("password123")
                .build();

        when(authService.registerStudent(any(StudentRegisterRequest.class)))
                .thenThrow(new BadRequestException("Invite token has expired"));

        mockMvc.perform(post("/api/v1/auth/register-student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invite token has expired"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - returns role")
    void login_ReturnsRole() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("student@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("token-xyz")
                .refreshToken("refresh-xyz")
                .role(Role.ROLE_STUDENT)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));
    }
}
