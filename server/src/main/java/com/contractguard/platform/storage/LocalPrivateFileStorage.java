package com.contractguard.platform.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalPrivateFileStorage implements PrivateFileStorage {
    private final Path root;

    public LocalPrivateFileStorage(StorageProperties properties) {
        this.root = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long tenantId, MultipartFile file) throws IOException {
        String key = tenantId + "/" + UUID.randomUUID();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IOException("非法存储路径");
        Files.createDirectories(target.getParent());
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return key;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) throw new IOException("文件不存在或不可读取");
        return Files.newInputStream(target);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path target = root.resolve(storageKey).normalize();
        if (target.startsWith(root)) Files.deleteIfExists(target);
    }
}

