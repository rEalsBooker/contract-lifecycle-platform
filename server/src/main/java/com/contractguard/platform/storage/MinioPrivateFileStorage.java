package com.contractguard.platform.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio")
public class MinioPrivateFileStorage implements PrivateFileStorage {
    private final StorageProperties properties;
    private final MinioClient client;

    public MinioPrivateFileStorage(StorageProperties properties) throws Exception {
        this.properties = properties;
        this.client = MinioClient.builder().endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey()).build();
        ensureBucket();
    }

    @Override
    public String store(Long tenantId, MultipartFile file) throws IOException {
        String key = tenantId + "/" + UUID.randomUUID();
        try (InputStream input = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder().bucket(properties.getBucket()).object(key)
                    .stream(input, file.getSize(), -1).contentType(file.getContentType()).build());
            return key;
        } catch (Exception exception) {
            throw new IOException("MinIO 文件保存失败", exception);
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(properties.getBucket()).object(storageKey).build());
        } catch (Exception exception) {
            throw new IOException("MinIO 文件读取失败", exception);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(properties.getBucket()).object(storageKey).build());
        } catch (Exception exception) {
            throw new IOException("MinIO 文件删除失败", exception);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build());
        if (!exists) client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
    }
}

