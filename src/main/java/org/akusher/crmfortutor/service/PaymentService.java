package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.PaymentCreateRequest;
import org.akusher.crmfortutor.dto.response.IncomeResponse;
import org.akusher.crmfortutor.dto.response.PaymentResponse;
import org.akusher.crmfortutor.entity.Payment;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.PaymentMapper;
import org.akusher.crmfortutor.repository.PaymentRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        StudentProfile student = studentProfileRepository.findByIdAndTutorId(request.getStudentId(), tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        Payment payment = Payment.builder()
                .student(student)
                .amount(request.getAmount())
                .lessonsCount(request.getLessonsCount())
                .paymentDate(request.getPaymentDate())
                .notes(request.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if (request.getLessonsCount() != null && request.getLessonsCount() > 0) {
            int currentBalance = student.getLessonBalance() != null ? student.getLessonBalance() : 0;
            student.setLessonBalance(currentBalance + request.getLessonsCount());
            studentProfileRepository.save(student);
        }

        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStudent(Long studentId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        // Verify student belongs to current tutor
        studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Payment> payments = paymentRepository.findByStudentIdAndTutorId(studentId, tutorId);
        return paymentMapper.toResponseList(payments);
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncome(Integer month, Integer year) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        LocalDate startDate;
        LocalDate endDate;

        if (month != null) {
            if (month < 1 || month > 12) {
                throw new BadRequestException("Month must be between 1 and 12");
            }
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        } else {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        BigDecimal totalIncome = paymentRepository.calculateIncomeBetween(tutorId, startDate, endDate);

        return IncomeResponse.builder()
                .totalIncome(totalIncome)
                .year(year)
                .month(month)
                .build();
    }
}
