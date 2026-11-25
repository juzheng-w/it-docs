package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.repository.DocAclRepository;
import com.tyeeli.it.docs.repository.entity.DocAcl;
import com.tyeeli.it.docs.repository.mapper.DocAclMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DocAclRepositoryImpl implements DocAclRepository {
    private final DocAclMapper mapper;

    @Override public List<DocAcl> listByDoc(Long docId) {
        return mapper.selectList(new QueryWrapper<DocAcl>().eq("doc_id", docId).orderByAsc("id"));
    }

    @Override public List<Long> matchingDocIds(String username, List<String> groups) {
        if ((username == null || username.trim().isEmpty()) && (groups == null || groups.isEmpty())) return new ArrayList<Long>();
        QueryWrapper<DocAcl> qw = new QueryWrapper<DocAcl>();
        qw.select("DISTINCT doc_id");
        qw.and(w -> {
            if (username != null && !username.trim().isEmpty()) {
                w.eq("subject_type", "USER").eq("subject_name", username);
                if (groups != null && !groups.isEmpty()) w.or().eq("subject_type", "LDAP_GROUP").in("subject_name", groups);
            } else {
                w.eq("subject_type", "LDAP_GROUP").in("subject_name", groups);
            }
        });
        List<DocAcl> rows = mapper.selectList(qw);
        List<Long> ids = new ArrayList<Long>();
        for (DocAcl row : rows) ids.add(row.getDocId());
        return ids;
    }

    @Override public boolean hasAny(Long docId) {
        return mapper.selectCount(new QueryWrapper<DocAcl>().eq("doc_id", docId)) > 0;
    }

    @Override public void replace(Long docId, List<DocAcl> rows) {
        mapper.delete(new QueryWrapper<DocAcl>().eq("doc_id", docId));
        if (rows != null) for (DocAcl row : rows) mapper.insert(row);
    }
}
