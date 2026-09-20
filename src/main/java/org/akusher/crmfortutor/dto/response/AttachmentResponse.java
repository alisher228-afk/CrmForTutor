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
public class AttachmentResponse {

    private Long id;
    private Long homeworkId;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long sizeBytes;
    private Long uploadedByUserId;
    private Instant uploadedAt;
}
