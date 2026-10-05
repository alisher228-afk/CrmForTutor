package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkCreateRequest;
import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.dto.response.HomeworkStatsResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.entity.LessonStatus;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.akusher.crmfortutor.mapper.AttachmentMapper;
import org.akusher.crmfortutor.mapper.HomeworkMapper;
import org.akusher.crmfortutor.repository.AttachmentRepository;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final HomeworkMapper homeworkMapper;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public HomeworkResponse createHomework(HomeworkCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Lesson lesson;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findByIdAndTutorId(request.getLessonId(), tutorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + request.getLessonId()));
        } else if (request.getStudentId() != null) {
            StudentProfile student = studentProfileRepository.findByIdAndTutorId(request.getStudentId(), tutorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

            List<Lesson> studentLessons = lessonRepository.findByTutorIdAndFilters(tutorId, student.getId(), null, null);
            if (!studentLessons.isEmpty()) {
                lesson = studentLessons.get(studentLessons.size() - 1);
            } else {
                User tutor = userRepository.findById(tutorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                lesson = Lesson.builder()
                        .tutor(tutor)
                        .student(student)
                        .startTime(now)
                        .endTime(now.plusHours(1))
                        .status(LessonStatus.SCHEDULED)
                        .topic(request.getTitle())
                        .groupName(student.getGroupName())
                        .build();
                lesson = lessonRepository.save(lesson);
            }
        } else if (request.getGroupName() != null && !request.getGroupName().isBlank()) {
            List<HomeworkResponse> groupHw = createGroupHomework(request);
            return groupHw.get(0);
        } else {
            throw new BadRequestException("Необходимо указать ID урока (lessonId) или ID ученика (studentId)");
        }

        Homework homework = Homework.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .status(HomeworkStatus.ASSIGNED)
                .build();

        Homework saved = homeworkRepository.save(homework);
        return homeworkMapper.toResponse(saved);
    }

    @Transactional
    public List<HomeworkResponse> createGroupHomework(HomeworkCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        if (request.getGroupName() == null || request.getGroupName().isBlank()) {
            throw new BadRequestException("Название группы обязательно для группового домашнего задания");
        }

        String grp = request.getGroupName().trim();
        List<StudentProfile> students = studentProfileRepository.findActiveByTutorIdAndGroupName(tutorId, grp);
        if (students.isEmpty()) {
            throw new BadRequestException("В группе '" + grp + "' не найдено активных учеников");
        }

        List<HomeworkResponse> responses = new ArrayList<>();
        for (StudentProfile student : students) {
            HomeworkCreateRequest singleRequest = HomeworkCreateRequest.builder()
                    .studentId(student.getId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .deadline(request.getDeadline())
                    .build();
            responses.add(createHomework(singleRequest));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public Page<HomeworkResponse> getHomeworkByStudent(Long studentId, HomeworkStatus status, String search, Pageable pageable) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        // Verify student belongs to this tutor (404 if not)
        studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Page<Homework> homeworks = homeworkRepository.findByStudentIdAndTutorIdWithFilters(studentId, tutorId, status, search, pageable);
        Page<HomeworkResponse> responses = homeworks.map(homeworkMapper::toResponse);
        populateAttachments(responses.getContent());
        return responses;
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomeworkByStudent(Long studentId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        // Verify student belongs to this tutor (404 if not)
        studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Homework> homeworks = homeworkRepository.findByStudentIdAndTutorId(studentId, tutorId);
        List<HomeworkResponse> responses = homeworkMapper.toResponseList(homeworks);
        populateAttachments(responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomeworkListByStudent(Long studentId, HomeworkStatus status, String search) {
        if (status == null && (search == null || search.isBlank())) {
            return getHomeworkByStudent(studentId);
        }
        Long tutorId = currentUserProvider.getCurrentTutorId();

        // Verify student belongs to this tutor (404 if not)
        studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Homework> homeworks = homeworkRepository.findByStudentIdAndTutorIdWithFiltersList(studentId, tutorId, status, search);
        List<HomeworkResponse> responses = homeworkMapper.toResponseList(homeworks);
        populateAttachments(responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public HomeworkStatsResponse getHomeworkStatsByStudent(Long studentId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        // Verify student belongs to this tutor (404 if not)
        studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Object[]> counts = homeworkRepository.countByStatusForStudentAndTutor(studentId, tutorId);
        return buildStatsResponse(counts);
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomeworkByGroup(String groupName) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        if (groupName == null || groupName.isBlank()) {
            throw new BadRequestException("Название группы обязательно");
        }
        List<Homework> homeworks = homeworkRepository.findByGroupNameAndTutorId(groupName.trim(), tutorId);
        List<HomeworkResponse> responses = homeworkMapper.toResponseList(homeworks);
        populateAttachments(responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public HomeworkResponse getHomeworkById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Homework homework = homeworkRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + id));

        HomeworkResponse response = homeworkMapper.toResponse(homework);
        List<AttachmentResponse> attachments = attachmentMapper.toResponseList(attachmentRepository.findByHomeworkId(homework.getId()));
        response.setAttachments(attachments);
        response.setAttachmentsCount(attachments.size());
        return response;
    }

    @Transactional
    public HomeworkResponse updateHomeworkStatus(Long id, HomeworkStatusUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Homework homework = homeworkRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + id));

        if (request.getStatus() != HomeworkStatus.REVIEWED) {
            throw new BadRequestException("Tutor can only change homework status to REVIEWED");
        }

        if (homework.getStatus() != HomeworkStatus.SUBMITTED) {
            if (homework.getStatus() == HomeworkStatus.ASSIGNED) {
                throw new BadRequestException("Cannot review homework that has not been submitted yet");
            }
            if (homework.getStatus() == HomeworkStatus.REVIEWED) {
                throw new BadRequestException("Homework is already reviewed");
            }
            throw new BadRequestException("Cannot transition homework from status " + homework.getStatus() + " to REVIEWED");
        }

        homework.setStatus(HomeworkStatus.REVIEWED);
        if (request.getStudentNotes() != null) {
            homework.setStudentNotes(request.getStudentNotes());
        }

        Homework updated = homeworkRepository.save(homework);
        HomeworkResponse response = homeworkMapper.toResponse(updated);
        List<AttachmentResponse> attachments = attachmentMapper.toResponseList(attachmentRepository.findByHomeworkId(updated.getId()));
        response.setAttachments(attachments);
        response.setAttachmentsCount(attachments.size());
        return response;
    }

    @Transactional
    public void deleteHomework(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Homework homework = homeworkRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + id));

        List<Attachment> attachments = attachmentRepository.findByHomeworkId(homework.getId());
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

        homeworkRepository.delete(homework);
        homeworkRepository.flush();
        log.info("Deleted homework {} and {} attachments by tutor {}", id, attachments.size(), tutorId);
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
}
