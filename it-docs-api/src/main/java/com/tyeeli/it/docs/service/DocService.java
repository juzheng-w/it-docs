package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.dto.DocQuery;
import com.tyeeli.it.docs.dto.DocAclRequest;
import com.tyeeli.it.docs.repository.entity.DocAcl;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.repository.entity.DocVersion;

import java.util.List;

/**
 * 文档服务：列表 / 详情 / 新建 / 修改 / 软删 / 换空间（发布）/ 版本与回滚。
 *
 * <p>红线：system = 工单归属系统，不是处理落点；个人空间由后端强制 owner 校验。
 * 每次写操作追加一条版本快照并留审计；删除是软删，物理行不消失。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.DocServiceImpl}</p>
 */
public interface DocService {

    /**
     * 分页列表：公共空间按可见性（PUBLIC 匿名 / INTERNAL 登录 / RESTRICTED 名单）可查；
     * keyword 有值走全文检索，否则条件查询。
     *
     * @param currentUser 当前用户，未登录为 null
     * @param globalRole  当前用户全局角色，未登录为 null
     */
    PageResult<Doc> list(DocQuery query, String currentUser, String globalRole, boolean ldapUser);

    /**
     * 文档详情，含读权限校验。已下架的文档只有 ADMIN / EDITOR 能取到（前台按 404 处理）。
     *
     * @param globalRole 当前用户全局角色，未登录为 null
     */
    Doc detail(Long id, String currentUser, String globalRole, boolean ldapUser);

    /**
     * 新建文档并生成第 1 版快照，需目标空间写权限。
     */
    Doc create(Doc doc, String operator, String globalRole, boolean ldapUser);

    /**
     * 修改文档并追加新版本快照。
     */
    Doc update(Long id, Doc input, String operator, String globalRole, boolean ldapUser);

    /**
     * 软删除（status=DELETED），保留历史版本。
     */
    void delete(Long id, String operator, String globalRole, boolean ldapUser);

    /**
     * 下架 / 恢复（status=DISABLED / ACTIVE）：不删数据，前台立即不可见，工作台可恢复。
     */
    Doc setStatus(Long id, String status, String operator, String globalRole, boolean ldapUser);

    /**
     * 换空间：个人区域 → 公共空间即「发布」，也用于公共空间之间移动。
     */
    Doc moveToSpace(Long id, Long targetSpaceId, String operator, String globalRole, boolean ldapUser);

    /**
     * 版本列表，按版本号倒序；含读权限校验。
     */
    List<DocVersion> versions(Long id, String currentUser, String globalRole, boolean ldapUser);

    /**
     * 单个历史版本详情（含正文），用于回看与回滚前预览。
     */
    DocVersion version(Long id, int versionNo, String currentUser, String globalRole, boolean ldapUser);

    /**
     * 回滚到指定版本：把旧内容写回正文，并追加一条新版本（不删历史）。
     */
    Doc restore(Long id, int versionNo, String operator, String globalRole, boolean ldapUser);

    /** 校验文档编辑权限，供附件等关联操作复用。 */
    void checkEdit(Long id, String currentUser, String globalRole, boolean ldapUser);

    List<DocAcl> listAcl(Long id, String currentUser, String globalRole, boolean ldapUser);

    void saveAcl(Long id, List<DocAclRequest> acls, String operator, String globalRole, boolean ldapUser);
}
