package org.akusher.crmfortutor.controller;

import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.dto.response.PaymentResponse;
import org.akusher.crmfortutor.dto.response.StudentPaymentsResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.service.StudentSelfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.akusher.crmfortutor.dto.request.HomeworkSubmitRequest;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentSelfControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudentSelfService studentSelfService;

    @InjectMocks
    private StudentSelfController studentSelfController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studentSelfController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me/profile - 200 OK")
    void getProfile_Success() throws Exception {
        StudentSelfResponse response = StudentSelfResponse.builder()
                .id(1L)
                .userId(10L)
                .tutorId(2L)
                .firstName("Anna")
                .lastName("Ivanova")
                .lessonBalance(5)
                .build();

        when(studentSelfService.getProfile()).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.lessonBalance").value(5))
                .andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/me/lessons - 200 OK")
    void getLessons_Success() throws Exception {
        LessonResponse lesson = LessonResponse.builder()
                .id(1L)
                .topic("Biology")
                .status(LessonStatus.SCHEDULED)
                .build();

        when(studentSelfService.getLessons(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(lesson));

        mockMvc.perform(get("/api/v1/me/lessons")
                        .param("from", "2026-09-01T00:00:00")
                        .param("to", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].topic").value("Biology"));
    }

    @Test
    @DisplayName("GET /api/v1/me/homework - 200 OK")
    void getHomework_Success() throws Exception {
        HomeworkResponse homework = HomeworkResponse.builder()
                .id(5L)
                .title("Essay")
                .status(HomeworkStatus.ASSIGNED)
                .build();

        when(studentSelfService.getHomework()).thenReturn(List.of(homework));

        mockMvc.perform(get("/api/v1/me/homework"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].title").value("Essay"));
    }

    @Test
    @DisplayName("GET /api/v1/me/payments - 200 OK")
    void getPayments_Success() throws Exception {
        PaymentResponse payment = PaymentResponse.builder()
                .id(10L)
                .amount(BigDecimal.valueOf(5000))
                .lessonsCount(5)
                .paymentDate(LocalDate.of(2026, 9, 10))
                .build();

        StudentPaymentsResponse response = StudentPaymentsResponse.builder()
                .lessonBalance(5)
                .payments(List.of(payment))
                .build();

        when(studentSelfService.getPayments()).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonBalance").value(5))
                .andExpect(jsonPath("$.payments[0].id").value(10))
                .andExpect(jsonPath("$.payments[0].amount").value(5000));
    }

    @Test
    @DisplayName("PATCH /api/v1/me/homework/{id}/submit - 200 OK")
    void submitHomework_Success() throws Exception {
        HomeworkResponse response = HomeworkResponse.builder()
                .id(5L)
                .title("Essay")
                .status(HomeworkStatus.SUBMITTED)
                .studentNotes("My completed task: https://github.com/my-repo")
                .build();

        when(studentSelfService.submitHomework(eq(5L), any())).thenReturn(response);

        HomeworkSubmitRequest request = new HomeworkSubmitRequest("My completed task: https://github.com/my-repo");

        mockMvc.perform(patch("/api/v1/me/homework/5/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.studentNotes").value("My completed task: https://github.com/my-repo"));
    }

    @Test
    @DisplayName("PATCH /api/v1/me/homework/{id}/submit - 400 Bad Request when already submitted")
    void submitHomework_AlreadySubmitted() throws Exception {
        when(studentSelfService.submitHomework(eq(5L), any()))
                .thenThrow(new BadRequestException("Homework is already submitted"));

        HomeworkSubmitRequest request = new HomeworkSubmitRequest("My answer");

        mockMvc.perform(patch("/api/v1/me/homework/5/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Homework is already submitted"));
    }
}
