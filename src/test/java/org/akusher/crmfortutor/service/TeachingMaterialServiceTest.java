package org.akusher.crmfortutor.service;

import org.akusher.crmfortutor.dto.request.MaterialUpdateRequest;
import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.dto.response.MaterialResponse;
import org.akusher.crmfortutor.entity.Attachment;
import org.akusher.crmfortutor.entity.Homework;
import org.akusher.crmfortutor.entity.TeachingMaterial;
import org.akusher.crmfortutor.entity.User;
import org.akusher.crmfortutor.exception.BadRequestException;
import org.akusher.crmfortutor.exception.ResourceNotFoundException;
import org.akusher.crmfortutor.mapper.AttachmentMapper;
import org.akusher.crmfortutor.mapper.MaterialMapper;
import org.akusher.crmfortutor.repository.AttachmentRepository;
import org.akusher.crmfortutor.repository.HomeworkRepository;
import org.akusher.crmfortutor.repository.TeachingMaterialRepository;
import org.akusher.crmfortutor.repository.UserRepository;
import org.akusher.crmfortutor.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeachingMaterialServiceTest {

    @Mock
    private TeachingMaterialRepository teachingMaterialRepository;
    @Mock
    private HomeworkRepository homeworkRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private AttachmentMapper attachmentMapper;

    @InjectMocks
    private TeachingMaterialService teachingMaterialService;

    private Long tutorId;
    private User tutor;
    private TeachingMaterial material;

    @BeforeEach
    void setUp() {
        tutorId = 1L;
        tutor = User.builder().id(tutorId).email("tutor@example.com").build();
        material = TeachingMaterial.builder()
                .id(10L)
                .tutor(tutor)
                .title("Cheat Sheet")
                .category("Math")
                .fileName("stored-uuid.pdf")
                .originalFileName("cheatsheet.pdf")
                .contentType("application/pdf")
                .sizeBytes(2048L)
                .build();
    }

    @Test
    @DisplayName("createMaterial - success")
    void createMaterial_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cheatsheet.pdf", "application/pdf", "dummy content".getBytes());

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(fileStorageService.store(file)).thenReturn("stored-uuid.pdf");
        when(teachingMaterialRepository.save(any(TeachingMaterial.class))).thenReturn(material);

        MaterialResponse response = MaterialResponse.builder()
                .id(10L)
                .title("Cheat Sheet")
                .category("Math")
                .originalFileName("cheatsheet.pdf")
                .build();
        when(materialMapper.toResponse(any(TeachingMaterial.class))).thenReturn(response);

        MaterialResponse result = teachingMaterialService.createMaterial("Cheat Sheet", "Math", "Notes", file);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Cheat Sheet");
        verify(teachingMaterialRepository).save(any(TeachingMaterial.class));
    }

    @Test
    @DisplayName("createMaterial - throws on blank title")
    void createMaterial_BlankTitle_Throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cheatsheet.pdf", "application/pdf", "dummy content".getBytes());

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> teachingMaterialService.createMaterial("", "Math", "Notes", file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Название материала обязательно");
    }

    @Test
    @DisplayName("getMaterials - success")
    void getMaterials_Success() {
        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(teachingMaterialRepository.findByTutorIdAndFilters(tutorId, "Math", "cheat"))
                .thenReturn(List.of(material));

        MaterialResponse response = MaterialResponse.builder().id(10L).title("Cheat Sheet").build();
        when(materialMapper.toResponseList(List.of(material))).thenReturn(List.of(response));

        List<MaterialResponse> list = teachingMaterialService.getMaterials("cheat", "Math");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Cheat Sheet");
    }

    @Test
    @DisplayName("attachMaterialToHomework - success")
    void attachMaterialToHomework_Success() {
        Homework homework = Homework.builder().id(5L).title("Homework #1").build();

        when(currentUserProvider.getCurrentTutorId()).thenReturn(tutorId);
        when(homeworkRepository.findByIdAndTutorId(5L, tutorId)).thenReturn(Optional.of(homework));
        when(teachingMaterialRepository.findByIdAndTutorId(10L, tutorId)).thenReturn(Optional.of(material));
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(fileStorageService.copy("stored-uuid.pdf")).thenReturn("copied-uuid.pdf");

        Attachment savedAttachment = Attachment.builder()
                .id(100L)
                .homework(homework)
                .fileName("copied-uuid.pdf")
                .originalFileName("cheatsheet.pdf")
                .build();
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(savedAttachment);

        AttachmentResponse response = AttachmentResponse.builder()
                .id(100L)
                .fileName("copied-uuid.pdf")
                .originalFileName("cheatsheet.pdf")
                .build();
        when(attachmentMapper.toResponse(savedAttachment)).thenReturn(response);

        AttachmentResponse result = teachingMaterialService.attachMaterialToHomework(5L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getOriginalFileName()).isEqualTo("cheatsheet.pdf");
        verify(fileStorageService).copy("stored-uuid.pdf");
        verify(attachmentRepository).save(any(Attachment.class));
    }
}
