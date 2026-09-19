package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkCreateRequest;
import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.service.HomeworkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @PostMapping
    public ResponseEntity<HomeworkResponse> createHomework(@Valid @RequestBody HomeworkCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(homeworkService.createHomework(request));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<HomeworkResponse>> getHomeworkByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(homeworkService.getHomeworkByStudent(studentId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<HomeworkResponse> updateHomeworkStatus(
            @PathVariable Long id,
            @Valid @RequestBody HomeworkStatusUpdateRequest request) {
        return ResponseEntity.ok(homeworkService.updateHomeworkStatus(id, request));
    }
}
