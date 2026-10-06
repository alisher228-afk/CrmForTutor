package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.dto.request.LessonCreateRequest;
import org.akusher.crmfortutor.dto.request.LessonStatusUpdateRequest;
import org.akusher.crmfortutor.dto.request.LessonUpdateRequest;
import org.akusher.crmfortutor.dto.response.LessonResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.LessonMapper;
import org.akusher.crmfortutor.repository.AttachmentRepository;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final LessonMapper lessonMapper;
    private final HomeworkRepository homeworkRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(LocalDateTime from, LocalDateTime to) {
        return getLessons(from, to, null);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(LocalDateTime from, LocalDateTime to, Long studentId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("Parameter 'to' must be after or equal to 'from'");
        }
        List<Lesson> lessons = lessonRepository.findByTutorIdAndFilters(tutorId, studentId, from, to);
        return lessonMapper.toResponseList(lessons);
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        Lesson lesson = lessonRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));
        return lessonMapper.toResponse(lesson);
    }

    @Transactional
    public LessonResponse createLesson(LessonCreateRequest request) {
        List<LessonResponse> created = createLessonsInternal(request);
        return created.get(0);
    }

    @Transactional
    public List<LessonResponse> createGroupLesson(LessonCreateRequest request) {
        if (request.getGroupName() == null || request.getGroupName().isBlank()) {
            throw new BadRequestException("Название группы обязательно для группового занятия");
        }
        return createLessonsInternal(request);
    }

    private List<LessonResponse> createLessonsInternal(LessonCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Время начала и окончания урока обязательно");
        }

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("Lesson end time must be after start time");
        }

        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));

        // Case 1: Individual lesson (studentId is provided)
        if (request.getStudentId() != null) {
            StudentProfile student = studentProfileRepository.findByIdAndTutorId(request.getStudentId(), tutorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

            if (lessonRepository.existsConflictingLesson(student.getId(), request.getStartTime(), request.getEndTime(), null)) {
                throw new BadRequestException("На это время у ученика уже запланирован урок");
            }

            Lesson lesson = Lesson.builder()
                    .tutor(tutor)
                    .student(student)
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .status(LessonStatus.SCHEDULED)
                    .topic(request.getTopic())
                    .meetingUrl(request.getMeetingUrl())
                    .groupName(request.getGroupName() != null && !request.getGroupName().isBlank() ? request.getGroupName().trim() : student.getGroupName())
                    .build();

            Lesson saved = lessonRepository.save(lesson);
            try {
                notificationService.sendLessonCreateNotification(saved);
            } catch (Exception e) {
                log.warn("Failed to send lesson creation notification: {}", e.getMessage());
            }
            return List.of(lessonMapper.toResponse(saved));
        }

        // Case 2: Group lesson (groupName is provided, studentId is null)
        if (request.getGroupName() != null && !request.getGroupName().isBlank()) {
            String grp = request.getGroupName().trim();
            List<StudentProfile> students = studentProfileRepository.findActiveByTutorIdAndGroupName(tutorId, grp);
            if (students.isEmpty()) {
                throw new BadRequestException("В группе '" + grp + "' не найдено активных учеников");
            }

            List<Lesson> lessonsToSave = new ArrayList<>();
            for (StudentProfile student : students) {
                if (lessonRepository.existsConflictingLesson(student.getId(), request.getStartTime(), request.getEndTime(), null)) {
                    String studentName = student.getFirstName() + (student.getLastName() != null ? " " + student.getLastName() : "");
                    throw new BadRequestException("На это время у ученика " + studentName + " уже запланирован урок");
                }

                Lesson lesson = Lesson.builder()
                        .tutor(tutor)
                        .student(student)
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .status(LessonStatus.SCHEDULED)
                        .topic(request.getTopic())
                        .meetingUrl(request.getMeetingUrl())
                        .groupName(grp)
                        .build();

                lessonsToSave.add(lesson);
            }

            List<Lesson> savedLessons = lessonRepository.saveAll(lessonsToSave);
            for (Lesson sl : savedLessons) {
                try {
                    notificationService.sendLessonCreateNotification(sl);
                } catch (Exception e) {
                    log.warn("Failed to send lesson creation notification: {}", e.getMessage());
                }
            }
            return lessonMapper.toResponseList(savedLessons);
        }

        throw new BadRequestException("Необходимо указать ученика (studentId) или название группы (groupName)");
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("Lesson end time must be after start time");
        }

        Lesson lesson = lessonRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));

        if (lessonRepository.existsConflictingLesson(lesson.getStudent().getId(), request.getStartTime(), request.getEndTime(), lesson.getId())) {
            throw new BadRequestException("На это время у ученика уже запланирован урок");
        }

        lesson.setStartTime(request.getStartTime());
        lesson.setEndTime(request.getEndTime());
        lesson.setTopic(request.getTopic());
        lesson.setMeetingUrl(request.getMeetingUrl());
        if (request.getGroupName() != null) {
            lesson.setGroupName(request.getGroupName());
        }

        Lesson updated = lessonRepository.save(lesson);
        return lessonMapper.toResponse(updated);
    }

    @Transactional
    public LessonResponse updateLessonStatus(Long id, LessonStatusUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Lesson lesson = lessonRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));

        LessonStatus oldStatus = lesson.getStatus();
        LessonStatus newStatus = request.getStatus();

        adjustBalanceOnStatusChange(oldStatus, newStatus, lesson.getStudent());

        lesson.setStatus(newStatus);
        Lesson updated = lessonRepository.save(lesson);
        return lessonMapper.toResponse(updated);
    }

    @Transactional
    public void deleteLesson(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Lesson lesson = lessonRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));

        // Delete all associated homework and their attachments before deleting the lesson
        List<Homework> homeworkList = homeworkRepository.findByLessonId(lesson.getId());
        for (Homework hw : homeworkList) {
            List<Attachment> attachments = attachmentRepository.findByHomeworkId(hw.getId());
            for (Attachment attachment : attachments) {
                try {
                    fileStorageService.delete(attachment.getFileName());
                } catch (Exception e) {
                    log.warn("Failed to delete physical file {} for attachment {}: {}",
                            attachment.getFileName(), attachment.getId(), e.getMessage());
                }
            }
            if (!attachments.isEmpty()) {
                attachmentRepository.deleteAll(attachments);
                attachmentRepository.flush();
            }
            homeworkRepository.delete(hw);
            homeworkRepository.flush();
        }

        adjustBalanceOnStatusChange(lesson.getStatus(), null, lesson.getStudent());

        lessonRepository.delete(lesson);
        lessonRepository.flush();
        log.info("Deleted lesson {} and {} associated homework by tutor {}", id, homeworkList.size(), tutorId);
    }

    private void adjustBalanceOnStatusChange(LessonStatus oldStatus, LessonStatus newStatus, StudentProfile student) {
        if (student == null) {
            return;
        }
        if (oldStatus != LessonStatus.COMPLETED && newStatus == LessonStatus.COMPLETED) {
            int currentBalance = student.getLessonBalance() != null ? student.getLessonBalance() : 0;
            student.setLessonBalance(currentBalance - 1);
            studentProfileRepository.save(student);
        } else if (oldStatus == LessonStatus.COMPLETED && newStatus != LessonStatus.COMPLETED) {
            int currentBalance = student.getLessonBalance() != null ? student.getLessonBalance() : 0;
            student.setLessonBalance(currentBalance + 1);
            studentProfileRepository.save(student);
        }
    }
}
