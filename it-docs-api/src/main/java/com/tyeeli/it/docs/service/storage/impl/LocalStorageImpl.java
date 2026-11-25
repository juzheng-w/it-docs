package com.tyeeli.it.docs.service.storage.impl;

import com.tyeeli.it.docs.service.storage.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地盘实现：单机部署默认生效（itdocs.storage.type=local）。
 * 容器部署时必须把 base-dir 挂到宿主机持久化目录；多副本/跨机迁移时改用 minio。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "itdocs.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageImpl implements StorageClient {

    @Value("${itdocs.storage.local.base-dir:./data/files}")
    private String baseDir;

    @PostConstruct
    public void init() {
        log.info("Storage type = LOCAL（{}）——单机部署；多副本请改用对象存储", baseDir);
    }

    @Override
    public String put(String relPath, InputStream in, long size, String mime) throws Exception {
        Path target = Paths.get(baseDir, relPath);
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return relPath;
    }

    @Override
    public InputStream get(String relPath) throws Exception {
        return Files.newInputStream(Paths.get(baseDir, relPath));
    }

    @Override
    public void remove(String relPath) throws Exception {
        Files.deleteIfExists(Paths.get(baseDir, relPath));
    }

    @Override
    public String type() {
        return "local";
    }
}
