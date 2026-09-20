package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.PaymentUpdateRequest;
import org.akusher.crmfortutor.dto.response.PaymentResponse;
import org.akusher.crmfortutor.entity.Payment;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.PaymentMapper;
import org.akusher.crmfortutor.repository.PaymentRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Long tutorId;
    private StudentProfile student;
    private Payment payment;

    @BeforeEach
    void setUp() {
        tutorId = 1L;
        User tutor = User.builder().id(tutorId).email("tutor@example.com").build();

        student = StudentProfile.builder()
                .id(2L)
                .tutor(tutor)
                .firstName("Anna")
                .lastName("Ivanova")
                .lessonBalance(10)
                .build();

        payment = Payment.builder()
                .id(100L)
                .student(student)
                .amount(BigDecimal.valueOf(5000))
                .lessonsCount(5)
                .paymentDate(LocalDate.of(2026, 9, 1))
                .notes("Initial payment for 5 lessons")
                .build();
    }

    @Test
    @DisplayName("updatePayment - increases lessonsCount, balance adjusted upwards")
    void updatePayment_IncreaseLessonsCount() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(paymentRepository.findByIdAndTutorId(100L, tutorId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .amount(BigDecimal.valueOf(8000))
                .lessonsCount(8) // +3 lessons
                .paymentDate(LocalDate.of(2026, 9, 2))
                .notes("Updated to 8 lessons")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(100L)
                .studentId(2L)
                .amount(BigDecimal.valueOf(8000))
                .lessonsCount(8)
                .paymentDate(LocalDate.of(2026, 9, 2))
                .notes("Updated to 8 lessons")
                .build();
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.updatePayment(100L, request);

        assertThat(result).isNotNull();
        assertThat(result.getLessonsCount()).isEqualTo(8);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(8000));

        // Balance was 10, was +5 earlier, now +8, delta = +3 => new balance 13
        assertThat(student.getLessonBalance()).isEqualTo(13);
        verify(studentProfileRepository).save(student);

        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(8000));
        assertThat(payment.getLessonsCount()).isEqualTo(8);
        assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(payment.getNotes()).isEqualTo("Updated to 8 lessons");
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("updatePayment - decreases lessonsCount, balance adjusted downwards")
    void updatePayment_DecreaseLessonsCount() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(paymentRepository.findByIdAndTutorId(100L, tutorId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .amount(BigDecimal.valueOf(2000))
                .lessonsCount(2) // -3 lessons
                .paymentDate(LocalDate.of(2026, 9, 1))
                .notes("Reduced lessons")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(100L)
                .studentId(2L)
                .amount(BigDecimal.valueOf(2000))
                .lessonsCount(2)
                .build();
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result = paymentService.updatePayment(100L, request);

        // Balance was 10, delta = 2 - 5 = -3 => new balance 7
        assertThat(student.getLessonBalance()).isEqualTo(7);
        verify(studentProfileRepository).save(student);
        assertThat(payment.getLessonsCount()).isEqualTo(2);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("updatePayment - same lessonsCount, balance unchanged")
    void updatePayment_SameLessonsCount() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(paymentRepository.findByIdAndTutorId(100L, tutorId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .amount(BigDecimal.valueOf(5500))
                .lessonsCount(5) // delta = 0
                .paymentDate(LocalDate.of(2026, 9, 1))
                .notes("Price adjusted, lessons count same")
                .build();

        PaymentResponse response = PaymentResponse.builder().id(100L).lessonsCount(5).build();
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        paymentService.updatePayment(100L, request);

        assertThat(student.getLessonBalance()).isEqualTo(10);
        verify(studentProfileRepository, never()).save(student);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("updatePayment - not found for tutor")
    void updatePayment_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(paymentRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        PaymentUpdateRequest request = PaymentUpdateRequest.builder()
                .amount(BigDecimal.valueOf(1000))
                .lessonsCount(1)
                .paymentDate(LocalDate.of(2026, 9, 1))
                .build();

        assertThatThrownBy(() -> paymentService.updatePayment(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found with id: 999");
    }

    @Test
    @DisplayName("deletePayment - deducts lessonsCount from student balance and deletes payment")
    void deletePayment_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(paymentRepository.findByIdAndTutorId(100L, tutorId)).thenReturn(Optional.of(payment));

        paymentService.deletePayment(100L);

        // Balance was 10, payment was for 5 lessons => new balance 5
        assertThat(student.getLessonBalance()).isEqualTo(5);
        verify(studentProfileRepository).save(student);
        verify(paymentRepository).delete(payment);
    }

    @Test
    @DisplayName("deletePayment - not found for tutor")
    void deletePayment_NotFound() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(paymentRepository.findByIdAndTutorId(999L, tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.deletePayment(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found with id: 999");

        verify(paymentRepository, never()).delete(any());
        verify(studentProfileRepository, never()).save(any());
    }
}
