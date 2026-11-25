package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.DocAcl;
import java.util.List;

public interface DocAclRepository {
    List<DocAcl> listByDoc(Long docId);
    List<Long> matchingDocIds(String username, List<String> groups);
    boolean hasAny(Long docId);
    void replace(Long docId, List<DocAcl> rows);
}
