package com.tyeeli.it.docs.converter;

import com.tyeeli.it.docs.dto.FileDTO;
import com.tyeeli.it.docs.repository.entity.FileMeta;

/**
 * 文件 实体 → DTO（对外只给 id + 访问 url）
 */
public final class FileConverter {

    private FileConverter() {
    }

    public static FileDTO toDTO(FileMeta f) {
        if (f == null) {
            return null;
        }
        FileDTO v = new FileDTO();
        v.setId(f.getId());
        v.setUrl("/api/files/" + f.getId() + "/view");
        v.setName(f.getOriginName());
        v.setSize(f.getSizeBytes());
        v.setBizType(f.getBizType());
        return v;
    }
}
