package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.Role;
import org.akusher.crmfortutor.entity.StudentProfile;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.AttachmentMapper;
import org.akusher.crmfortutor.repository.AttachmentRepository;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentStudentProvider;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.akusher.crmfortutor.security.UserPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final HomeworkRepository homeworkRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentStudentProvider currentStudentProvider;
    private final AttachmentMapper attachmentMapper;

    @Transactional
    public AttachmentResponse uploadAttachment(Long homeworkId, MultipartFile file) {
        UserPrincipal currentPrincipal = currentUserProvider.getCurrentUser();
        User currentUser = userRepository.findById(currentPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentPrincipal.getId()));

        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + homeworkId));

        checkHomeworkAccess(homework, currentPrincipal);

        String storedFileName = fileStorageService.store(file);

        Attachment attachment = Attachment.builder()
                .homework(homework)
                .fileName(storedFileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedByUser(currentUser)
                .uploadedAt(Instant.now())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Uploaded attachment {} for homework {} by user {}", saved.getId(), homeworkId, currentUser.getId());
        return attachmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long homeworkId) {
        UserPrincipal currentPrincipal = currentUserProvider.getCurrentUser();

        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + homeworkId));

        checkHomeworkAccess(homework, currentPrincipal);

        List<Attachment> attachments = attachmentRepository.findByHomeworkId(homeworkId);
        return attachmentMapper.toResponseList(attachments);
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment downloadAttachment(Long id) {
        UserPrincipal currentPrincipal = currentUserProvider.getCurrentUser();

        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + id));

        checkHomeworkAccess(attachment.getHomework(), currentPrincipal);

        Resource resource = fileStorageService.load(attachment.getFileName());
        return new DownloadedAttachment(
                resource,
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }

    @Transactional
    public void deleteAttachment(Long id) {
        UserPrincipal currentPrincipal = currentUserProvider.getCurrentUser();

        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + id));

        boolean isUploader = attachment.getUploadedByUser().getId().equals(currentPrincipal.getId());
        boolean isTutorOwner = currentPrincipal.getRole() == Role.ROLE_TUTOR
                && attachment.getHomework().getLesson().getTutor().getId().equals(currentPrincipal.getId());

        if (!isUploader && !isTutorOwner) {
            throw new AccessDeniedException("You do not have permission to delete this attachment");
        }

        fileStorageService.delete(attachment.getFileName());
        attachmentRepository.delete(attachment);
        log.info("Deleted attachment {} by user {}", id, currentPrincipal.getId());
    }

    private void checkHomeworkAccess(Homework homework, UserPrincipal currentPrincipal) {
        if (currentPrincipal.getRole() == Role.ROLE_TUTOR) {
            if (!homework.getLesson().getTutor().getId().equals(currentPrincipal.getId())) {
                throw new AccessDeniedException("You do not have access to this homework");
            }
        } else if (currentPrincipal.getRole() == Role.ROLE_STUDENT) {
            StudentProfile currentStudent = currentStudentProvider.getCurrentStudentProfile();
            if (!homework.getLesson().getStudent().getId().equals(currentStudent.getId())) {
                throw new AccessDeniedException("You do not have access to this homework");
            }
        } else {
            throw new AccessDeniedException("Access denied");
        }
    }
}
