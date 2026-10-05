package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkCreateRequest;
import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.HomeworkStatsResponse;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.service.HomeworkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/homework")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @PostMapping
    public ResponseEntity<HomeworkResponse> createHomework(@Valid @RequestBody HomeworkCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(homeworkService.createHomework(request));
    }

    @PostMapping("/group")
    public ResponseEntity<List<HomeworkResponse>> createGroupHomework(@Valid @RequestBody HomeworkCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(homeworkService.createGroupHomework(request));
    }

    @GetMapping("/group")
    public ResponseEntity<List<HomeworkResponse>> getHomeworkByGroupQuery(@RequestParam("groupName") String groupName) {
        return ResponseEntity.ok(homeworkService.getHomeworkByGroup(groupName));
    }

    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<HomeworkResponse>> getHomeworkByGroup(@PathVariable String groupName) {
        return ResponseEntity.ok(homeworkService.getHomeworkByGroup(groupName));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getHomeworkByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) HomeworkStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        if (page != null) {
            return ResponseEntity.ok(homeworkService.getHomeworkByStudent(studentId, status, search, pageable));
        }
        return ResponseEntity.ok(homeworkService.getHomeworkListByStudent(studentId, status, search));
    }

    @GetMapping("/student/{studentId}/stats")
    public ResponseEntity<HomeworkStatsResponse> getHomeworkStatsByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(homeworkService.getHomeworkStatsByStudent(studentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HomeworkResponse> getHomeworkById(@PathVariable Long id) {
        return ResponseEntity.ok(homeworkService.getHomeworkById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<HomeworkResponse> updateHomeworkStatus(
            @PathVariable Long id,
            @Valid @RequestBody HomeworkStatusUpdateRequest request) {
        return ResponseEntity.ok(homeworkService.updateHomeworkStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHomework(@PathVariable Long id) {
        homeworkService.deleteHomework(id);
        return ResponseEntity.noContent().build();
    }
}
