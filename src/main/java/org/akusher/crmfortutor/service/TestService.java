package org.akusher.crmfortutor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.TestCreateRequest;
import org.akusher.crmfortutor.dto.request.TestSubmissionRequest;
import org.akusher.crmfortutor.dto.request.TestUpdateRequest;
import org.akusher.crmfortutor.dto.response.TestResponse;
import org.akusher.crmfortutor.dto.response.TestSubmissionResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.TestEntity;
import org.akusher.crmfortutor.entity.TestSubmissionEntity;
import org.akusher.crmfortutor.entity.TestTargetType;
import org.akusher.crmfortutor.entity.TestType;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.TestMapper;
import org.akusher.crmfortutor.mapper.TestSubmissionMapper;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.TestRepository;
import org.akusher.crmfortutor.repository.TestSubmissionRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentStudentProvider;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final TestSubmissionRepository testSubmissionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentStudentProvider currentStudentProvider;
    private final TestMapper testMapper;
    private final TestSubmissionMapper testSubmissionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Page<TestResponse> getTests(String search, TestType type, TestTargetType targetType, String groupName, Long studentId, Pageable pageable) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        Page<TestEntity> tests = testRepository.searchTests(tutorId, search, type, targetType, groupName, studentId, pageable);
        return tests.map(t -> {
            TestResponse r = testMapper.toResponse(t);
            enrichWithSubmissionStats(r);
            return r;
        });
    }

    @Transactional(readOnly = true)
    public Page<TestResponse> getTests(String search, TestType type, Pageable pageable) {
        return getTests(search, type, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getAllTests() {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        List<TestEntity> tests = testRepository.findByTutorIdOrderByCreatedAtDesc(tutorId);
        List<TestResponse> responses = testMapper.toResponseList(tests);
        responses.forEach(this::enrichWithSubmissionStats);
        return responses;
    }

    @Transactional(readOnly = true)
    public TestResponse getTestById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TestEntity test = testRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + id));
        TestResponse response = testMapper.toResponse(test);
        enrichWithSubmissionStats(response);
        return response;
    }

    @Transactional
    public TestResponse createTest(TestCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));

        if (request.getType() == TestType.EXTERNAL) {
            if (request.getExternalUrl() == null || request.getExternalUrl().isBlank()) {
                throw new BadRequestException("External URL is required for EXTERNAL tests");
            }
        }

        TestEntity entity = testMapper.toEntity(request);
        entity.setTutor(tutor);

        applyTargetAssignment(entity, request.getTargetType(), request.getGroupName(), request.getStudentId(), tutorId);

        TestEntity saved = testRepository.save(entity);
        TestResponse response = testMapper.toResponse(saved);
        enrichWithSubmissionStats(response);
        return response;
    }

    @Transactional
    public TestResponse updateTest(Long id, TestUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TestEntity test = testRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + id));

        testMapper.updateEntityFromDto(request, test);

        if (request.getExternalUrl() != null) {
            test.setExternalUrl(request.getExternalUrl().trim());
        }
        if (request.getQuestionsJson() != null) {
            test.setQuestionsJson(request.getQuestionsJson().trim());
        }

        if (request.getTargetType() != null) {
            applyTargetAssignment(test, request.getTargetType(), request.getGroupName(), request.getStudentId(), tutorId);
        }

        TestEntity updated = testRepository.save(test);
        TestResponse response = testMapper.toResponse(updated);
        enrichWithSubmissionStats(response);
        return response;
    }

    private void applyTargetAssignment(TestEntity entity, TestTargetType targetType, String groupName, Long studentId, Long tutorId) {
        if (targetType == TestTargetType.GROUP) {
            if (groupName == null || groupName.isBlank()) {
                throw new BadRequestException("Название группы обязательно для группового теста");
            }
            entity.setTargetType(TestTargetType.GROUP);
            entity.setGroupName(groupName.trim());
            entity.setStudent(null);
        } else if (targetType == TestTargetType.INDIVIDUAL) {
            if (studentId == null) {
                throw new BadRequestException("Ученик обязателен для индивидуального теста");
            }
            StudentProfile student = studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
            entity.setTargetType(TestTargetType.INDIVIDUAL);
            entity.setStudent(student);
            entity.setGroupName(null);
        } else {
            entity.setTargetType(TestTargetType.ALL);
            entity.setGroupName(null);
            entity.setStudent(null);
        }
    }

    @Transactional
    public void deleteTest(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TestEntity test = testRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + id));

        testRepository.delete(test);
    }

    @Transactional(readOnly = true)
    public List<TestSubmissionResponse> getTestSubmissions(Long testId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TestEntity test = testRepository.findByIdAndTutorId(testId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        List<TestSubmissionEntity> submissions = testSubmissionRepository.findByTestIdOrderBySubmittedAtDesc(test.getId());
        return testSubmissionMapper.toResponseList(submissions);
    }

    // ==========================================
    // Student Methods
    // ==========================================

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsForCurrentStudent(String search, TestType type) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        Long tutorId = student.getTutor().getId();

        String groupName = (student.getGroupName() != null && !student.getGroupName().isBlank())
                ? student.getGroupName().trim()
                : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<TestEntity> tests = testRepository.findTestsForStudent(
                tutorId,
                student.getId(),
                groupName,
                cleanSearch,
                type
        );
        if (tests.isEmpty()) {
            return List.of();
        }

        List<Long> testIds = tests.stream().map(TestEntity::getId).toList();
        List<TestSubmissionEntity> submissions = testSubmissionRepository.findByStudentIdAndTestIds(student.getId(), testIds);
        Map<Long, TestSubmissionEntity> submissionMap = submissions.stream()
                .collect(Collectors.toMap(s -> s.getTest().getId(), s -> s, (s1, s2) -> s1));

        List<TestResponse> responses = testMapper.toResponseList(tests);
        for (TestResponse r : responses) {
            TestSubmissionEntity sub = submissionMap.get(r.getId());
            if (sub != null) {
                r.setMySubmission(testSubmissionMapper.toResponse(sub));
            }
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public TestResponse getTestForCurrentStudent(Long id) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        TestEntity test = testRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + id));

        checkStudentAccess(test, student);

        TestResponse response = testMapper.toResponse(test);
        testSubmissionRepository.findTopByTestIdAndStudentIdOrderBySubmittedAtDesc(id, student.getId())
                .ifPresent(sub -> response.setMySubmission(testSubmissionMapper.toResponse(sub)));
        return response;
    }

    @Transactional
    public TestSubmissionResponse submitTestForCurrentStudent(Long testId, TestSubmissionRequest request) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        TestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        checkStudentAccess(test, student);

        int totalQuestions = 0;
        int score = 0;

        if (test.getType() == TestType.INTERNAL && test.getQuestionsJson() != null && !test.getQuestionsJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(test.getQuestionsJson());
                JsonNode questionsNode = root.isArray() ? root : (root.has("questions") ? root.get("questions") : null);
                if (questionsNode != null && questionsNode.isArray()) {
                    totalQuestions = questionsNode.size();
                    JsonNode answersNode = null;
                    if (request != null && request.getAnswersJson() != null && !request.getAnswersJson().isBlank()) {
                        answersNode = objectMapper.readTree(request.getAnswersJson());
                    }

                    for (int i = 0; i < totalQuestions; i++) {
                        JsonNode q = questionsNode.get(i);
                        int correctIndex = q.has("correctOptionIndex") ? q.get("correctOptionIndex").asInt(0) : 0;
                        int chosenIndex = -1;
                        if (answersNode != null) {
                            if (answersNode.isObject() && answersNode.has(String.valueOf(i))) {
                                chosenIndex = answersNode.get(String.valueOf(i)).asInt(-1);
                            } else if (answersNode.isArray() && i < answersNode.size()) {
                                chosenIndex = answersNode.get(i).asInt(-1);
                            }
                        }
                        if (chosenIndex == correctIndex) {
                            score++;
                        }
                    }
                }
            } catch (Exception e) {
                totalQuestions = Math.max(totalQuestions, 1);
            }
        } else {
            totalQuestions = 1;
            score = 1;
        }

        if (totalQuestions == 0) {
            totalQuestions = 1;
            score = 1;
        }

        int percentage = (int) Math.round((double) score * 100.0 / totalQuestions);

        TestSubmissionEntity submission = testSubmissionRepository
                .findTopByTestIdAndStudentIdOrderBySubmittedAtDesc(testId, student.getId())
                .orElseGet(() -> TestSubmissionEntity.builder()
                        .test(test)
                        .student(student)
                        .build());

        submission.setScore(score);
        submission.setTotalQuestions(totalQuestions);
        submission.setPercentage(percentage);
        submission.setTimeSpentSeconds(request != null ? request.getTimeSpentSeconds() : null);
        submission.setAnswersJson(request != null ? request.getAnswersJson() : null);
        submission.setSubmittedAt(Instant.now());

        TestSubmissionEntity saved = testSubmissionRepository.save(submission);
        return testSubmissionMapper.toResponse(saved);
    }

    private void checkStudentAccess(TestEntity test, StudentProfile student) {
        Long tutorId = student.getTutor().getId();
        if (!test.getTutor().getId().equals(tutorId)) {
            throw new BadRequestException("Этот тест не принадлежит вашему преподавателю");
        }

        boolean isAccessible = false;
        if (test.getTargetType() == null || test.getTargetType() == TestTargetType.ALL) {
            isAccessible = true;
        } else if (test.getTargetType() == TestTargetType.GROUP) {
            isAccessible = student.getGroupName() != null && !student.getGroupName().isBlank()
                    && student.getGroupName().trim().equalsIgnoreCase(test.getGroupName() != null ? test.getGroupName().trim() : "");
        } else if (test.getTargetType() == TestTargetType.INDIVIDUAL) {
            isAccessible = test.getStudent() != null && test.getStudent().getId().equals(student.getId());
        }

        if (!isAccessible) {
            throw new BadRequestException("Этот тест не назначен вам");
        }
    }

    private void enrichWithSubmissionStats(TestResponse response) {
        if (response == null || response.getId() == null) return;
        List<TestSubmissionEntity> subs = testSubmissionRepository.findByTestIdOrderBySubmittedAtDesc(response.getId());
        response.setSubmissionsCount((long) subs.size());
        if (!subs.isEmpty()) {
            double avg = subs.stream().mapToInt(TestSubmissionEntity::getPercentage).average().orElse(0.0);
            response.setAverageScore(Math.round(avg * 10.0) / 10.0);
        }
    }
}
