package org.akusher.crmfortutor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akusher.crmfortutor.dto.request.ChangePasswordRequest;
import org.akusher.crmfortutor.dto.request.UserProfileUpdateRequest;
import org.akusher.crmfortutor.dto.response.UserProfileResponse;
import org.akusher.crmfortutor.entity.Role;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.service.ProfileService;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/profile - 200 OK")
    void getProfile_Success() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(1L)
                .email("tutor@example.com")
                .role(Role.ROLE_TUTOR)
                .firstName("Анна")
                .lastName("Иванова")
                .phone("+77001234567")
                .specialization("Математика")
                .createdAt(Instant.now())
                .build();

        when(profileService.getProfile()).thenReturn(response);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tutor@example.com"))
                .andExpect(jsonPath("$.firstName").value("Анна"))
                .andExpect(jsonPath("$.specialization").value("Математика"));
    }

    @Test
    @DisplayName("PUT /api/v1/profile - 200 OK")
    void updateProfile_Success() throws Exception {
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .firstName("Анна")
                .lastName("Петрова")
                .phone("+77009876543")
                .specialization("Физика")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(1L)
                .email("tutor@example.com")
                .role(Role.ROLE_TUTOR)
                .firstName("Анна")
                .lastName("Петрова")
                .phone("+77009876543")
                .specialization("Физика")
                .build();

        when(profileService.updateProfile(any(UserProfileUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Петрова"))
                .andExpect(jsonPath("$.specialization").value("Физика"));
    }

    @Test
    @DisplayName("POST /api/v1/profile/change-password - 200 OK")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldSecret123")
                .newPassword("newSecret456")
                .build();

        mockMvc.perform(post("/api/v1/profile/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Пароль успешно изменен"));

        verify(profileService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/profile/change-password - validation failure on short password")
    void changePassword_ValidationFailure() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("123")
                .newPassword("456")
                .build();

        mockMvc.perform(post("/api/v1/profile/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
