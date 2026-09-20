package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.HomeworkSubmitRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.dto.response.PaymentResponse;
import org.akusher.crmfortutor.dto.response.StudentPaymentsResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.Payment;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.HomeworkMapper;
import org.akusher.crmfortutor.mapper.LessonMapper;
import org.akusher.crmfortutor.mapper.PaymentMapper;
import org.akusher.crmfortutor.mapper.StudentMapper;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.PaymentRepository;
import org.akusher.crmfortutor.security.CurrentStudentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentSelfServiceTest {

    @Mock
    private CurrentStudentProvider currentStudentProvider;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private HomeworkRepository homeworkRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private LessonMapper lessonMapper;
    @Mock
    private HomeworkMapper homeworkMapper;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private StudentSelfService studentSelfService;

    private StudentProfile student;

    @BeforeEach
    void setUp() {
        User tutor = User.builder().id(1L).email("tutor@example.com").build();
        User studentUser = User.builder().id(2L).email("student@example.com").build();

        student = StudentProfile.builder()
                .id(10L)
                .user(studentUser)
                .tutor(tutor)
                .firstName("Alex")
                .lastName("Smirnov")
                .lessonBalance(4)
                .notes("Secret tutor notes about student")
                .build();
    }

    @Test
    @DisplayName("getProfile - returns StudentSelfResponse without notes")
    void getProfile_Success() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        StudentSelfResponse response = StudentSelfResponse.builder()
                .id(10L)
                .userId(2L)
                .tutorId(1L)
                .firstName("Alex")
                .lastName("Smirnov")
                .lessonBalance(4)
                .build();
        when(studentMapper.toSelfResponse(student)).thenReturn(response);

        StudentSelfResponse result = studentSelfService.getProfile();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getFirstName()).isEqualTo("Alex");
        assertThat(result.getLessonBalance()).isEqualTo(4);
        verify(currentStudentProvider).getCurrentStudentProfile();
        verify(studentMapper).toSelfResponse(student);
    }

    @Test
    @DisplayName("getLessons - filters strictly by student ID and interval")
    void getLessons_Success() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 30, 23, 59);

        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        Lesson lesson = Lesson.builder().id(100L).topic("Chemistry").build();
        List<Lesson> lessons = List.of(lesson);
        when(lessonRepository.findByStudentIdAndInterval(10L, from, to)).thenReturn(lessons);

        LessonResponse lessonResponse = LessonResponse.builder().id(100L).topic("Chemistry").build();
        when(lessonMapper.toResponseList(lessons)).thenReturn(List.of(lessonResponse));

        List<LessonResponse> result = studentSelfService.getLessons(from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTopic()).isEqualTo("Chemistry");
        verify(lessonRepository).findByStudentIdAndInterval(10L, from, to);
    }

    @Test
    @DisplayName("getLessons - throws BadRequestException when from or to is null or to < from")
    void getLessons_InvalidInterval() {
        LocalDateTime from = LocalDateTime.of(2026, 9, 10, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 5, 0, 0);

        assertThatThrownBy(() -> studentSelfService.getLessons(null, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Parameters 'from' and 'to' are required");

        assertThatThrownBy(() -> studentSelfService.getLessons(from, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Parameters 'from' and 'to' are required");

        assertThatThrownBy(() -> studentSelfService.getLessons(from, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Parameter 'to' must be after or equal to 'from'");
    }

    @Test
    @DisplayName("getHomework - filters strictly by student ID")
    void getHomework_Success() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        Homework homework = Homework.builder().id(200L).title("Lab Work #1").status(HomeworkStatus.ASSIGNED).build();
        List<Homework> homeworkList = List.of(homework);
        when(homeworkRepository.findByStudentId(10L)).thenReturn(homeworkList);

        HomeworkResponse homeworkResponse = HomeworkResponse.builder().id(200L).title("Lab Work #1").status(HomeworkStatus.ASSIGNED).build();
        when(homeworkMapper.toResponseList(homeworkList)).thenReturn(List.of(homeworkResponse));

        List<HomeworkResponse> result = studentSelfService.getHomework();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Lab Work #1");
        verify(homeworkRepository).findByStudentId(10L);
    }

    @Test
    @DisplayName("getPayments - returns student payments history and lessonBalance")
    void getPayments_Success() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        Payment payment = Payment.builder().id(300L).amount(BigDecimal.valueOf(4000)).lessonsCount(4).paymentDate(LocalDate.of(2026, 9, 15)).build();
        List<Payment> paymentList = List.of(payment);
        when(paymentRepository.findByStudentId(10L)).thenReturn(paymentList);

        PaymentResponse paymentResponse = PaymentResponse.builder().id(300L).amount(BigDecimal.valueOf(4000)).lessonsCount(4).paymentDate(LocalDate.of(2026, 9, 15)).build();
        when(paymentMapper.toResponseList(paymentList)).thenReturn(List.of(paymentResponse));

        StudentPaymentsResponse result = studentSelfService.getPayments();

        assertThat(result).isNotNull();
        assertThat(result.getLessonBalance()).isEqualTo(4);
        assertThat(result.getPayments()).hasSize(1);
        assertThat(result.getPayments().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        verify(paymentRepository).findByStudentId(10L);
    }

    @Test
    @DisplayName("submitHomework - success transitions ASSIGNED to SUBMITTED and sets studentNotes")
    void submitHomework_Success() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        Lesson lesson = Lesson.builder().id(50L).student(student).build();
        Homework homework = Homework.builder()
                .id(200L)
                .lesson(lesson)
                .title("Essay")
                .status(HomeworkStatus.ASSIGNED)
                .build();
        when(homeworkRepository.findById(200L)).thenReturn(Optional.of(homework));
        when(homeworkRepository.save(homework)).thenReturn(homework);

        HomeworkResponse response = HomeworkResponse.builder()
                .id(200L)
                .title("Essay")
                .status(HomeworkStatus.SUBMITTED)
                .studentNotes("Here is my solution: https://example.com")
                .build();
        when(homeworkMapper.toResponse(homework)).thenReturn(response);

        HomeworkSubmitRequest request = HomeworkSubmitRequest.builder()
                .studentNotes("Here is my solution: https://example.com")
                .build();

        HomeworkResponse result = studentSelfService.submitHomework(200L, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HomeworkStatus.SUBMITTED);
        assertThat(result.getStudentNotes()).isEqualTo("Here is my solution: https://example.com");
        assertThat(homework.getStatus()).isEqualTo(HomeworkStatus.SUBMITTED);
        assertThat(homework.getStudentNotes()).isEqualTo("Here is my solution: https://example.com");
        verify(homeworkRepository).save(homework);
    }

    @Test
    @DisplayName("submitHomework - not found throws ResourceNotFoundException")
    void submitHomework_NotFound() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);
        when(homeworkRepository.findById(999L)).thenReturn(Optional.empty());

        HomeworkSubmitRequest request = new HomeworkSubmitRequest("solution");

        assertThatThrownBy(() -> studentSelfService.submitHomework(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Homework not found with id: 999");
    }

    @Test
    @DisplayName("submitHomework - homework of another student throws BadRequestException")
    void submitHomework_BelongsToAnotherStudent() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        StudentProfile anotherStudent = StudentProfile.builder().id(999L).build();
        Lesson lesson = Lesson.builder().id(50L).student(anotherStudent).build();
        Homework homework = Homework.builder()
                .id(200L)
                .lesson(lesson)
                .status(HomeworkStatus.ASSIGNED)
                .build();
        when(homeworkRepository.findById(200L)).thenReturn(Optional.of(homework));

        HomeworkSubmitRequest request = new HomeworkSubmitRequest("solution");

        assertThatThrownBy(() -> studentSelfService.submitHomework(200L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Homework does not belong to the current student");
    }

    @Test
    @DisplayName("submitHomework - already SUBMITTED throws BadRequestException")
    void submitHomework_AlreadySubmitted() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        Lesson lesson = Lesson.builder().id(50L).student(student).build();
        Homework homework = Homework.builder()
                .id(200L)
                .lesson(lesson)
                .status(HomeworkStatus.SUBMITTED)
                .build();
        when(homeworkRepository.findById(200L)).thenReturn(Optional.of(homework));

        HomeworkSubmitRequest request = new HomeworkSubmitRequest("another solution");

        assertThatThrownBy(() -> studentSelfService.submitHomework(200L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Homework is already submitted");
    }

    @Test
    @DisplayName("submitHomework - already REVIEWED throws BadRequestException")
    void submitHomework_AlreadyReviewed() {
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(student);

        Lesson lesson = Lesson.builder().id(50L).student(student).build();
        Homework homework = Homework.builder()
                .id(200L)
                .lesson(lesson)
                .status(HomeworkStatus.REVIEWED)
                .build();
        when(homeworkRepository.findById(200L)).thenReturn(Optional.of(homework));

        HomeworkSubmitRequest request = new HomeworkSubmitRequest("another solution");

        assertThatThrownBy(() -> studentSelfService.submitHomework(200L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Homework is already reviewed");
    }
}
