package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.HomeworkMapper;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkServiceTest {

    @Mock
    private HomeworkRepository homeworkRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private HomeworkMapper homeworkMapper;

    @InjectMocks
    private HomeworkService homeworkService;

    private Long tutorId;
    private Homework homework;

    @BeforeEach
    void setUp() {
        tutorId = 1L;
        User tutor = User.builder().id(tutorId).build();
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Oleg").build();
        Lesson lesson = Lesson.builder().id(5L).tutor(tutor).student(student).build();

        homework = Homework.builder()
                .id(50L)
                .lesson(lesson)
                .title("Math Homework #3")
                .description("Exercises 10 to 15 on page 42")
                .deadline(LocalDateTime.of(2026, 9, 25, 18, 0))
                .status(HomeworkStatus.ASSIGNED)
                .build();
    }

    @Test
    @DisplayName("getHomeworkById - success")
    void getHomeworkById_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(50L, tutorId)).thenReturn(Optional.of(homework));

        HomeworkResponse response = HomeworkResponse.builder()
                .id(50L)
                .lessonId(5L)
                .title("Math Homework #3")
                .description("Exercises 10 to 15 on page 42")
                .deadline(LocalDateTime.of(2026, 9, 25, 18, 0))
                .status(HomeworkStatus.ASSIGNED)
                .build();
        when(homeworkMapper.toResponse(homework)).thenReturn(response);

        HomeworkResponse result = homeworkService.getHomeworkById(50L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getTitle()).isEqualTo("Math Homework #3");
        verify(homeworkRepository).findByIdAndTutorId(50L, tutorId);
    }

    @Test
    @DisplayName("getHomeworkById - not found for tutor")
    void getHomeworkById_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> homeworkService.getHomeworkById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Homework not found with id: 999");
    }

    @Test
    @DisplayName("updateHomeworkStatus - success transition from SUBMITTED to REVIEWED")
    void updateHomeworkStatus_Success() {
        homework.setStatus(HomeworkStatus.SUBMITTED);
        homework.setStudentNotes("Student answer");

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(50L, tutorId)).thenReturn(Optional.of(homework));
        when(homeworkRepository.save(homework)).thenReturn(homework);

        HomeworkResponse response = HomeworkResponse.builder()
                .id(50L)
                .status(HomeworkStatus.REVIEWED)
                .studentNotes("Student answer")
                .build();
        when(homeworkMapper.toResponse(homework)).thenReturn(response);

        HomeworkStatusUpdateRequest request = HomeworkStatusUpdateRequest.builder()
                .status(HomeworkStatus.REVIEWED)
                .build();

        HomeworkResponse result = homeworkService.updateHomeworkStatus(50L, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HomeworkStatus.REVIEWED);
        assertThat(homework.getStatus()).isEqualTo(HomeworkStatus.REVIEWED);
        verify(homeworkRepository).save(homework);
    }

    @Test
    @DisplayName("updateHomeworkStatus - tutor cannot transition to SUBMITTED")
    void updateHomeworkStatus_CannotSetToSubmitted() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(50L, tutorId)).thenReturn(Optional.of(homework));

        HomeworkStatusUpdateRequest request = HomeworkStatusUpdateRequest.builder()
                .status(HomeworkStatus.SUBMITTED)
                .build();

        assertThatThrownBy(() -> homeworkService.updateHomeworkStatus(50L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tutor can only change homework status to REVIEWED");
    }

    @Test
    @DisplayName("updateHomeworkStatus - tutor cannot review unsubmitted ASSIGNED homework")
    void updateHomeworkStatus_CannotReviewUnsubmitted() {
        homework.setStatus(HomeworkStatus.ASSIGNED);

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(50L, tutorId)).thenReturn(Optional.of(homework));

        HomeworkStatusUpdateRequest request = HomeworkStatusUpdateRequest.builder()
                .status(HomeworkStatus.REVIEWED)
                .build();

        assertThatThrownBy(() -> homeworkService.updateHomeworkStatus(50L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot review homework that has not been submitted yet");
    }

    @Test
    @DisplayName("updateHomeworkStatus - tutor cannot review already REVIEWED homework")
    void updateHomeworkStatus_AlreadyReviewed() {
        homework.setStatus(HomeworkStatus.REVIEWED);

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(50L, tutorId)).thenReturn(Optional.of(homework));

        HomeworkStatusUpdateRequest request = HomeworkStatusUpdateRequest.builder()
                .status(HomeworkStatus.REVIEWED)
                .build();

        assertThatThrownBy(() -> homeworkService.updateHomeworkStatus(50L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Homework is already reviewed");
    }
}
