package org.akusher.crmfortutor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.akusher.crmfortutor.dto.request.LessonUpdateRequest;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.service.LessonService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private LessonController lessonController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lessonController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/v1/lessons/{id} - 200 OK")
    void getLessonById_Success() throws Exception {
        LessonResponse response = LessonResponse.builder()
                .id(1L)
                .studentId(2L)
                .topic("Algebra")
                .build();
        when(lessonService.getLessonById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/lessons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.topic").value("Algebra"));
    }

    @Test
    @DisplayName("GET /api/v1/lessons/{id} - 404 Not Found")
    void getLessonById_NotFound() throws Exception {
        when(lessonService.getLessonById(99L))
                .thenThrow(new ResourceNotFoundException("Lesson not found with id: 99"));

        mockMvc.perform(get("/api/v1/lessons/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Lesson not found with id: 99"));
    }

    @Test
    @DisplayName("PUT /api/v1/lessons/{id} - 200 OK")
    void updateLesson_Success() throws Exception {
        LessonUpdateRequest request = LessonUpdateRequest.builder()
                .startTime(LocalDateTime.of(2026, 9, 21, 10, 0))
                .endTime(LocalDateTime.of(2026, 9, 21, 11, 0))
                .topic("Trigonometry")
                .meetingUrl("https://meet.google.com/xyz")
                .build();

        LessonResponse response = LessonResponse.builder()
                .id(1L)
                .startTime(LocalDateTime.of(2026, 9, 21, 10, 0))
                .endTime(LocalDateTime.of(2026, 9, 21, 11, 0))
                .topic("Trigonometry")
                .meetingUrl("https://meet.google.com/xyz")
                .build();
        when(lessonService.updateLesson(eq(1L), any(LessonUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/lessons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.topic").value("Trigonometry"))
                .andExpect(jsonPath("$.meetingUrl").value("https://meet.google.com/xyz"));
    }

    @Test
    @DisplayName("PUT /api/v1/lessons/{id} - 400 Bad Request when startTime is null")
    void updateLesson_ValidationError() throws Exception {
        LessonUpdateRequest request = LessonUpdateRequest.builder()
                .endTime(LocalDateTime.of(2026, 9, 21, 11, 0))
                .topic("Trigonometry")
                .build();

        mockMvc.perform(put("/api/v1/lessons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
