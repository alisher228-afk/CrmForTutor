package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.StudentCreateRequest;
import org.akusher.crmfortutor.dto.request.StudentUpdateRequest;
import org.akusher.crmfortutor.dto.response.StudentGroupResponse;
import org.akusher.crmfortutor.dto.response.StudentInviteResponse;
import org.akusher.crmfortutor.dto.response.StudentResponse;
import org.akusher.crmfortutor.dto.response.TelegramLinkCodeResponse;
import org.akusher.crmfortutor.entity.StudentStatus;
import org.akusher.crmfortutor.service.StudentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<Page<StudentResponse>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String groupName,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentService.getStudents(search, status, format, groupName, pageable));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<StudentGroupResponse>> getGroups() {
        return ResponseEntity.ok(studentService.getGroups());
    }

    @GetMapping("/groups/{groupName}/students")
    public ResponseEntity<List<StudentResponse>> getStudentsByGroup(@PathVariable String groupName) {
        return ResponseEntity.ok(studentService.getStudentsByGroup(groupName));
    }


    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<StudentInviteResponse> createInviteToken(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.createInviteToken(id));
    }

    @PostMapping("/{id}/telegram-link-code")
    public ResponseEntity<TelegramLinkCodeResponse> generateTelegramLinkCode(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.generateTelegramLinkCode(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
