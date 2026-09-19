package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.StudentCreateRequest;
import org.akusher.crmfortutor.dto.request.StudentUpdateRequest;
import org.akusher.crmfortutor.dto.response.StudentResponse;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.StudentStatus;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.StudentMapper;
import org.akusher.crmfortutor.repository.StudentProfileRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final StudentMapper studentMapper;

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
        } else {
            student.setUser(null);
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
}
