package com.contractguard.platform.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface PrivateFileStorage {
    String store(Long tenantId, MultipartFile file) throws IOException;
    InputStream open(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
}

