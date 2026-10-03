package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.TestCreateRequest;
import org.akusher.crmfortutor.dto.request.TestUpdateRequest;
import org.akusher.crmfortutor.dto.response.TestResponse;
import org.akusher.crmfortutor.entity.TestEntity;
import org.akusher.crmfortutor.entity.TestType;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.TestMapper;
import org.akusher.crmfortutor.repository.TestRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TestMapper testMapper;

    @Transactional(readOnly = true)
    public Page<TestResponse> getTests(String search, TestType type, Pageable pageable) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        Page<TestEntity> tests = testRepository.searchTests(tutorId, search, type, pageable);
        return tests.map(testMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getAllTests() {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        List<TestEntity> tests = testRepository.findByTutorIdOrderByCreatedAtDesc(tutorId);
        return testMapper.toResponseList(tests);
    }

    @Transactional(readOnly = true)
    public TestResponse getTestById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TestEntity test = testRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + id));
        return testMapper.toResponse(test);
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

        TestEntity saved = testRepository.save(entity);
        return testMapper.toResponse(saved);
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

        TestEntity updated = testRepository.save(test);
        return testMapper.toResponse(updated);
    }

    @Transactional
    public void deleteTest(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TestEntity test = testRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + id));

        testRepository.delete(test);
    }
}
