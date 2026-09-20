package org.akusher.crmfortutor.controller;

import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.service.HomeworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HomeworkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HomeworkService homeworkService;

    @InjectMocks
    private HomeworkController homeworkController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeworkController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/homework/{id} - 200 OK")
    void getHomeworkById_Success() throws Exception {
        HomeworkResponse response = HomeworkResponse.builder()
                .id(1L)
                .lessonId(10L)
                .title("Math Exercise")
                .status(HomeworkStatus.ASSIGNED)
                .build();

        when(homeworkService.getHomeworkById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/homework/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Math Exercise"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("GET /api/v1/homework/{id} - 404 Not Found")
    void getHomeworkById_NotFound() throws Exception {
        when(homeworkService.getHomeworkById(99L))
                .thenThrow(new ResourceNotFoundException("Homework not found with id: 99"));

        mockMvc.perform(get("/api/v1/homework/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Homework not found with id: 99"));
    }

    @Test
    @DisplayName("PATCH /api/v1/homework/{id}/status - 200 OK")
    void updateHomeworkStatus_Success() throws Exception {
        HomeworkResponse response = HomeworkResponse.builder()
                .id(1L)
                .lessonId(10L)
                .title("Math Exercise")
                .status(HomeworkStatus.REVIEWED)
                .build();

        when(homeworkService.updateHomeworkStatus(eq(1L), any())).thenReturn(response);

        HomeworkStatusUpdateRequest request = HomeworkStatusUpdateRequest.builder()
                .status(HomeworkStatus.REVIEWED)
                .build();

        mockMvc.perform(patch("/api/v1/homework/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REVIEWED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/homework/{id}/status - 400 Bad Request when invalid transition")
    void updateHomeworkStatus_BadRequest() throws Exception {
        when(homeworkService.updateHomeworkStatus(eq(1L), any()))
                .thenThrow(new BadRequestException("Tutor can only change homework status to REVIEWED"));

        HomeworkStatusUpdateRequest request = HomeworkStatusUpdateRequest.builder()
                .status(HomeworkStatus.SUBMITTED)
                .build();

        mockMvc.perform(patch("/api/v1/homework/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Tutor can only change homework status to REVIEWED"));
    }
}
