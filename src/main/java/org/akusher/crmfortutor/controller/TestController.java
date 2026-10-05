package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.TestCreateRequest;
import org.akusher.crmfortutor.dto.request.TestUpdateRequest;
import org.akusher.crmfortutor.dto.response.TestResponse;
import org.akusher.crmfortutor.entity.TestType;
import org.akusher.crmfortutor.service.TestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tests")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @GetMapping
    public ResponseEntity<Page<TestResponse>> getTests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TestType type,
            @RequestParam(required = false) org.akusher.crmfortutor.entity.TestTargetType targetType,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) Long studentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(testService.getTests(search, type, targetType, groupName, studentId, pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TestResponse>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestResponse> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getTestById(id));
    }

    @PostMapping
    public ResponseEntity<TestResponse> createTest(@Valid @RequestBody TestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testService.createTest(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestResponse> updateTest(
            @PathVariable Long id,
            @Valid @RequestBody TestUpdateRequest request) {
        return ResponseEntity.ok(testService.updateTest(id, request));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<List<org.akusher.crmfortutor.dto.response.TestSubmissionResponse>> getTestSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(testService.getTestSubmissions(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        testService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }
}
