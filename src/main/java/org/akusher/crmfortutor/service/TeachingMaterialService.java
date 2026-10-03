package org.akusher.crmfortutor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingMaterialService {

    private final TeachingMaterialRepository teachingMaterialRepository;
    private final HomeworkRepository homeworkRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserProvider currentUserProvider;
    private final MaterialMapper materialMapper;
    private final AttachmentMapper attachmentMapper;

    @Transactional
    public MaterialResponse createMaterial(
            String title,
            String category,
            String description,
            MultipartFile file) {

        Long tutorId = currentUserProvider.getCurrentTutorId();
        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));

        if (title == null || title.isBlank()) {
            throw new BadRequestException("Название материала обязательно");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл для материала обязателен");
        }

        String storedFileName = fileStorageService.store(file);

        String rawOriginalName = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : title.trim();
        String originalName = rawOriginalName.length() > 255 ? rawOriginalName.substring(0, 255) : rawOriginalName;

        String rawContentType = file.getContentType();
        String safeContentType = rawContentType != null && rawContentType.length() > 100
                ? rawContentType.substring(0, 100)
                : rawContentType;

        TeachingMaterial material = TeachingMaterial.builder()
                .tutor(tutor)
                .title(title.trim().length() > 255 ? title.trim().substring(0, 255) : title.trim())
                .category(category != null && !category.isBlank() ? category.trim() : null)
                .description(description != null && !description.isBlank() ? description.trim() : null)
                .fileName(storedFileName)
                .originalFileName(originalName)
                .contentType(safeContentType)
                .sizeBytes(file.getSize())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TeachingMaterial saved = teachingMaterialRepository.save(material);
        log.info("Created teaching material {} ('{}') for tutor {}", saved.getId(), saved.getTitle(), tutorId);
        return materialMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterials(String search, String category) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
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
    public List<String> getCategories() {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        return teachingMaterialRepository.findDistinctCategoriesByTutorId(tutorId);
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TeachingMaterial material = teachingMaterialRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден с id: " + id));
        return materialMapper.toResponse(material);
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment downloadMaterial(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
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
    public MaterialResponse updateMaterial(Long id, MaterialUpdateRequest request) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TeachingMaterial material = teachingMaterialRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден с id: " + id));

        materialMapper.updateEntityFromDto(request, material);
        if (request.getCategory() != null) {
            material.setCategory(request.getCategory().isBlank() ? null : request.getCategory().trim());
        }

        TeachingMaterial updated = teachingMaterialRepository.save(material);
        log.info("Updated teaching material {} for tutor {}", updated.getId(), tutorId);
        return materialMapper.toResponse(updated);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        Long tutorId = currentUserProvider.getCurrentTutorId();
        TeachingMaterial material = teachingMaterialRepository.findByIdAndTutorId(id, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден с id: " + id));

        fileStorageService.delete(material.getFileName());
        teachingMaterialRepository.delete(material);
        log.info("Deleted teaching material {} by tutor {}", id, tutorId);
    }

    @Transactional
    public AttachmentResponse attachMaterialToHomework(Long homeworkId, Long materialId) {
        Long tutorId = currentUserProvider.getCurrentTutorId();

        Homework homework = homeworkRepository.findByIdAndTutorId(homeworkId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + homeworkId));

        TeachingMaterial material = teachingMaterialRepository.findByIdAndTutorId(materialId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Материал не найден с id: " + materialId));

        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + tutorId));

        // Copy file so deleting the attachment does not affect the library material
        String copiedFileName = fileStorageService.copy(material.getFileName());

        Attachment attachment = Attachment.builder()
                .homework(homework)
                .fileName(copiedFileName)
                .originalFileName(material.getOriginalFileName())
                .contentType(material.getContentType())
                .sizeBytes(material.getSizeBytes())
                .uploadedByUser(tutor)
                .uploadedAt(Instant.now())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attached material {} ('{}') to homework {} as attachment {}",
                material.getId(), material.getTitle(), homeworkId, saved.getId());

        return attachmentMapper.toResponse(saved);
    }
}
