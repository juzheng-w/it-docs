package com.tyeeli.it.docs.service.storage.impl;

import com.tyeeli.it.docs.service.storage.StorageClient;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;

/**
 * MinIO 实现：bucket 启动时自动创建。
 * 配置：itdocs.storage.type=minio + itdocs.storage.minio.{endpoint,accessKey,secretKey,bucket}
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "itdocs.storage.type", havingValue = "minio")
public class MinioStorageImpl implements StorageClient {

    @Value("${itdocs.storage.minio.endpoint}")
    private String endpoint;
    @Value("${itdocs.storage.minio.access-key}")
    private String accessKey;
    @Value("${itdocs.storage.minio.secret-key}")
    private String secretKey;
    @Value("${itdocs.storage.minio.bucket:itdocs-file}")
    private String bucket;

    private MinioClient client;

    @PostConstruct
    public void init() throws Exception {
        client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO bucket [{}] created at {}", bucket, endpoint);
        }
        log.info("Storage type = minio ({} bucket={})", endpoint, bucket);
    }

    @Override
    public String put(String relPath, InputStream in, long size, String mime) throws Exception {
        try (InputStream is = in) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(relPath)
                    .stream(is, size, -1)
                    .contentType(mime == null ? "application/octet-stream" : mime)
                    .build());
        }
        return relPath;
    }

    @Override
    public InputStream get(String relPath) throws Exception {
        return client.getObject(GetObjectArgs.builder().bucket(bucket).object(relPath).build());
    }

    @Override
    public void remove(String relPath) throws Exception {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(relPath).build());
    }

    @Override
    public String type() {
        return "minio";
    }
}
