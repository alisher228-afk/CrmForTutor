package org.akusher.crmfortutor.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file);

    Resource load(String fileName);

    void delete(String fileName);
}
