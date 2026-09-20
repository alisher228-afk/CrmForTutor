package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.Lesson;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private HomeworkRepository homeworkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private CurrentStudentProvider currentStudentProvider;
    @Mock
    private AttachmentMapper attachmentMapper;

    @InjectMocks
    private AttachmentService attachmentService;

    private User tutorUser;
    private User studentUser;
    private StudentProfile studentProfile;
    private Lesson lesson;
    private Homework homework;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        tutorUser = User.builder().id(1L).email("tutor@example.com").role(Role.ROLE_TUTOR).build();
        studentUser = User.builder().id(2L).email("student@example.com").role(Role.ROLE_STUDENT).build();

        studentProfile = StudentProfile.builder().id(10L).user(studentUser).tutor(tutorUser).build();
        lesson = Lesson.builder().id(20L).tutor(tutorUser).student(studentProfile).build();
        homework = Homework.builder().id(30L).lesson(lesson).title("Physics Task").build();

        attachment = Attachment.builder()
                .id(100L)
                .homework(homework)
                .fileName("uuid-file.pdf")
                .originalFileName("homework.pdf")
                .contentType("application/pdf")
                .sizeBytes(1024L)
                .uploadedByUser(studentUser)
                .uploadedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("uploadAttachment - tutor owner can upload successfully")
    void uploadAttachment_Tutor_Success() {
        UserPrincipal tutorPrincipal = UserPrincipal.builder().id(1L).role(Role.ROLE_TUTOR).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(tutorPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tutorUser));
        when(homeworkRepository.findById(30L)).thenReturn(Optional.of(homework));
        when(fileStorageService.store(any())).thenReturn("uuid-tutor-file.pdf");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);

        AttachmentResponse response = AttachmentResponse.builder().id(100L).fileName("uuid-tutor-file.pdf").build();
        when(attachmentMapper.toResponse(any(Attachment.class))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        AttachmentResponse result = attachmentService.uploadAttachment(30L, file);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        verify(fileStorageService).store(file);
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    @DisplayName("uploadAttachment - student owner can upload successfully")
    void uploadAttachment_Student_Success() {
        UserPrincipal studentPrincipal = UserPrincipal.builder().id(2L).role(Role.ROLE_STUDENT).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(studentPrincipal);
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));
        when(homeworkRepository.findById(30L)).thenReturn(Optional.of(homework));
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(studentProfile);
        when(fileStorageService.store(any())).thenReturn("uuid-student-file.pdf");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);

        AttachmentResponse response = AttachmentResponse.builder().id(100L).fileName("uuid-student-file.pdf").build();
        when(attachmentMapper.toResponse(any(Attachment.class))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "student.pdf", "application/pdf", "data".getBytes());

        AttachmentResponse result = attachmentService.uploadAttachment(30L, file);

        assertThat(result).isNotNull();
        verify(fileStorageService).store(file);
    }

    @Test
    @DisplayName("uploadAttachment - unauthorized tutor throws AccessDeniedException")
    void uploadAttachment_UnauthorizedTutor() {
        UserPrincipal otherTutorPrincipal = UserPrincipal.builder().id(99L).role(Role.ROLE_TUTOR).build();
        User otherTutor = User.builder().id(99L).role(Role.ROLE_TUTOR).build();

        when(currentUserProvider.getCurrentUser()).thenReturn(otherTutorPrincipal);
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherTutor));
        when(homeworkRepository.findById(30L)).thenReturn(Optional.of(homework));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(30L, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have access to this homework");
    }

    @Test
    @DisplayName("getAttachments - success for homework student owner")
    void getAttachments_Success() {
        UserPrincipal studentPrincipal = UserPrincipal.builder().id(2L).role(Role.ROLE_STUDENT).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(studentPrincipal);
        when(homeworkRepository.findById(30L)).thenReturn(Optional.of(homework));
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(studentProfile);
        when(attachmentRepository.findByHomeworkId(30L)).thenReturn(List.of(attachment));

        AttachmentResponse response = AttachmentResponse.builder().id(100L).originalFileName("homework.pdf").build();
        when(attachmentMapper.toResponseList(List.of(attachment))).thenReturn(List.of(response));

        List<AttachmentResponse> result = attachmentService.getAttachments(30L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalFileName()).isEqualTo("homework.pdf");
    }

    @Test
    @DisplayName("downloadAttachment - success returns DownloadedAttachment")
    void downloadAttachment_Success() {
        UserPrincipal studentPrincipal = UserPrincipal.builder().id(2L).role(Role.ROLE_STUDENT).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(studentPrincipal);
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));
        when(currentStudentProvider.getCurrentStudentProfile()).thenReturn(studentProfile);

        Resource resource = new ByteArrayResource("file content".getBytes());
        when(fileStorageService.load("uuid-file.pdf")).thenReturn(resource);

        DownloadedAttachment result = attachmentService.downloadAttachment(100L);

        assertThat(result).isNotNull();
        assertThat(result.originalFileName()).isEqualTo("homework.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.resource()).isEqualTo(resource);
    }

    @Test
    @DisplayName("deleteAttachment - allowed for uploader student")
    void deleteAttachment_ByUploaderStudent_Success() {
        UserPrincipal studentPrincipal = UserPrincipal.builder().id(2L).role(Role.ROLE_STUDENT).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(studentPrincipal);
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));

        attachmentService.deleteAttachment(100L);

        verify(fileStorageService).delete("uuid-file.pdf");
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    @DisplayName("deleteAttachment - allowed for tutor owner even if uploaded by student")
    void deleteAttachment_ByTutorOwner_Success() {
        UserPrincipal tutorPrincipal = UserPrincipal.builder().id(1L).role(Role.ROLE_TUTOR).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(tutorPrincipal);
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));

        attachmentService.deleteAttachment(100L);

        verify(fileStorageService).delete("uuid-file.pdf");
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    @DisplayName("deleteAttachment - denied for another student")
    void deleteAttachment_ByAnotherStudent_Denied() {
        UserPrincipal anotherStudent = UserPrincipal.builder().id(999L).role(Role.ROLE_STUDENT).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherStudent);
        when(attachmentRepository.findById(100L)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> attachmentService.deleteAttachment(100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to delete this attachment");
    }

    @Test
    @DisplayName("deleteAttachment - not found throws ResourceNotFoundException")
    void deleteAttachment_NotFound() {
        UserPrincipal tutorPrincipal = UserPrincipal.builder().id(1L).role(Role.ROLE_TUTOR).build();
        when(currentUserProvider.getCurrentUser()).thenReturn(tutorPrincipal);
        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.deleteAttachment(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Attachment not found with id: 999");
    }
}
