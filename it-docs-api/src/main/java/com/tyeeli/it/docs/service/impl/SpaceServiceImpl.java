package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.dto.AclItemRequest;
import com.tyeeli.it.docs.dto.SpaceCreateRequest;
import com.tyeeli.it.docs.dto.SpaceUpdateRequest;
import com.tyeeli.it.docs.repository.SpaceAclRepository;
import com.tyeeli.it.docs.repository.SpaceRepository;
import com.tyeeli.it.docs.repository.entity.Space;
import com.tyeeli.it.docs.repository.entity.SpaceAcl;
import com.tyeeli.it.docs.service.LdapService;
import com.tyeeli.it.docs.service.SpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 空间与权限。
 * 权限判定链：
 * 可见性（公共空间）：PUBLIC 匿名放行 / INTERNAL 需登录 / RESTRICTED 需 ACL 命中（ADMIN 直通）
 * 读个人区域：owner == currentUser（ADMIN 也不越权读）
 * 写公共空间：全局角色 >= EDITOR 且 ACL 命中（USER 直授 或 LDAP_GROUP，域组读 memberOf）
 * ADMIN：写全放行
 * ACL 结果 Caffeine 缓存 5 分钟，变更时失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceServiceImpl implements SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceAclRepository aclRepository;
    private final LdapService ldapService;

    /**
     * spaceId -> (subjectType:subjectName -> role)，5 分钟，变更时失效
     */
    private final Cache<Long, List<SpaceAcl>> aclCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(500).build();

    @Override
    public List<Space> listPublic() {
        return spaceRepository.listPublic();
    }

    /**
     * 登录用户可见的公共空间：PUBLIC 直通；INTERNAL 已登录即通；
     * RESTRICTED 需 ACL 命中（USER 直授或 LDAP 域组），ADMIN 直通。
     */
    @Override
    public List<Space> listVisible(String username, String globalRole, boolean ldapUser) {
        List<Space> out = new ArrayList<Space>();
        for (Space s : spaceRepository.listActivePublic()) {
            if (visibleTo(s, username, globalRole, ldapUser)) {
                out.add(s);
            }
        }
        return out;
    }

    @Override
    public List<Space> listAll() {
        return spaceRepository.listAll();
    }

    @Override
    public Space mustGet(Long id) {
        Space s = spaceRepository.findById(id);
        if (s == null) {
            throw BizException.notFound("空间不存在");
        }
        return s;
    }

    /**
     * 个人区域：首次访问自动创建 P-<username>
     */
    @Override
    public Space ensurePersonalSpace(String username) {
        String code = Constants.PERSONAL_SPACE_PREFIX + username;
        Space s = spaceRepository.findByCode(code);
        if (s != null) {
            return s;
        }
        s = new Space();
        s.setSpaceName(username + " 的个人区域");
        s.setSpaceCode(code);
        s.setSpaceType(Constants.SPACE_PERSONAL);
        s.setOwner(username);
        s.setStatus(Constants.STATUS_ACTIVE);
        s.setDescription("私有草稿区，仅本人可见；发布后进入公共空间");
        spaceRepository.insert(s);
        return s;
    }

    @Override
    public Space create(SpaceCreateRequest req, String operator) {
        String name = req.getName();
        String code = req.getCode();
        String description = req.getDescription();
        if (code == null || !code.matches("[A-Za-z0-9_-]{2,50}")) {
            throw BizException.badRequest("空间编码 2~50 位字母数字_-");
        }
        if (spaceRepository.findByCode(code) != null) {
            throw BizException.badRequest("空间编码已存在");
        }
        Space s = new Space();
        s.setSpaceName(name);
        s.setSpaceCode(code);
        s.setSpaceType(Constants.SPACE_PUBLIC);
        s.setVisibility(checkVisibility(req.getVisibility()));
        s.setDescription(description);
        s.setStatus(Constants.STATUS_ACTIVE);
        spaceRepository.insert(s);
        log.info("空间创建 code={} visibility={} operator={}", code, s.getVisibility(), operator);
        return s;
    }

    @Override
    public void update(Long id, SpaceUpdateRequest req) {
        Space s = mustGet(id);
        if (Constants.SPACE_PERSONAL.equals(s.getSpaceType())) {
            throw BizException.badRequest("个人区域不支持修改信息");
        }
        s.setSpaceName(req.getName());
        s.setDescription(req.getDescription());
        if (StrUtil.isNotBlank(req.getVisibility())) {
            String before = s.getVisibility();
            s.setVisibility(checkVisibility(req.getVisibility()));
            if (!StrUtil.equals(before, s.getVisibility())) {
                log.info("空间可见性变更 spaceId={} {} -> {} ", id, before, s.getVisibility());
            }
        }
        s.setUpdatedAt(new Date());
        spaceRepository.update(s);
    }

    /**
     * 停用 / 启用：停用后所有列表不出现（空间管理页除外），数据全保留
     */
    @Override
    public void changeStatus(Long id, String status, String operator) {
        if (!Constants.STATUS_ACTIVE.equals(status) && !Constants.STATUS_DISABLED.equals(status)) {
            throw BizException.badRequest("status 只能是 ACTIVE / DISABLED");
        }
        Space s = mustGet(id);
        if (Constants.SPACE_PERSONAL.equals(s.getSpaceType())
                && Constants.STATUS_DISABLED.equals(status)) {
            throw BizException.badRequest("个人区域不能停用");
        }
        s.setStatus(status);
        s.setUpdatedAt(new Date());
        spaceRepository.update(s);
        log.info("空间状态变更 spaceId={} status={} operator={}", id, status, operator);
    }

    /** 可见性白名单校验，空值回落 INTERNAL（默认仅登录可看） */
    private String checkVisibility(String visibility) {
        String v = StrUtil.trimToNull(visibility);
        if (v == null) {
            return Constants.SPACE_VIS_INTERNAL;
        }
        if (Constants.SPACE_VIS_PUBLIC.equals(v) || Constants.SPACE_VIS_INTERNAL.equals(v)
                || Constants.SPACE_VIS_RESTRICTED.equals(v)) {
            return v;
        }
        throw BizException.badRequest("visibility 只能是 PUBLIC / INTERNAL / RESTRICTED");
    }

    // ---------------- ACL ----------------

    @Override
    public List<SpaceAcl> listAcl(Long spaceId) {
        List<SpaceAcl> cached = aclCache.getIfPresent(spaceId);
        if (cached != null) {
            return cached;
        }
        List<SpaceAcl> list = aclRepository.listBySpace(spaceId);
        aclCache.put(spaceId, list);
        return list;
    }

    /**
     * 全量覆盖式保存（前端提交整个 ACL 列表），保存后立即失效缓存。
     * 先删后插必须同事务，否则中途失败会留下空授权表（等于清空权限）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAcl(Long spaceId, List<AclItemRequest> acls, String operator) {
        mustGet(spaceId);
        aclRepository.deleteBySpace(spaceId);
        if (acls != null) {
            for (AclItemRequest req : acls) {
                if (req.getSubjectType() == null || req.getSubjectName() == null || req.getRole() == null) {
                    continue;
                }
                SpaceAcl n = new SpaceAcl();
                n.setSpaceId(spaceId);
                n.setSubjectType(req.getSubjectType().trim());
                n.setSubjectName(req.getSubjectName().trim());
                n.setRole(req.getRole().trim());
                n.setCreatedBy(operator);
                aclRepository.insert(n);
            }
        }
        aclCache.invalidate(spaceId);
        log.info("ACL 更新 spaceId={} operator={}", spaceId, operator);
    }

    /**
     * 写权限判定：ADMIN 直接过；PERSONAL 只看 owner；
     * PUBLIC 要求全局角色 ADMIN/EDITOR 且 ACL 命中 EDITOR（USER 直授或 LDAP_GROUP）。
     */
    @Override
    public void checkWrite(Long spaceId, String username, String globalRole, boolean ldapUser) {
        Space s = mustGet(spaceId);
        if (!Constants.STATUS_ACTIVE.equals(s.getStatus())) {
            throw BizException.notFound("空间不存在");
        }
        if (Constants.SPACE_PERSONAL.equals(s.getSpaceType())) {
            if (!username.equals(s.getOwner())) {
                throw BizException.forbidden("个人区域仅本人可写");
            }
            return;
        }
        if (Constants.ROLE_ADMIN.equals(globalRole)) {
            return;
        }
        if (!Constants.ROLE_EDITOR.equals(globalRole)) {
            throw BizException.forbidden("需要 EDITOR 及以上角色");
        }
        for (SpaceAcl a : listAcl(spaceId)) {
            if (!Constants.ACL_EDITOR.equals(a.getRole())) {
                continue;
            }
            boolean userHit = Constants.SUBJECT_USER.equals(a.getSubjectType())
                    && a.getSubjectName().equals(username);
            boolean groupHit = Constants.SUBJECT_LDAP_GROUP.equals(a.getSubjectType())
                    && ldapUser && userInLdapGroup(username, a.getSubjectName());
            if (userHit || groupHit) {
                return;
            }
        }
        throw BizException.forbidden("没有该空间的编辑权限");
    }

    @Override
    public List<Long> editableSpaceIds(String username, String globalRole, boolean ldapUser) {
        List<Long> ids = new ArrayList<Long>();
        if (Constants.ROLE_ADMIN.equals(globalRole)) return ids;
        for (Space space : spaceRepository.listActivePublic()) {
            if (Constants.ROLE_EDITOR.equals(globalRole)
                    && aclHit(space.getId(), username, ldapUser, Constants.ACL_EDITOR)) {
                ids.add(space.getId());
            }
        }
        return ids;
    }

    /**
     * 读权限：个人区域仅 owner（ADMIN 也不可越权读，需求 5.4）；
     * 公共空间按可见性三档：PUBLIC 放行（含未登录）/ INTERNAL 需登录 / RESTRICTED 需 ACL 命中（ADMIN 直通）。
     */
    @Override
    public void checkRead(Long spaceId, String username, String globalRole, boolean ldapUser) {
        Space s = mustGet(spaceId);
        if (!Constants.STATUS_ACTIVE.equals(s.getStatus())) {
            throw BizException.notFound("空间不存在");
        }
        if (Constants.SPACE_PERSONAL.equals(s.getSpaceType())) {
            // username 为 null = 未登录（公开读接口会走到这里），按无权限处理而不是 NPE
            if (username == null || !username.equals(s.getOwner())) {
                throw BizException.forbidden("个人区域仅本人可见");
            }
            return;
        }
        if (!visibleTo(s, username, globalRole, ldapUser)) {
            if (username == null) {
                throw BizException.unauthorized("该内容需要登录后查看");
            }
            if (Constants.SPACE_VIS_RESTRICTED.equals(s.getVisibility())) {
                throw BizException.forbidden("该空间仅限授权名单访问");
            }
            throw BizException.forbidden("无权访问该空间");
        }
    }

    /**
     * 可见性判定（公共空间）：PUBLIC 直通；INTERNAL 要求 username 非空；
     * RESTRICTED 要求 ADMIN 或 ACL 命中（VIEWER/EDITOR 任一）。username 为 null = 未登录。
     */
    private boolean visibleTo(Space s, String username, String globalRole, boolean ldapUser) {
        String vis = StrUtil.blankToDefault(s.getVisibility(), Constants.SPACE_VIS_PUBLIC);
        if (Constants.SPACE_VIS_PUBLIC.equals(vis)) {
            return true;
        }
        if (username == null) {
            return false;
        }
        if (Constants.SPACE_VIS_INTERNAL.equals(vis)) {
            return true;
        }
        if (Constants.ROLE_ADMIN.equals(globalRole)) {
            return true;
        }
        return aclHit(s.getId(), username, ldapUser, null);
    }

    /**
     * ACL 命中：wantRole 为 null 时 VIEWER/EDITOR 任一命中即算（读权限用）；
     * 指定角色时只认该角色（写权限用 EDITOR）。
     */
    private boolean aclHit(Long spaceId, String username, boolean ldapUser, String wantRole) {
        for (SpaceAcl a : listAcl(spaceId)) {
            if (wantRole != null && !wantRole.equals(a.getRole())) {
                continue;
            }
            boolean userHit = Constants.SUBJECT_USER.equals(a.getSubjectType())
                    && a.getSubjectName().equals(username);
            boolean groupHit = Constants.SUBJECT_LDAP_GROUP.equals(a.getSubjectType())
                    && ldapUser && userInLdapGroup(username, a.getSubjectName());
            if (userHit || groupHit) {
                return true;
            }
        }
        return false;
    }

    /**
     * 可见空间：可见公共空间 + 自己的个人区域（不存在则不补，查询不建数据）。
     * 未登录只回 PUBLIC 空间，用于不指定 spaceId 的全局列表与搜索。
     */
    @Override
    public List<Long> visibleSpaceIds(String currentUser, String globalRole, boolean ldapUser) {
        List<Long> ids = new ArrayList<Long>();
        if (currentUser == null) {
            for (Space s : spaceRepository.listPublic()) {
                ids.add(s.getId());
            }
            return ids;
        }
        for (Space s : listVisible(currentUser, globalRole, ldapUser)) {
            ids.add(s.getId());
        }
        Space own = spaceRepository.findByCode(Constants.PERSONAL_SPACE_PREFIX + currentUser);
        if (own != null) {
            ids.add(own.getId());
        }
        return ids;
    }

    /**
     * 域组命中：查用户 AD memberOf（LdapService 缓存 5 分钟），ACL 的 subjectName
     * 填组 CN（IT-Admins）或完整 DN（CN=IT-Admins,OU=...）均可，忽略大小写。
     * 只比直接归属组，不含嵌套；LDAP 查询失败时空列表 = 不命中（fail-closed，
     * USER 直授仍可用）。LOCAL 账号恒 false（调用前已按 ldapUser 过滤）。
     */
    private boolean userInLdapGroup(String username, String groupName) {
        String want = StrUtil.trimToNull(groupName);
        if (want == null) {
            return false;
        }
        for (String g : ldapService.fetchGroups(username)) {
            if (want.equalsIgnoreCase(g)) {
                return true;
            }
        }
        return false;
    }
}
