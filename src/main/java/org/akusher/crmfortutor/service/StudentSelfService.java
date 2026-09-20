package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkSubmitRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.dto.response.StudentPaymentsResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.Payment;
import org.akusher.crmfortutor.entity.StudentProfile;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentSelfService {

    private final CurrentStudentProvider currentStudentProvider;
    private final LessonRepository lessonRepository;
    private final HomeworkRepository homeworkRepository;
    private final PaymentRepository paymentRepository;
    private final StudentMapper studentMapper;
    private final LessonMapper lessonMapper;
    private final HomeworkMapper homeworkMapper;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public StudentSelfResponse getProfile() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        return studentMapper.toSelfResponse(student);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new BadRequestException("Parameters 'from' and 'to' are required");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("Parameter 'to' must be after or equal to 'from'");
        }

        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Lesson> lessons = lessonRepository.findByStudentIdAndInterval(student.getId(), from, to);
        return lessonMapper.toResponseList(lessons);
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomework() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Homework> homeworks = homeworkRepository.findByStudentId(student.getId());
        return homeworkMapper.toResponseList(homeworks);
    }

    @Transactional(readOnly = true)
    public StudentPaymentsResponse getPayments() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Payment> payments = paymentRepository.findByStudentId(student.getId());
        return StudentPaymentsResponse.builder()
                .lessonBalance(student.getLessonBalance() != null ? student.getLessonBalance() : 0)
                .payments(paymentMapper.toResponseList(payments))
                .build();
    }

    @Transactional
    public HomeworkResponse submitHomework(Long id, HomeworkSubmitRequest request) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();

        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + id));

        if (!homework.getLesson().getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Homework does not belong to the current student");
        }

        if (homework.getStatus() != HomeworkStatus.ASSIGNED) {
            if (homework.getStatus() == HomeworkStatus.SUBMITTED) {
                throw new BadRequestException("Homework is already submitted");
            }
            if (homework.getStatus() == HomeworkStatus.REVIEWED) {
                throw new BadRequestException("Homework is already reviewed");
            }
            throw new BadRequestException("Only homework in ASSIGNED status can be submitted (current status: " + homework.getStatus() + ")");
        }

        homework.setStatus(HomeworkStatus.SUBMITTED);
        if (request != null && request.getStudentNotes() != null) {
            homework.setStudentNotes(request.getStudentNotes());
        }

        Homework updated = homeworkRepository.save(homework);
        return homeworkMapper.toResponse(updated);
    }
}
