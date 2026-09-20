package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.config.TelegramProperties;
import org.akusher.crmfortutor.dto.request.StudentCreateRequest;
import org.akusher.crmfortutor.dto.request.StudentUpdateRequest;
import org.akusher.crmfortutor.dto.response.StudentInviteResponse;
import org.akusher.crmfortutor.dto.response.StudentResponse;
import org.akusher.crmfortutor.dto.response.TelegramLinkCodeResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.StudentStatus;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.StudentMapper;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final StudentMapper studentMapper;
    private final TelegramProperties telegramProperties;

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudents(String search, StudentStatus status, Pageable pageable) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        StudentStatus targetStatus = (status != null) ? status : StudentStatus.ACTIVE;
        Page<StudentProfile> students = studentProfileRepository.findActiveStudents(tutorId, targetStatus, search, pageable);
        return students.map(studentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        StudentProfile student = studentProfileRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return studentMapper.toResponse(student);
    }

    @Transactional
    public StudentResponse createStudent(StudentCreateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));

        StudentProfile student = studentMapper.toEntity(request);
        student.setTutor(tutor);
        if (student.getLessonBalance() == null) {
            student.setLessonBalance(0);
        }

        if (request.getUserId() != null) {
            User studentUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
            student.setUser(studentUser);
        }

        StudentProfile saved = studentProfileRepository.save(student);
        return studentMapper.toResponse(saved);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        StudentProfile student = studentProfileRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        studentMapper.updateEntityFromDto(request, student);

        if (request.getUserId() != null) {
            User studentUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
            student.setUser(studentUser);
        }

        StudentProfile updated = studentProfileRepository.save(student);
        return studentMapper.toResponse(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        StudentProfile student = studentProfileRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        student.setStatus(StudentStatus.ARCHIVED);
        studentProfileRepository.save(student);
    }

    @Transactional
    public StudentInviteResponse createInviteToken(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        StudentProfile student = studentProfileRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (student.getUser() != null) {
            throw new BadRequestException("Student is already linked to a user account");
        }

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        student.setInviteToken(token);
        student.setInviteTokenExpiresAt(expiresAt);
        studentProfileRepository.save(student);

        return StudentInviteResponse.builder()
                .studentId(student.getId())
                .inviteToken(token)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public TelegramLinkCodeResponse generateTelegramLinkCode(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        StudentProfile student = studentProfileRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        String code = generateUniqueTelegramLinkCode();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

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
