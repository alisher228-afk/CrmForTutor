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
import org.akusher.crmfortutor.dto.response.HomeworkStatsResponse;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;

import java.util.List;

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
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
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

    @Test
    @DisplayName("GET /api/v1/homework/student/{studentId} - 200 OK (unpaged list by default)")
    void getHomeworkByStudent_Unpaged_Success() throws Exception {
        HomeworkResponse response = HomeworkResponse.builder()
                .id(1L)
                .lessonId(10L)
                .studentId(2L)
                .title("Math Homework")
                .status(HomeworkStatus.ASSIGNED)
                .attachmentsCount(1)
                .build();

        when(homeworkService.getHomeworkListByStudent(eq(2L), any(), any()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/homework/student/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Math Homework"))
                .andExpect(jsonPath("$[0].attachmentsCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/homework/student/{studentId} - 200 OK (paginated when page param present)")
    void getHomeworkByStudent_Paginated_Success() throws Exception {
        HomeworkResponse response = HomeworkResponse.builder()
                .id(1L)
                .lessonId(10L)
                .studentId(2L)
                .title("Math Homework")
                .status(HomeworkStatus.ASSIGNED)
                .attachmentsCount(1)
                .build();

        when(homeworkService.getHomeworkByStudent(eq(2L), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/homework/student/2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Math Homework"))
                .andExpect(jsonPath("$.content[0].attachmentsCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/homework/student/{studentId}/stats - 200 OK")
    void getHomeworkStatsByStudent_Success() throws Exception {
        HomeworkStatsResponse stats = HomeworkStatsResponse.builder()
                .totalCount(15)
                .assignedCount(5)
                .submittedCount(3)
                .reviewedCount(7)
                .build();

        when(homeworkService.getHomeworkStatsByStudent(2L)).thenReturn(stats);

        mockMvc.perform(get("/api/v1/homework/student/2/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(15))
                .andExpect(jsonPath("$.assignedCount").value(5))
                .andExpect(jsonPath("$.submittedCount").value(3))
                .andExpect(jsonPath("$.reviewedCount").value(7));
    }

    @Test
    @DisplayName("GET /api/v1/homework/stats - 200 OK")
    void getTutorHomeworkStats_Success() throws Exception {
        HomeworkStatsResponse stats = HomeworkStatsResponse.builder()
                .totalCount(10)
                .assignedCount(4)
                .submittedCount(2)
                .reviewedCount(4)
                .build();

        when(homeworkService.getTutorHomeworkStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/homework/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.assignedCount").value(4))
                .andExpect(jsonPath("$.submittedCount").value(2))
                .andExpect(jsonPath("$.reviewedCount").value(4));
    }
}
