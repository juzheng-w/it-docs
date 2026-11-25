package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.dto.AclItemRequest;
import com.tyeeli.it.docs.dto.SpaceCreateRequest;
import com.tyeeli.it.docs.dto.SpaceUpdateRequest;
import com.tyeeli.it.docs.repository.entity.Space;
import com.tyeeli.it.docs.repository.entity.SpaceAcl;

import java.util.List;

/**
 * 空间与权限。
 *
 * <p>权限判定链：
 * 可见性（仅公共空间）：PUBLIC 匿名放行 / INTERNAL 需登录 / RESTRICTED 需 ACL 命中（ADMIN 直通）；
 * 读个人区域 → owner == currentUser（ADMIN 也不越权读）；
 * 写公共空间 → 全局角色 &gt;= EDITOR 且 ACL 命中（USER 直授 / LDAP_GROUP）；
 * ADMIN → 写全放行（读个人区域仍不越权）。
 * ACL 结果缓存 5 分钟，配置变更主动失效（单副本即时生效）。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.SpaceServiceImpl}</p>
 */
public interface SpaceService {

    /**
     * 匿名访客可见的公共空间（visibility=PUBLIC）。
     */
    List<Space> listPublic();

    /**
     * 当前登录用户可见的公共空间：PUBLIC + INTERNAL + 已获 ACL 授权的 RESTRICTED（ADMIN 直通）。
     * 查阅端空间下拉、工作台空间选择、me() 接口共用。
     */
    List<Space> listVisible(String username, String globalRole, boolean ldapUser);

    /**
     * 管理视角全部空间（含停用与个人区域），仅管理员使用。
     */
    List<Space> listAll();

    /**
     * 取空间，不存在抛 404 业务异常。
     */
    Space mustGet(Long id);

    /**
     * 取个人区域，不存在则创建 P-&lt;username&gt;（幂等）。
     */
    Space ensurePersonalSpace(String username);

    /**
     * 当前用户可见的空间 id 集合：可见公共空间 + 自己的个人区域。
     *
     * <p>未登录（currentUser=null）只返回 visibility=PUBLIC 的空间。用于「不指定空间」的全局列表与搜索，
     * 把可见范围收敛在 SQL 条件里，避免个人区文档被列表接口翻出来。</p>
     */
    List<Long> visibleSpaceIds(String currentUser, String globalRole, boolean ldapUser);

    /** 当前用户具有编辑权限的公共空间 ID。 */
    List<Long> editableSpaceIds(String currentUser, String globalRole, boolean ldapUser);

    /**
     * 新建公共空间，编码唯一；visibility 缺省 INTERNAL。
     */
    Space create(SpaceCreateRequest req, String operator);

    /**
     * 修改空间名称、描述与可见性。
     */
    void update(Long id, SpaceUpdateRequest req);

    /**
     * 停用 / 启用空间：停用后所有列表不可见（仅空间管理页可见），数据与文档全保留。
     */
    void changeStatus(Long id, String status, String operator);

    /**
     * 空间授权列表（带 5 分钟缓存）。
     */
    List<SpaceAcl> listAcl(Long spaceId);

    /**
     * 全量覆盖式保存授权，保存后立即失效缓存。
     */
    void saveAcl(Long spaceId, List<AclItemRequest> acls, String operator);

    /**
     * 写权限校验，不通过抛 403。
     */
    void checkWrite(Long spaceId, String username, String globalRole, boolean ldapUser);

    /**
     * 读权限校验：公共空间按可见性三档判定，个人区域仅 owner 可读。
     */
    void checkRead(Long spaceId, String username, String globalRole, boolean ldapUser);
}
