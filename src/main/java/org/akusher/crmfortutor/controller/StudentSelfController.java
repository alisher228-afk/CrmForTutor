package org.akusher.crmfortutor.controller;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkSubmitRequest;
import org.akusher.crmfortutor.dto.request.LessonCancelRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.dto.response.MaterialResponse;
import org.akusher.crmfortutor.dto.response.StudentPaymentsResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.service.DownloadedAttachment;
import org.akusher.crmfortutor.service.StudentSelfService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentSelfController {

    private final StudentSelfService studentSelfService;

    @GetMapping("/profile")
    public ResponseEntity<StudentSelfResponse> getProfile() {
        return ResponseEntity.ok(studentSelfService.getProfile());
    }

    @GetMapping("/lessons")
    public ResponseEntity<List<LessonResponse>> getLessons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(studentSelfService.getLessons(from, to));
    }

    @GetMapping("/homework")
    public ResponseEntity<List<HomeworkResponse>> getHomework() {
        return ResponseEntity.ok(studentSelfService.getHomework());
    }

    @GetMapping("/payments")
    public ResponseEntity<StudentPaymentsResponse> getPayments() {
        return ResponseEntity.ok(studentSelfService.getPayments());
    }

    @PatchMapping("/homework/{id}/submit")
    public ResponseEntity<HomeworkResponse> submitHomework(
            @PathVariable Long id,
            @RequestBody(required = false) HomeworkSubmitRequest request) {
        return ResponseEntity.ok(studentSelfService.submitHomework(id, request));
    }

    @PatchMapping("/lessons/{id}/cancel")
    public ResponseEntity<LessonResponse> cancelLesson(
            @PathVariable Long id,
            @RequestBody(required = false) LessonCancelRequest request) {
        return ResponseEntity.ok(studentSelfService.cancelLesson(id, request));
    }

    @GetMapping("/materials")
    public ResponseEntity<List<MaterialResponse>> getMaterials(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(studentSelfService.getMaterials(search, category));
    }

    @GetMapping("/materials/categories")
    public ResponseEntity<List<String>> getMaterialCategories() {
        return ResponseEntity.ok(studentSelfService.getMaterialCategories());
    }

    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable Long id,
            @RequestParam(value = "inline", required = false, defaultValue = "false") boolean inline) {

        DownloadedAttachment downloaded = studentSelfService.downloadMaterial(id);

        MediaType mediaType;
        try {
            mediaType = downloaded.contentType() != null
                    ? MediaType.parseMediaType(downloaded.contentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(downloaded.originalFileName(), StandardCharsets.UTF_8)
                .build();

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());

        if (downloaded.sizeBytes() != null && downloaded.sizeBytes() > 0) {
            builder.contentLength(downloaded.sizeBytes());
        }

        return builder.body(downloaded.resource());
    }
}
