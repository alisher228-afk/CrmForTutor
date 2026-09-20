package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.HomeworkCreateRequest;
import org.akusher.crmfortutor.dto.request.HomeworkStatusUpdateRequest;
import org.akusher.crmfortutor.dto.response.HomeworkResponse;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.HomeworkStatus;
import org.akusher.crmfortutor.entity.Lesson;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.HomeworkMapper;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.LessonRepository;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final HomeworkMapper homeworkMapper;

    @Transactional
    public HomeworkResponse createHomework(HomeworkCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Lesson lesson = lessonRepository.findByIdAndTutorId(request.getLessonId(), tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + request.getLessonId()));

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

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getHomeworkByStudent(Long studentId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        // Verify student belongs to this tutor (404 if not)
        studentProfileRepository.findByIdAndTutorId(studentId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Homework> homeworks = homeworkRepository.findByStudentIdAndTutorId(studentId, tutorId);
        return homeworkMapper.toResponseList(homeworks);
    }

    @Transactional(readOnly = true)
    public HomeworkResponse getHomeworkById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Homework homework = homeworkRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + id));

        return homeworkMapper.toResponse(homework);
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
        return homeworkMapper.toResponse(updated);
    }
}
