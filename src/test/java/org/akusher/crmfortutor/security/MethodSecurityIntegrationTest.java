package org.akusher.crmfortutor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MethodSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot access tutor endpoint /api/v1/students (403 Forbidden)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessTutorEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot access tutor endpoint /api/v1/lessons (403 Forbidden)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessLessonsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/lessons")
                        .param("from", "2026-09-01T00:00:00")
                        .param("to", "2026-09-30T23:59:59"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot access tutor endpoint /api/v1/payments/student/1 (403 Forbidden)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessPaymentsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/payments/student/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot access tutor endpoint /api/v1/homework/student/1 (403 Forbidden)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessHomeworkEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/homework/student/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_TUTOR cannot access student cabinet /api/v1/me/profile (403 Forbidden)")
    @WithMockUser(roles = "TUTOR")
    void tutorCannotAccessStudentProfile() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot access tutor endpoint PATCH /api/v1/homework/1/status (403 Forbidden)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessTutorHomeworkStatusPatch() throws Exception {
        mockMvc.perform(patch("/api/v1/homework/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_TUTOR cannot access student endpoint PATCH /api/v1/me/homework/1/submit (403 Forbidden)")
    @WithMockUser(roles = "TUTOR")
    void tutorCannotAccessStudentHomeworkSubmitPatch() throws Exception {
        mockMvc.perform(patch("/api/v1/me/homework/1/submit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_STUDENT cannot access tutor endpoint POST /api/v1/students/1/telegram-link-code (403 Forbidden)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessTelegramLinkCode() throws Exception {
        mockMvc.perform(post("/api/v1/students/1/telegram-link-code"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user can access webhook POST /api/v1/telegram/webhook (200 OK)")
    void anonymousCanAccessTelegramWebhook() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
