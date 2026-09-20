package org.akusher.crmfortutor.service;

import org.springframework.core.io.Resource;

public record DownloadedAttachment(
        Resource resource,
        String originalFileName,
        String contentType,
        Long sizeBytes
) {}
