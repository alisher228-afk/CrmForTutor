package org.akusher.crmfortutor.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private TestMapper testMapper;

    @InjectMocks
    private TestService testService;

    private Long tutorId;
    private User tutor;
    private TestEntity testEntity;

    @BeforeEach
    void setUp() {
        tutorId = 1L;
        tutor = User.builder().id(tutorId).email("tutor@example.com").build();
        testEntity = TestEntity.builder()
                .id(10L)
                .tutor(tutor)
                .title("Sample Quiz")
                .type(TestType.INTERNAL)
                .build();
    }

    @Test
    @DisplayName("createTest - Internal Success")
    void createTest_Internal_Success() {
        TestCreateRequest request = TestCreateRequest.builder()
                .title("Grammar Test")
                .type(TestType.INTERNAL)
                .questionsJson("[{\"question\":\"Q1\"}]")
                .build();

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(testMapper.toEntity(request)).thenReturn(testEntity);
        when(testRepository.save(any(TestEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TestResponse response = TestResponse.builder().id(10L).title("Grammar Test").type(TestType.INTERNAL).build();
        when(testMapper.toResponse(any(TestEntity.class))).thenReturn(response);

        TestResponse result = testService.createTest(request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Grammar Test");
        verify(testRepository).save(any(TestEntity.class));
    }

    @Test
    @DisplayName("createTest - External missing URL throws BadRequestException")
    void createTest_External_MissingUrl_Throws() {
        TestCreateRequest request = TestCreateRequest.builder()
                .title("Quizland Quiz")
                .type(TestType.EXTERNAL)
                .externalUrl("")
                .build();

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> testService.createTest(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("External URL is required");
    }

    @Test
    @DisplayName("getTestById - Success")
    void getTestById_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(testRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(testEntity));

        TestResponse response = TestResponse.builder().id(10L).title("Sample Quiz").build();
        when(testMapper.toResponse(testEntity)).thenReturn(response);

        TestResponse result = testService.getTestById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getTestById - Not Found")
    void getTestById_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(testRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.getTestById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Test not found with id: 999");
    }

    @Test
    @DisplayName("updateTest - Success")
    void updateTest_Success() {
        TestUpdateRequest request = TestUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(testRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(testEntity));
        when(testRepository.save(any(TestEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TestResponse response = TestResponse.builder().id(10L).title("Updated Title").build();
        when(testMapper.toResponse(testEntity)).thenReturn(response);

        TestResponse result = testService.updateTest(10L, request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(testMapper).updateEntityFromDto(request, testEntity);
    }

    @Test
    @DisplayName("deleteTest - Success")
    void deleteTest_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(testRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(testEntity));

        testService.deleteTest(10L);

        verify(testRepository).delete(testEntity);
    }
}
