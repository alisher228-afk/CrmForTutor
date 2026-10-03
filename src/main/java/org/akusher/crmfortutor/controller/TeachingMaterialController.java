package org.akusher.crmfortutor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.akusher.crmfortutor.dto.request.MaterialUpdateRequest;
import org.akusher.crmfortutor.dto.response.AttachmentResponse;
import org.akusher.crmfortutor.dto.response.MaterialResponse;
import org.akusher.crmfortutor.service.DownloadedAttachment;
import org.akusher.crmfortutor.service.TeachingMaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
public class TeachingMaterialController {

    private final TeachingMaterialService teachingMaterialService;

    @PostMapping(value = "/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> createMaterial(
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {

        MaterialResponse response = teachingMaterialService.createMaterial(title, category, description, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/materials")
    public ResponseEntity<List<MaterialResponse>> getMaterials(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category) {

        List<MaterialResponse> materials = teachingMaterialService.getMaterials(search, category);
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/materials/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(teachingMaterialService.getCategories());
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<MaterialResponse> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(teachingMaterialService.getMaterialById(id));
    }

    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable Long id,
            @RequestParam(value = "inline", required = false, defaultValue = "false") boolean inline) {

        DownloadedAttachment downloaded = teachingMaterialService.downloadMaterial(id);

        MediaType mediaType;
        try {
            mediaType = downloaded.contentType() != null
                    ? MediaType.parseMediaType(downloaded.contentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(downloaded.originalFileName(), StandardCharsets.UTF_8)
                .build();

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());

        if (downloaded.sizeBytes() != null && downloaded.sizeBytes() > 0) {
            builder.contentLength(downloaded.sizeBytes());
        }

        return builder.body(downloaded.resource());
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<MaterialResponse> updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody MaterialUpdateRequest request) {

        return ResponseEntity.ok(teachingMaterialService.updateMaterial(id, request));
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        teachingMaterialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/homework/{homeworkId}/attachments/from-material/{materialId}")
    public ResponseEntity<AttachmentResponse> attachMaterialToHomework(
            @PathVariable Long homeworkId,
            @PathVariable Long materialId) {

        AttachmentResponse response = teachingMaterialService.attachMaterialToHomework(homeworkId, materialId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
