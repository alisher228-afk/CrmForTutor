package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.HomeworkCreateRequest;
import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.HomeworkStatsResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.AttachmentMapper;
import org.akusher.crmfortutor.mapper.HomeworkMapper;
import org.akusher.crmfortutor.repository.AttachmentRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private HomeworkMapper homeworkMapper;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private FileStorageService fileStorageService;

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

    @Test
    @DisplayName("createHomework - success with lessonId")
    void createHomework_WithLessonId_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(lessonRepository.findByIdAndTutorId(5L, tutorId)).thenReturn(Optional.of(homework.getLesson()));
        when(homeworkRepository.save(any(Homework.class))).thenAnswer(inv -> {
            Homework h = inv.getArgument(0);
            h.setId(100L);
            return h;
        });

        HomeworkResponse response = HomeworkResponse.builder().id(100L).title("Test HW").build();
        when(homeworkMapper.toResponse(any(Homework.class))).thenReturn(response);

        HomeworkCreateRequest request = HomeworkCreateRequest.builder()
                .lessonId(5L)
                .title("Test HW")
                .description("Description")
                .build();

        HomeworkResponse result = homeworkService.createHomework(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        verify(homeworkRepository).save(any(Homework.class));
    }

    @Test
    @DisplayName("createHomework - success with studentId when existing lesson is found")
    void createHomework_WithStudentId_ExistingLesson() {
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Oleg").build();
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(2L, tutorId)).thenReturn(Optional.of(student));
        when(lessonRepository.findByTutorIdAndFilters(tutorId, 2L, null, null)).thenReturn(List.of(homework.getLesson()));
        when(homeworkRepository.save(any(Homework.class))).thenAnswer(inv -> {
            Homework h = inv.getArgument(0);
            h.setId(101L);
            return h;
        });

        HomeworkResponse response = HomeworkResponse.builder().id(101L).title("Student HW").build();
        when(homeworkMapper.toResponse(any(Homework.class))).thenReturn(response);

        HomeworkCreateRequest request = HomeworkCreateRequest.builder()
                .studentId(2L)
                .title("Student HW")
                .build();

        HomeworkResponse result = homeworkService.createHomework(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        verify(homeworkRepository).save(any(Homework.class));
    }

    @Test
    @DisplayName("createHomework - success with studentId when no lessons exist (auto-creates lesson)")
    void createHomework_WithStudentId_AutoCreatesLesson() {
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Oleg").build();
        User tutor = User.builder().id(tutorId).build();

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(2L, tutorId)).thenReturn(Optional.of(student));
        when(lessonRepository.findByTutorIdAndFilters(tutorId, 2L, null, null)).thenReturn(List.of());
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            l.setId(999L);
            return l;
        });
        when(homeworkRepository.save(any(Homework.class))).thenAnswer(inv -> {
            Homework h = inv.getArgument(0);
            h.setId(102L);
            return h;
        });

        HomeworkResponse response = HomeworkResponse.builder().id(102L).title("Auto-created HW").build();
        when(homeworkMapper.toResponse(any(Homework.class))).thenReturn(response);

        HomeworkCreateRequest request = HomeworkCreateRequest.builder()
                .studentId(2L)
                .title("Auto-created HW")
                .build();

        HomeworkResponse result = homeworkService.createHomework(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(102L);
        verify(lessonRepository).save(any(Lesson.class));
        verify(homeworkRepository).save(any(Homework.class));
    }

    @Test
    @DisplayName("deleteHomework - success deletes attachments and homework")
    void deleteHomework_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(50L, tutorId)).thenReturn(Optional.of(homework));

        org.akusher.crmfortutor.entity.Attachment att = org.akusher.crmfortutor.entity.Attachment.builder()
                .id(10L)
                .homework(homework)
                .fileName("stored_file.pdf")
                .originalFileName("test.pdf")
                .build();
        when(attachmentRepository.findByHomeworkId(50L)).thenReturn(List.of(att));

        homeworkService.deleteHomework(50L);

        verify(fileStorageService).delete("stored_file.pdf");
        verify(attachmentRepository).deleteAll(List.of(att));
        verify(attachmentRepository).flush();
        verify(homeworkRepository).delete(homework);
        verify(homeworkRepository).flush();
    }

    @Test
    @DisplayName("deleteHomework - throws ResourceNotFoundException when homework not found")
    void deleteHomework_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> homeworkService.deleteHomework(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Homework not found");
    }

    @Test
    @DisplayName("getHomeworkByStudent - paginated with filters")
    void getHomeworkByStudent_Paginated_Success() {
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Oleg").build();
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(2L, tutorId)).thenReturn(Optional.of(student));

        Page<Homework> homeworkPage = new PageImpl<>(List.of(homework));
        Pageable pageable = PageRequest.of(0, 20);
        when(homeworkRepository.findByStudentIdAndTutorIdWithFilters(eq(2L), eq(tutorId), eq(HomeworkStatus.ASSIGNED), eq("Math"), eq(pageable)))
                .thenReturn(homeworkPage);

        HomeworkResponse response = HomeworkResponse.builder().id(50L).title("Math Homework #3").build();
        when(homeworkMapper.toResponse(homework)).thenReturn(response);

        Page<HomeworkResponse> result = homeworkService.getHomeworkByStudent(2L, HomeworkStatus.ASSIGNED, "Math", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Math Homework #3");
        verify(homeworkRepository).findByStudentIdAndTutorIdWithFilters(2L, tutorId, HomeworkStatus.ASSIGNED, "Math", pageable);
    }

    @Test
    @DisplayName("getHomeworkStatsByStudent - success")
    void getHomeworkStatsByStudent_Success() {
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Oleg").build();
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(2L, tutorId)).thenReturn(Optional.of(student));

        List<Object[]> counts = List.of(
                new Object[]{HomeworkStatus.ASSIGNED, 4L},
                new Object[]{HomeworkStatus.SUBMITTED, 1L},
                new Object[]{HomeworkStatus.REVIEWED, 10L}
        );
        when(homeworkRepository.countByStatusForStudentAndTutor(2L, tutorId)).thenReturn(counts);

        HomeworkStatsResponse stats = homeworkService.getHomeworkStatsByStudent(2L);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalCount()).isEqualTo(15L);
        assertThat(stats.getAssignedCount()).isEqualTo(4L);
        assertThat(stats.getSubmittedCount()).isEqualTo(1L);
        assertThat(stats.getReviewedCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getHomeworkListByStudent - returns unpaged list with filters")
    void getHomeworkListByStudent_Success() {
        StudentProfile student = StudentProfile.builder().id(2L).firstName("Oleg").build();
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(studentProfileRepository.findByIdAndTutorId(2L, tutorId)).thenReturn(Optional.of(student));

        List<Homework> homeworkList = List.of(homework);
        when(homeworkRepository.findByStudentIdAndTutorIdWithFiltersList(2L, tutorId, HomeworkStatus.ASSIGNED, "Math"))
                .thenReturn(homeworkList);

        HomeworkResponse response = HomeworkResponse.builder().id(50L).title("Math Homework #3").build();
        when(homeworkMapper.toResponseList(homeworkList)).thenReturn(List.of(response));

        List<HomeworkResponse> result = homeworkService.getHomeworkListByStudent(2L, HomeworkStatus.ASSIGNED, "Math");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Math Homework #3");
        verify(homeworkRepository).findByStudentIdAndTutorIdWithFiltersList(2L, tutorId, HomeworkStatus.ASSIGNED, "Math");
    }
}
