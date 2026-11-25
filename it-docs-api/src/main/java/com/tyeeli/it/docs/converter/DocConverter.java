package com.tyeeli.it.docs.converter;

import com.tyeeli.it.docs.dto.DocDTO;
import com.tyeeli.it.docs.dto.DocDetailDTO;
import com.tyeeli.it.docs.dto.DocSaveRequest;
import com.tyeeli.it.docs.dto.DocVersionDTO;
import com.tyeeli.it.docs.dto.DocVersionDetailDTO;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.repository.entity.DocVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档 实体 ↔ DTO/DTO。Web 层不直接接触实体，只经这里转换。
 */
public final class DocConverter {

    private DocConverter() {
    }

    /**
     * 请求 → 实体（只映射业务可写字段，docNo/status/审计字段由服务端设置）
     */
    public static Doc toEntity(DocSaveRequest req) {
        Doc d = new Doc();
        d.setSpaceId(req.getSpaceId());
        d.setDocType(req.getDocType());
        d.setTitle(req.getTitle() == null ? null : req.getTitle().trim());
        d.setSystem(req.getSystem());
        d.setCategory(req.getCategory());
        d.setOwner(req.getOwner());
        d.setSummary(req.getSummary());
        d.setContentMd(req.getContentMd());
        d.setContentJson(req.getContentJson());
        d.setTags(req.getTags());
        return d;
    }

    /**
     * 实体 → 列表项（不含正文）
     */
    public static DocDTO toDTO(Doc d) {
        if (d == null) {
            return null;
        }
        DocDTO v = new DocDTO();
        v.setId(d.getId());
        v.setDocNo(d.getDocNo());
        v.setSpaceId(d.getSpaceId());
        v.setDocType(d.getDocType());
        v.setTitle(d.getTitle());
        v.setSystem(d.getSystem());
        v.setCategory(d.getCategory());
        v.setOwner(d.getOwner());
        v.setSummary(d.getSummary());
        v.setTags(d.getTags());
        v.setStatus(d.getStatus());
        v.setUpdatedBy(d.getUpdatedBy());
        v.setUpdatedAt(d.getUpdatedAt());
        return v;
    }

    public static List<DocDTO> toDTOList(List<Doc> list) {
        List<DocDTO> out = new ArrayList<DocDTO>();
        if (list != null) {
            for (Doc d : list) {
                out.add(toDTO(d));
            }
        }
        return out;
    }

    /**
     * 实体 → 详情（含正文）
     */
    public static DocDetailDTO toDetailDTO(Doc d) {
        if (d == null) {
            return null;
        }
        DocDetailDTO v = new DocDetailDTO();
        v.setId(d.getId());
        v.setDocNo(d.getDocNo());
        v.setSpaceId(d.getSpaceId());
        v.setDocType(d.getDocType());
        v.setTitle(d.getTitle());
        v.setSystem(d.getSystem());
        v.setCategory(d.getCategory());
        v.setOwner(d.getOwner());
        v.setSummary(d.getSummary());
        v.setTags(d.getTags());
        v.setStatus(d.getStatus());
        v.setUpdatedBy(d.getUpdatedBy());
        v.setUpdatedAt(d.getUpdatedAt());
        v.setContentMd(d.getContentMd());
        v.setContentJson(d.getContentJson());
        v.setCreatedBy(d.getCreatedBy());
        v.setCreatedAt(d.getCreatedAt());
        return v;
    }

    public static DocVersionDTO toVersionDTO(DocVersion e) {
        if (e == null) {
            return null;
        }
        DocVersionDTO v = new DocVersionDTO();
        v.setId(e.getId());
        v.setDocId(e.getDocId());
        v.setVersionNo(e.getVersionNo());
        v.setSnapshotV(e.getSnapshotV());
        v.setTitle(e.getTitle());
        v.setOperator(e.getOperator());
        v.setCreatedAt(e.getCreatedAt());
        return v;
    }

    /**
     * 实体 → 版本详情（含正文），仅单个版本查看时使用。
     */
    public static DocVersionDetailDTO toVersionDetailDTO(DocVersion e) {
        if (e == null) {
            return null;
        }
        DocVersionDetailDTO v = new DocVersionDetailDTO();
        v.setId(e.getId());
        v.setDocId(e.getDocId());
        v.setVersionNo(e.getVersionNo());
        v.setTitle(e.getTitle());
        v.setOperator(e.getOperator());
        v.setCreatedAt(e.getCreatedAt());
        v.setSnapshotV(e.getSnapshotV());
        v.setSummary(e.getSummary());
        v.setSystem(e.getSystem());
        v.setCategory(e.getCategory());
        v.setOwner(e.getOwner());
        v.setTags(e.getTags());
        v.setContentMd(e.getContentMd());
        v.setContentJson(e.getContentJson());
        return v;
    }

    public static List<DocVersionDTO> toVersionDTOList(List<DocVersion> list) {
        List<DocVersionDTO> out = new ArrayList<DocVersionDTO>();
        if (list != null) {
            for (DocVersion e : list) {
                out.add(toVersionDTO(e));
            }
        }
        return out;
    }
}
