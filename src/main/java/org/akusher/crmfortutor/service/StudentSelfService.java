package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkSubmitRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.HomeworkStatsResponse;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.dto.response.StudentPaymentsResponse;
import org.akusher.crmfortutor.dto.response.StudentSelfResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.Payment;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.akusher.crmfortutor.dto.request.LessonCancelRequest;
import org.akusher.crmfortutor.dto.response.MaterialResponse;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.entity.TeachingMaterial;
import org.akusher.crmfortutor.mapper.AttachmentMapper;
import org.akusher.crmfortutor.mapper.HomeworkMapper;
import org.akusher.crmfortutor.mapper.LessonMapper;
import org.akusher.crmfortutor.mapper.MaterialMapper;
import org.akusher.crmfortutor.mapper.PaymentMapper;
import org.akusher.crmfortutor.mapper.StudentMapper;
import org.akusher.crmfortutor.repository.AttachmentRepository;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.PaymentRepository;
import org.akusher.crmfortutor.repository.TeachingMaterialRepository;
import org.akusher.crmfortutor.security.CurrentStudentProvider;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.response.TelegramLinkCodeResponse;
import org.akusher.crmfortutor.repository.StudentProfileRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentSelfService {

    private final CurrentStudentProvider currentStudentProvider;
    private final StudentProfileRepository studentProfileRepository;
    private final TelegramProperties telegramProperties;
    private final LessonRepository lessonRepository;
    private final HomeworkRepository homeworkRepository;
    private final PaymentRepository paymentRepository;
    private final StudentMapper studentMapper;
    private final LessonMapper lessonMapper;
    private final HomeworkMapper homeworkMapper;
    private final PaymentMapper paymentMapper;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final TeachingMaterialRepository teachingMaterialRepository;
    private final MaterialMapper materialMapper;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public StudentSelfResponse getProfile() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        return studentMapper.toSelfResponse(student);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("Parameter 'to' must be after or equal to 'from'");
        }

        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Lesson> lessons = lessonRepository.findByStudentIdAndFilters(student.getId(), from, to);
        return lessonMapper.toResponseList(lessons);
    }

    @Transactional(readOnly = true)
    public Page<HomeworkResponse> getHomework(HomeworkStatus status, String search, Pageable pageable) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        Page<Homework> homeworks = homeworkRepository.findByStudentIdWithFilters(student.getId(), status, search, pageable);
        Page<HomeworkResponse> responses = homeworks.map(homeworkMapper::toResponse);
        populateAttachments(responses.getContent());
        return responses;
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomework() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Homework> homeworks = homeworkRepository.findByStudentId(student.getId());
        List<HomeworkResponse> responses = homeworkMapper.toResponseList(homeworks);
        populateAttachments(responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomeworkList(HomeworkStatus status, String search) {
        if (status == null && (search == null || search.isBlank())) {
            return getHomework();
        }
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Homework> homeworks = homeworkRepository.findByStudentIdWithFiltersList(student.getId(), status, search);
        List<HomeworkResponse> responses = homeworkMapper.toResponseList(homeworks);
        populateAttachments(responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public HomeworkStatsResponse getHomeworkStats() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        List<Object[]> counts = homeworkRepository.countByStatusForStudent(student.getId());
        return buildStatsResponse(counts);
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
        HomeworkResponse response = homeworkMapper.toResponse(updated);
        List<AttachmentResponse> attachments = attachmentMapper.toResponseList(attachmentRepository.findByHomeworkId(updated.getId()));
        response.setAttachments(attachments);
        response.setAttachmentsCount(attachments.size());
        return response;
    }

    private void populateAttachments(List<HomeworkResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        List<Long> ids = responses.stream().map(HomeworkResponse::getId).toList();
        List<Attachment> allAttachments = attachmentRepository.findByHomeworkIdIn(ids);
        Map<Long, List<AttachmentResponse>> byHomework = allAttachments.stream()
                .map(attachmentMapper::toResponse)
                .collect(Collectors.groupingBy(AttachmentResponse::getHomeworkId));
        responses.forEach(r -> {
            List<AttachmentResponse> atts = byHomework.getOrDefault(r.getId(), Collections.emptyList());
            r.setAttachments(atts);
            r.setAttachmentsCount(atts.size());
        });
    }

    private HomeworkStatsResponse buildStatsResponse(List<Object[]> counts) {
        long assigned = 0;
        long submitted = 0;
        long reviewed = 0;

        if (counts != null) {
            for (Object[] row : counts) {
                HomeworkStatus status = (HomeworkStatus) row[0];
                long count = ((Number) row[1]).longValue();
                if (status == HomeworkStatus.ASSIGNED) {
                    assigned = count;
                } else if (status == HomeworkStatus.SUBMITTED) {
                    submitted = count;
                } else if (status == HomeworkStatus.REVIEWED) {
                    reviewed = count;
                }
            }
        }

        return HomeworkStatsResponse.builder()
                .totalCount(assigned + submitted + reviewed)
                .assignedCount(assigned)
                .submittedCount(submitted)
                .reviewedCount(reviewed)
                .build();
    }

    @Transactional
    public LessonResponse cancelLesson(Long id, LessonCancelRequest request) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Урок не найден с id: " + id));

        if (!lesson.getStudent().getId().equals(student.getId())) {
            throw new BadRequestException("Этот урок не принадлежит вам");
        }

        if (lesson.getStatus() != LessonStatus.SCHEDULED) {
            throw new BadRequestException("Можно отменить только запланированный урок (текущий статус: " + lesson.getStatus() + ")");
        }

        if (lesson.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Нельзя отменить урок, который уже начался или прошел");
        }

        if (lesson.getStartTime().isBefore(LocalDateTime.now().plusHours(12))) {
            throw new BadRequestException("До занятия осталось менее 12 часов. Поздняя отмена невозможна через систему, свяжитесь с преподавателем напрямую.");
        }

        lesson.setStatus(LessonStatus.CANCELLED_BY_STUDENT);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            lesson.setCancellationReason(request.getReason().trim());
        }

        Lesson updated = lessonRepository.save(lesson);
        return lessonMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterials(String search, String category) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        Long tutorId = student.getTutor().getId();
        String normalizedCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        List<TeachingMaterial> list = teachingMaterialRepository.findByTutorIdAndFilters(
                tutorId,
                normalizedCategory,
                normalizedSearch
        );
        return materialMapper.toResponseList(list);
    }

    @Transactional(readOnly = true)
    public List<String> getMaterialCategories() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        return teachingMaterialRepository.findDistinctCategoriesByTutorId(student.getTutor().getId());
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment downloadMaterial(Long id) {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        Long tutorId = student.getTutor().getId();

        TeachingMaterial material = teachingMaterialRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден с id: " + id));

        Resource resource = fileStorageService.load(material.getFileName());
        return new DownloadedAttachment(
                resource,
                material.getOriginalFileName(),
                material.getContentType(),
                material.getSizeBytes()
        );
    }

    @Transactional
    public TelegramLinkCodeResponse generateTelegramLinkCode() {
        StudentProfile student = currentStudentProvider.getCurrentStudentProfile();
        String code = generateUniqueTelegramLinkCode();
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

        student.setTelegramLinkCode(code);
        student.setTelegramLinkCodeExpiresAt(expiresAt);
        studentProfileRepository.save(student);

        return TelegramLinkCodeResponse.builder()
                .studentId(student.getId())
                .linkCode(code)
                .expiresAt(expiresAt)
                .botUsername(telegramProperties != null ? telegramProperties.getBotUsername() : null)
                .build();
    }

    private String generateUniqueTelegramLinkCode() {
        Random random = new SecureRandom();
        for (int i = 0; i < 100; i++) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            Optional<StudentProfile> existing = studentProfileRepository.findByTelegramLinkCode(code);
            if (existing.isEmpty() || existing.get().getTelegramLinkCodeExpiresAt() == null
                    || existing.get().getTelegramLinkCodeExpiresAt().isBefore(Instant.now())) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique telegram link code");
    }
}
