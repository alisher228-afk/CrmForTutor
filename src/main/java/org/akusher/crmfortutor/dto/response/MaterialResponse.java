package org.akusher.crmfortutor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialResponse {

    private Long id;
    private Long tutorId;
    private String title;
    private String description;
    private String category;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long sizeBytes;
    private Instant createdAt;
    private Instant updatedAt;
}
