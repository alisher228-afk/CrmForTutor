package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.LessonCreateRequest;
import org.akusher.crmfortutor.dto.request.LessonStatusUpdateRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final LessonMapper lessonMapper;

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(LocalDateTime from, LocalDateTime to) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        if (from == null || to == null) {
            throw new BadRequestException("Parameters 'from' and 'to' are required");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("Parameter 'to' must be after or equal to 'from'");
        }
        List<Lesson> lessons = lessonRepository.findByTutorIdAndInterval(tutorId, from, to);
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
        Long tutorId = currentUserProvider.getCurrentTutorId();

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("Lesson end time must be after start time");
        }

        StudentProfile student = studentProfileRepository.findByIdAndTutorId(request.getStudentId(), tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));

        Lesson lesson = Lesson.builder()
                .tutor(tutor)
                .student(student)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(LessonStatus.SCHEDULED)
                .topic(request.getTopic())
                .meetingUrl(request.getMeetingUrl())
                .build();

        Lesson saved = lessonRepository.save(lesson);
        return lessonMapper.toResponse(saved);
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("Lesson end time must be after start time");
        }

        Lesson lesson = lessonRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));

        lesson.setStartTime(request.getStartTime());
        lesson.setEndTime(request.getEndTime());
        lesson.setTopic(request.getTopic());
        lesson.setMeetingUrl(request.getMeetingUrl());

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

        // Idempotent decrement of student's lesson balance when transitioning to COMPLETED
        if (oldStatus != LessonStatus.COMPLETED && newStatus == LessonStatus.COMPLETED) {
            StudentProfile student = lesson.getStudent();
            int currentBalance = student.getLessonBalance() != null ? student.getLessonBalance() : 0;
            student.setLessonBalance(currentBalance - 1);
            studentProfileRepository.save(student);
        }

        lesson.setStatus(newStatus);
        Lesson updated = lessonRepository.save(lesson);
        return lessonMapper.toResponse(updated);
    }

    @Transactional
    public void deleteLesson(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Lesson lesson = lessonRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));

        lessonRepository.delete(lesson);
    }
}
