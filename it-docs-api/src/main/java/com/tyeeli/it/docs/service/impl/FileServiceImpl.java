package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.FileMetaRepository;
import com.tyeeli.it.docs.repository.entity.FileMeta;
import com.tyeeli.it.docs.service.DocService;
import com.tyeeli.it.docs.service.FileService;
import com.tyeeli.it.docs.service.OpLogService;
import com.tyeeli.it.docs.service.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件：白名单校验 → 按文档/上传人去重 → 存对象存储 → 写 it_file。
 * 正文只存 /api/files/{id}/view，不存物理路径——换存储方案不影响已存内容。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMetaRepository fileRepository;
    private final StorageClient storage;
    private final DocService docService;
    private final OpLogService opLogService;

    @Value("${itdocs.upload.max-bytes:" + com.tyeeli.it.docs.common.Constants.MAX_FILE_BYTES + "}")
    private long maxBytes;

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean inList(String[] list, String ext) {
        return ArrayUtil.contains(list, ext);
    }

    private static byte[] readAll(InputStream in, int size) throws Exception {
        return IoUtil.readBytes(in, true);
    }

    private static String sha256(byte[] data) {
        return DigestUtil.sha256Hex(data);
    }

    @Override
    public FileMeta upload(MultipartFile file, String bizType, Long docId, String operator,
                           String globalRole, boolean ldapUser) {
        if (docId != null) {
            docService.checkEdit(docId, operator, globalRole, ldapUser);
        }
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("文件为空");
        }
        if (file.getSize() > maxBytes) {
            throw BizException.badRequest("单文件上限 " + (maxBytes / 1024 / 1024) + " MB");
        }
        String origin = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extOf(origin);
        boolean isImage = bizType != null && Constants.FILE_IMAGE.equals(bizType);
        if (isImage && !inList(Constants.IMAGE_EXTS, ext)) {
            throw BizException.badRequest("图片仅支持 png/jpg/jpeg/webp/gif");
        }
        if (!isImage && !inList(Constants.ATTACH_EXTS, ext)) {
            throw BizException.badRequest("附件类型不允许：" + ext);
        }

        byte[] bytes;
        try (InputStream in = file.getInputStream()) {
            bytes = readAll(in, (int) file.getSize());
        } catch (Exception e) {
            throw new BizException(500, "读取上传内容失败");
        }
        String sha = sha256(bytes);

        // 去重：同哈希复用同一条记录
        FileMeta exists = fileRepository.findBySha256AndDocIdAndUploader(sha, docId, operator);
        if (exists != null && Constants.STATUS_ACTIVE.equals(exists.getStatus())) {
            return exists;
        }
        if (exists != null) { // 曾被删过：复活记录，对象本体可能还在
            exists.setStatus(Constants.STATUS_ACTIVE);
            fileRepository.update(exists);
            return exists;
        }

        String storedName = sha.substring(0, 16) + "." + ext;
        SimpleDateFormat ym = new SimpleDateFormat("yyyy/MM", Locale.ROOT);
        String relPath = "files/" + ym.format(new Date()) + "/"
                + sha.substring(16, 18) + "/" + sha.substring(18, 20) + "/" + storedName;
        try {
            storage.put(relPath, new java.io.ByteArrayInputStream(bytes), bytes.length,
                    file.getContentType());
        } catch (Exception e) {
            log.error("对象存储写入失败 relPath={}", relPath, e);
            throw new BizException(500, "文件存储失败，请稍后重试");
        }

        FileMeta f = new FileMeta();
        f.setBizType(bizType == null ? Constants.FILE_ATTACHMENT : bizType);
        f.setOriginName(origin);
        f.setStoredName(storedName);
        f.setRelPath(relPath);
        f.setExt(ext);
        f.setMime(file.getContentType());
        f.setSizeBytes((long) bytes.length);
        f.setSha256(sha);
        f.setDocId(docId);
        f.setUploadedBy(operator);
        f.setStatus(Constants.STATUS_ACTIVE);
        fileRepository.insert(f);
        opLogService.record("FILE_UPLOAD", docId, operator, null, f);
        return f;
    }

    @Override
    public FileMeta mustGet(Long id) {
        FileMeta f = fileRepository.findById(id);
        if (f == null || Constants.STATUS_DELETED.equals(f.getStatus())) {
            throw BizException.notFound("文件不存在");
        }
        return f;
    }

    @Override
    public FileMeta mustReadable(Long id, String currentUser, String globalRole, boolean ldapUser) {
        FileMeta f = mustGet(id);
        if (f.getDocId() != null) {
            // 复用文档读权限：公共文档的图游客可看，个人区文档的图只有本人；
            // 已下架文档的图同样只对 ADMIN/EDITOR 开放（工作台编辑时还要显示截图）
            docService.detail(f.getDocId(), currentUser, globalRole, ldapUser);
        } else if (!Constants.ROLE_ADMIN.equals(globalRole)
                && (currentUser == null || !currentUser.equals(f.getUploadedBy()))) {
            throw BizException.notFound("文件不存在");
        }
        return f;
    }

    @Override
    public void bindToDoc(Long id, Long docId, String operator, String globalRole, boolean ldapUser) {
        FileMeta f = mustGet(id);
        docService.checkEdit(docId, operator, globalRole, ldapUser);
        if (f.getDocId() != null) {
            if (f.getDocId().equals(docId)) return;
            throw BizException.forbidden("文件已绑定其他文档");
        }
        if (f.getDocId() == null && !operator.equals(f.getUploadedBy())
                && !Constants.ROLE_ADMIN.equals(globalRole)) {
            throw BizException.forbidden("只能绑定自己上传的文件");
        }
        f.setDocId(docId);
        fileRepository.update(f);
        opLogService.record("FILE_BIND", docId, operator, null, f);
    }

    @Override
    public InputStream openStream(FileMeta f) {
        try {
            return storage.get(f.getRelPath());
        } catch (Exception e) {
            log.error("对象存储读取失败 relPath={}", f.getRelPath(), e);
            throw new BizException(500, "文件读取失败");
        }
    }

    /**
     * 逻辑解绑（不物理删，孤儿由定时任务清理）
     */
    @Override
    public void softDelete(Long id, String operator, String globalRole, boolean ldapUser) {
        FileMeta f = mustGet(id);
        if (f.getDocId() != null) {
            docService.checkEdit(f.getDocId(), operator, globalRole, ldapUser);
        } else if (!operator.equals(f.getUploadedBy()) && !Constants.ROLE_ADMIN.equals(globalRole)) {
            throw BizException.forbidden("无权删除该文件");
        }
        FileMeta before = new FileMeta();
        BeanUtil.copyProperties(f, before);
        f.setStatus(Constants.STATUS_DELETED);
        fileRepository.update(f);
        opLogService.record("FILE_DELETE", f.getDocId(), operator, before, f);
        log.info("文件逻辑删除 id={} relPath={} operator={}", id, f.getRelPath(), operator);
    }
}
