package org.akusher.crmfortutor.controller;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkSubmitRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.dto.response.StudentPaymentsResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.service.StudentSelfService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
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
}
