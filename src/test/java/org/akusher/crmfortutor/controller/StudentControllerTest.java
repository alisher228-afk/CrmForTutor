package org.akusher.crmfortutor.controller;

import org.akusher.crmfortutor.dto.response.StudentInviteResponse;
import org.akusher.crmfortutor.dto.response.TelegramLinkCodeResponse;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/students/{id}/invite - 200 OK")
    void createInviteToken_Success() throws Exception {
        StudentInviteResponse response = StudentInviteResponse.builder()
                .studentId(5L)
                .inviteToken("test-uuid-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(studentService.createInviteToken(5L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/5/invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(5))
                .andExpect(jsonPath("$.inviteToken").value("test-uuid-token"));
    }

    @Test
    @DisplayName("POST /api/v1/students/{id}/invite - 404 Not Found")
    void createInviteToken_NotFound() throws Exception {
        when(studentService.createInviteToken(99L))
                .thenThrow(new ResourceNotFoundException("Student not found with id: 99"));

        mockMvc.perform(post("/api/v1/students/99/invite"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found with id: 99"));
    }

    @Test
    @DisplayName("POST /api/v1/students/{id}/invite - 400 Bad Request when already linked")
    void createInviteToken_AlreadyLinked() throws Exception {
        when(studentService.createInviteToken(5L))
                .thenThrow(new BadRequestException("Student is already linked to a user account"));

        mockMvc.perform(post("/api/v1/students/5/invite"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Student is already linked to a user account"));
    }

    @Test
    @DisplayName("POST /api/v1/students/{id}/telegram-link-code - 200 OK")
    void generateTelegramLinkCode_Success() throws Exception {
        TelegramLinkCodeResponse response = TelegramLinkCodeResponse.builder()
                .studentId(5L)
                .linkCode("654321")
                .expiresAt(Instant.now().plusSeconds(900))
                .botUsername("TestTutorBot")
                .build();

        when(studentService.generateTelegramLinkCode(5L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/students/5/telegram-link-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(5))
                .andExpect(jsonPath("$.linkCode").value("654321"))
                .andExpect(jsonPath("$.code").value("654321"))
                .andExpect(jsonPath("$.botUsername").value("TestTutorBot"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/students/{id}/telegram-link-code - 404 Not Found")
    void generateTelegramLinkCode_NotFound() throws Exception {
        when(studentService.generateTelegramLinkCode(99L))
                .thenThrow(new ResourceNotFoundException("Student not found with id: 99"));

        mockMvc.perform(post("/api/v1/students/99/telegram-link-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student not found with id: 99"));
    }
}
