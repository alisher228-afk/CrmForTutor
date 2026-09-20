package org.akusher.crmfortutor.controller;

import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.exception.GlobalExceptionHandler;
import org.akusher.crmfortutor.service.AttachmentService;
import org.akusher.crmfortutor.service.DownloadedAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private AttachmentController attachmentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attachmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/homework/{id}/attachments - 201 Created")
    void uploadAttachment_Success() throws Exception {
        AttachmentResponse response = AttachmentResponse.builder()
                .id(1L)
                .homeworkId(10L)
                .fileName("uuid-file.pdf")
                .originalFileName("essay.pdf")
                .contentType("application/pdf")
                .sizeBytes(2048L)
                .uploadedByUserId(5L)
                .uploadedAt(Instant.now())
                .build();

        when(attachmentService.uploadAttachment(eq(10L), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "essay.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/homework/10/attachments").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFileName").value("essay.pdf"))
                .andExpect(jsonPath("$.sizeBytes").value(2048));
    }

    @Test
    @DisplayName("GET /api/v1/homework/{id}/attachments - 200 OK")
    void getAttachments_Success() throws Exception {
        AttachmentResponse response = AttachmentResponse.builder()
                .id(1L)
                .homeworkId(10L)
                .fileName("uuid-file.pdf")
                .originalFileName("essay.pdf")
                .contentType("application/pdf")
                .sizeBytes(2048L)
                .build();

        when(attachmentService.getAttachments(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/homework/10/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].originalFileName").value("essay.pdf"));
    }

    @Test
    @DisplayName("GET /api/v1/attachments/{id}/download - 200 OK")
    void downloadAttachment_Success() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("PDF data stream".getBytes());
        DownloadedAttachment downloaded = new DownloadedAttachment(
                resource, "essay.pdf", "application/pdf", (long) "PDF data stream".length()
        );

        when(attachmentService.downloadAttachment(1L)).thenReturn(downloaded);

        mockMvc.perform(get("/api/v1/attachments/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("filename=\"essay.pdf\"")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("PDF data stream".getBytes()));
    }

    @Test
    @DisplayName("DELETE /api/v1/attachments/{id} - 204 No Content")
    void deleteAttachment_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/attachments/1"))
                .andExpect(status().isNoContent());

        verify(attachmentService).deleteAttachment(1L);
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException handled as 400 Bad Request")
    void uploadAttachment_ExceedsMaxSize_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", "too big".getBytes()
        );

        when(attachmentService.uploadAttachment(eq(10L), any()))
                .thenThrow(new MaxUploadSizeExceededException(10485760));

        mockMvc.perform(multipart("/api/v1/homework/10/attachments").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
