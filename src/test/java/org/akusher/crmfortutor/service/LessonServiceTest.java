package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.LessonUpdateRequest;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.LessonMapper;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private LessonMapper lessonMapper;

    @InjectMocks
    private LessonService lessonService;

    private Long tutorId;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        tutorId = 1L;
        User tutor = User.builder().id(tutorId).email("tutor@example.com").build();
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Ivan").lastName("Petrov").build();

        lesson = Lesson.builder()
                .id(10L)
                .tutor(tutor)
                .student(student)
                .startTime(LocalDateTime.of(2026, 9, 20, 10, 0))
                .endTime(LocalDateTime.of(2026, 9, 20, 11, 0))
                .status(LessonStatus.SCHEDULED)
                .topic("Algebra")
                .meetingUrl("https://meet.google.com/abc")
                .build();
    }

    @Test
    @DisplayName("getLessonById - success")
    void getLessonById_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(lessonRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(lesson));

        LessonResponse response = LessonResponse.builder()
                .id(10L)
                .tutorId(tutorId)
                .studentId(2L)
                .topic("Algebra")
                .status(LessonStatus.SCHEDULED)
                .build();
        when(lessonMapper.toResponse(lesson)).thenReturn(response);

        LessonResponse result = lessonService.getLessonById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTopic()).isEqualTo("Algebra");
        verify(lessonRepository).findByIdAndTutorId(10L, tutorId);
    }

    @Test
    @DisplayName("getLessonById - not found for other tutor or non-existent")
    void getLessonById_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(lessonRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.getLessonById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lesson not found with id: 999");
    }

    @Test
    @DisplayName("updateLesson - success")
    void updateLesson_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(lessonRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newStart = LocalDateTime.of(2026, 9, 21, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 9, 21, 15, 30);
        LessonUpdateRequest request = LessonUpdateRequest.builder()
                .startTime(newStart)
                .endTime(newEnd)
                .topic("Geometry")
                .meetingUrl("https://zoom.us/j/123")
                .build();

        LessonResponse response = LessonResponse.builder()
                .id(10L)
                .startTime(newStart)
                .endTime(newEnd)
                .topic("Geometry")
                .meetingUrl("https://zoom.us/j/123")
                .build();
        when(lessonMapper.toResponse(lesson)).thenReturn(response);

        LessonResponse result = lessonService.updateLesson(10L, request);

        assertThat(result.getTopic()).isEqualTo("Geometry");
        assertThat(lesson.getStartTime()).isEqualTo(newStart);
        assertThat(lesson.getEndTime()).isEqualTo(newEnd);
        assertThat(lesson.getTopic()).isEqualTo("Geometry");
        assertThat(lesson.getMeetingUrl()).isEqualTo("https://zoom.us/j/123");
        verify(lessonRepository).save(lesson);
    }

    @Test
    @DisplayName("updateLesson - invalid time (end before start)")
    void updateLesson_InvalidTime() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);

        LessonUpdateRequest request = LessonUpdateRequest.builder()
                .startTime(LocalDateTime.of(2026, 9, 21, 15, 0))
                .endTime(LocalDateTime.of(2026, 9, 21, 14, 0))
                .topic("Geometry")
                .build();

        assertThatThrownBy(() -> lessonService.updateLesson(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Lesson end time must be after start time");
    }

    @Test
    @DisplayName("updateLesson - invalid time (end equals start)")
    void updateLesson_EqualTime() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        LocalDateTime time = LocalDateTime.of(2026, 9, 21, 15, 0);

        LessonUpdateRequest request = LessonUpdateRequest.builder()
                .startTime(time)
                .endTime(time)
                .topic("Geometry")
                .build();

        assertThatThrownBy(() -> lessonService.updateLesson(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Lesson end time must be after start time");
    }

    @Test
    @DisplayName("updateLesson - not found for tutor")
    void updateLesson_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(lessonRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.empty());

        LessonUpdateRequest request = LessonUpdateRequest.builder()
                .startTime(LocalDateTime.of(2026, 9, 21, 14, 0))
                .endTime(LocalDateTime.of(2026, 9, 21, 15, 0))
                .build();

        assertThatThrownBy(() -> lessonService.updateLesson(10L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lesson not found with id: 10");
    }
}
