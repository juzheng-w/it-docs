package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.dto.DocQuery;
import com.tyeeli.it.docs.dto.DocAclRequest;
import com.tyeeli.it.docs.repository.DocAclRepository;
import com.tyeeli.it.docs.repository.DocRepository;
import com.tyeeli.it.docs.repository.DocVersionRepository;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.repository.entity.DocVersion;
import com.tyeeli.it.docs.repository.entity.DocAcl;
import com.tyeeli.it.docs.repository.entity.Space;
import com.tyeeli.it.docs.service.DocService;
import com.tyeeli.it.docs.service.OpLogService;
import com.tyeeli.it.docs.service.SpaceService;
import com.tyeeli.it.docs.service.UserService;
import com.tyeeli.it.docs.service.LdapService;
import com.tyeeli.it.docs.repository.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 文档：列表（分页 + 筛选 + 全文搜索）/ 详情 / 写（存版本）/ 软删 / 发布 / 回滚。
 * 红线：system = 工单归属系统，不是处理落点；个人空间后端强制 owner 校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocServiceImpl implements DocService {

    private final DocRepository docRepository;
    private final DocAclRepository docAclRepository;
    private final DocVersionRepository versionRepository;
    private final SpaceService spaceService;
    private final OpLogService opLogService;
    private final UserService userService;
    private final LdapService ldapService;
    private final JdbcTemplate jdbcTemplate;

    // ---------------- 数据权限（按业务系统） ----------------

    /**
     * 当前账号的可见系统范围：null = 不限（匿名 / 未配置 / ADMIN），
     * 非空 = 只能看/改归属这些系统的问答卡（无系统归属的普通文档不受限）。
     */
    private List<String> scopeOf(String username) {
        if (StrUtil.isBlank(username)) {
            return null; // 匿名走空间可见性，不叠加系统权限
        }
        User u = userService.findByUsername(username);
        if (u == null || Constants.ROLE_ADMIN.equals(u.getRole()) || StrUtil.isBlank(u.getSysScope())) {
            return null;
        }
        List<String> scope = new ArrayList<String>();
        for (String s : u.getSysScope().split(",")) {
            if (StrUtil.isNotBlank(s)) {
                scope.add(s.trim());
            }
        }
        return scope.isEmpty() ? null : scope;
    }

    /** 文档是否在账号可见系统范围内（无系统归属恒通过） */
    private boolean inScope(Doc d, List<String> scope) {
        return scope == null || StrUtil.isBlank(d.getSystem()) || scope.contains(d.getSystem());
    }

    /** 读路径：范围外按「不存在」处理，不暴露存在性 */
    private void checkScopeRead(Doc d, String username) {
        List<String> scope = scopeOf(username);
        if (scope != null && !inScope(d, scope)) {
            throw BizException.notFound("内容不存在或不在你的可见系统范围");
        }
    }

    /** 写路径：范围外明确拒绝 */
    private void checkScopeWrite(Doc d, String operator) {
        List<String> scope = scopeOf(operator);
        if (scope != null && !inScope(d, scope)) {
            throw BizException.forbidden("该文档归属系统不在你的可见范围内，不能修改");
        }
    }

    /**
     * 列表：公共空间按可见性过滤（PUBLIC 匿名 / INTERNAL 登录 / RESTRICTED 名单），
     * 个人区域只出自己的。keyword 有值走 FULLTEXT，否则按条件分页。
     */
    @Override
    public PageResult<Doc> list(DocQuery query, String currentUser, String globalRole, boolean ldapUser) {
        int page = query.pageNo();
        int size = query.pageSize();
        Long spaceId = query.getSpaceId();
        String system = query.getSystem();
        String category = query.getCategory();
        String docType = query.getDocType();
        String keyword = query.getKeyword();
        // 工作台要能看到已下架的才能恢复；查阅端不传，只看在架
        boolean includeDisabled = Boolean.TRUE.equals(query.getIncludeDisabled());
        // 数据权限：账号限定了可见系统时，只出这些系统的问答卡 + 无系统归属的文档
        List<String> allowedSystems = scopeOf(currentUser);
        List<Long> readableDocIds = readableDocIds(currentUser, globalRole, ldapUser);
        List<Long> editableSpaceIds = spaceService.editableSpaceIds(currentUser, globalRole, ldapUser);

        // 可见范围：指定空间时按该空间校验；不指定则收敛到「可见公共空间 + 本人个人区域」，
        // 否则未登录/普通用户会把别人的个人区草稿一起翻出来
        List<Long> visibleIds = null;
        if (spaceId != null) {
            Space s = spaceService.mustGet(spaceId);
            if (Constants.SPACE_PERSONAL.equals(s.getSpaceType())) {
                if (currentUser == null) {
                    throw BizException.unauthorized("请先登录");
                }
                // 个人区域是本人草稿，不受「可见系统」数据权限约束（否则自己写的草稿会看不见）
                allowedSystems = null;
            }
            spaceService.checkRead(spaceId, currentUser, globalRole, ldapUser);
        } else {
            visibleIds = spaceService.visibleSpaceIds(currentUser, globalRole, ldapUser);
            if (visibleIds.isEmpty()) {
                return PageResult.of(0, page, size, new ArrayList<Doc>());
            }
        }

        if (StrUtil.isNotBlank(keyword)) {
            Page<Doc> p = docRepository.searchByKeyword(keyword.trim(), spaceId, visibleIds,
                    system, category, docType, includeDisabled, allowedSystems, readableDocIds,
                    editableSpaceIds, page, size);
            auditAdminRestrictedReads(p.getRecords(), currentUser, globalRole);
            return PageResult.of(p.getTotal(), page, size, p.getRecords());
        }

        Page<Doc> p = docRepository.page(spaceId, visibleIds, system, category, docType,
                includeDisabled, allowedSystems, readableDocIds, editableSpaceIds, page, size);
        auditAdminRestrictedReads(p.getRecords(), currentUser, globalRole);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    @Override
    public Doc detail(Long id, String currentUser, String globalRole, boolean ldapUser) {
        Doc d = mustGet(id);
        spaceService.checkRead(d.getSpaceId(), currentUser, globalRole, ldapUser);
        checkScopeRead(d, currentUser);
        checkDocRead(d, currentUser, globalRole, ldapUser);
        // 已下架：查阅端打不开（直接链接也不行）；ADMIN/EDITOR 可预览，否则工作台没法编辑恢复
        if (Constants.STATUS_DISABLED.equals(d.getStatus())) {
            boolean canManage = Constants.ROLE_ADMIN.equals(globalRole);
            if (!canManage && Constants.ROLE_EDITOR.equals(globalRole)) {
                try {
                    spaceService.checkWrite(d.getSpaceId(), currentUser, globalRole, ldapUser);
                    canManage = true;
                } catch (BizException ignored) {
                    canManage = false;
                }
            }
            if (!canManage) throw BizException.notFound("内容已下架，暂不可查看");
        }
        return d;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Doc create(Doc doc, String operator, String globalRole, boolean ldapUser) {
        spaceService.checkWrite(doc.getSpaceId(), operator, globalRole, ldapUser);
        if (StrUtil.isBlank(doc.getTitle())) {
            throw BizException.badRequest("标题不能为空");
        }
        // 数据权限：不能新建归属范围外系统的文档（否则建完自己就看不到、也改不了）
        List<String> scope = scopeOf(operator);
        if (scope != null && StrUtil.isNotBlank(doc.getSystem()) && !scope.contains(doc.getSystem())) {
            throw BizException.forbidden("归属系统不在你的可见范围内，不能设为 " + doc.getSystem());
        }
        if (doc.getDocType() == null) {
            doc.setDocType(Constants.DOC_TECHNICAL);
        }
        if (!Constants.DOC_QA.equals(doc.getDocType()) && !Constants.DOC_MD.equals(doc.getDocType())
                && !Constants.DOC_REQUIREMENT.equals(doc.getDocType())
                && !Constants.DOC_TECHNICAL.equals(doc.getDocType())) {
            throw BizException.badRequest("文档类型只能是 qa / req / tech（md 仅旧数据兼容）");
        }
        doc.setDocNo(genDocNo(doc.getDocType()));
        doc.setStatus(Constants.STATUS_ACTIVE);
        doc.setCreatedBy(operator);
        doc.setUpdatedBy(operator);
        doc.setCreatedAt(new Date());
        doc.setUpdatedAt(new Date());
        docRepository.insert(doc);
        saveVersion(doc, operator, 1);
        opLogService.record("CREATE", doc.getId(), operator, null, doc);
        return doc;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Doc update(Long id, Doc input, String operator, String globalRole, boolean ldapUser) {
        Doc before = mustGet(id);
        spaceService.checkWrite(before.getSpaceId(), operator, globalRole, ldapUser);
        if (input.getDocType() != null && !input.getDocType().equals(before.getDocType())) {
            throw BizException.badRequest("文档类型创建后不可更改");
        }
        // 数据权限：改不了范围外的文档，也不能借更新把文档挪到范围外的系统
        List<String> scope = scopeOf(operator);
        if (scope != null) {
            if (!inScope(before, scope)) {
                throw BizException.forbidden("该文档归属系统不在你的可见范围内，不能修改");
            }
            if (StrUtil.isNotBlank(input.getSystem()) && !scope.contains(input.getSystem())) {
                throw BizException.forbidden("归属系统不在你的可见范围内，不能设为 " + input.getSystem());
            }
        }
        Doc snapshot = copy(before);
        before.setTitle(input.getTitle());
        before.setSummary(input.getSummary());
        before.setContentMd(input.getContentMd());
        before.setContentJson(input.getContentJson());
        before.setTags(input.getTags());
        before.setSystem(input.getSystem());
        before.setCategory(input.getCategory());
        before.setOwner(input.getOwner());
        before.setUpdatedBy(operator);
        before.setUpdatedAt(new Date());
        docRepository.update(before);
        Integer nextNo = nextVersionNo(id);
        saveVersion(before, operator, nextNo);
        opLogService.record("UPDATE", id, operator, snapshot, before);
        return before;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id, String operator, String globalRole, boolean ldapUser) {
        Doc before = mustGet(id);
        spaceService.checkWrite(before.getSpaceId(), operator, globalRole, ldapUser);
        checkScopeWrite(before, operator);
        Doc snapshot = copy(before);
        before.setStatus(Constants.STATUS_DELETED);
        before.setUpdatedBy(operator);
        before.setUpdatedAt(new Date());
        docRepository.update(before);
        opLogService.record("DELETE", id, operator, snapshot, before);
    }

    /**
     * 下架 / 恢复：下架只是状态翻转，内容、版本、审计全保留；
     * 前台查阅端立刻看不到（列表与详情都拦），工作台仍可见可编辑可恢复。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Doc setStatus(Long id, String status, String operator, String globalRole, boolean ldapUser) {
        if (!Constants.STATUS_ACTIVE.equals(status) && !Constants.STATUS_DISABLED.equals(status)) {
            throw BizException.badRequest("状态只能是 ACTIVE（上架）或 DISABLED（下架）");
        }
        Doc before = mustGet(id);
        spaceService.checkWrite(before.getSpaceId(), operator, globalRole, ldapUser);
        checkScopeWrite(before, operator);
        if (status.equals(before.getStatus())) {
            return before;
        }
        Doc snapshot = copy(before);
        before.setStatus(status);
        before.setUpdatedBy(operator);
        before.setUpdatedAt(new Date());
        docRepository.update(before);
        opLogService.record(Constants.STATUS_DISABLED.equals(status) ? "UNPUBLISH" : "REPUBLISH",
                id, operator, snapshot, before);
        log.info("文档{} id={} operator={}", Constants.STATUS_DISABLED.equals(status) ? "下架" : "恢复",
                id, operator);
        return before;
    }

    /**
     * 换空间：个人区域 → 公共空间即「发布」，也用于公共空间之间移动
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Doc moveToSpace(Long id, Long targetSpaceId, String operator, String globalRole, boolean ldapUser) {
        Doc before = mustGet(id);
        // 源空间写权限（个人区域只允许本人）
        spaceService.checkWrite(before.getSpaceId(), operator, globalRole, ldapUser);
        checkScopeWrite(before, operator);
        Space target = spaceService.mustGet(targetSpaceId);
        if (Constants.SPACE_PERSONAL.equals(target.getSpaceType())) {
            throw BizException.badRequest("目标必须是公共空间");
        }
        spaceService.checkWrite(targetSpaceId, operator, globalRole, ldapUser);
        Doc snapshot = copy(before);
        before.setSpaceId(targetSpaceId);
        before.setUpdatedBy(operator);
        before.setUpdatedAt(new Date());
        docRepository.update(before);
        opLogService.record("PUBLISH", id, operator, snapshot, before);
        return before;
    }

    @Override
    public List<DocVersion> versions(Long id, String currentUser, String globalRole, boolean ldapUser) {
        Doc d = mustGet(id);
        spaceService.checkRead(d.getSpaceId(), currentUser, globalRole, ldapUser);
        checkScopeRead(d, currentUser);
        checkDocRead(d, currentUser, globalRole, ldapUser);
        return versionRepository.listByDoc(id);
    }

    @Override
    public DocVersion version(Long id, int versionNo, String currentUser, String globalRole, boolean ldapUser) {
        Doc d = mustGet(id);
        spaceService.checkRead(d.getSpaceId(), currentUser, globalRole, ldapUser);
        checkScopeRead(d, currentUser);
        checkDocRead(d, currentUser, globalRole, ldapUser);
        DocVersion v = versionRepository.find(id, versionNo);
        if (v == null) {
            throw BizException.notFound("版本不存在");
        }
        return v;
    }

    @Override
    public void checkEdit(Long id, String currentUser, String globalRole, boolean ldapUser) {
        Doc d = mustGet(id);
        spaceService.checkWrite(d.getSpaceId(), currentUser, globalRole, ldapUser);
        checkScopeWrite(d, currentUser);
    }

    @Override
    public List<DocAcl> listAcl(Long id, String currentUser, String globalRole, boolean ldapUser) {
        checkEdit(id, currentUser, globalRole, ldapUser);
        if (Constants.SPACE_PERSONAL.equals(spaceService.mustGet(mustGet(id).getSpaceId()).getSpaceType())) {
            throw BizException.badRequest("个人区域仅本人可见，不支持单篇授权");
        }
        return docAclRepository.listByDoc(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveAcl(Long id, List<DocAclRequest> acls, String operator, String globalRole, boolean ldapUser) {
        checkEdit(id, operator, globalRole, ldapUser);
        if (Constants.SPACE_PERSONAL.equals(spaceService.mustGet(mustGet(id).getSpaceId()).getSpaceType())) {
            throw BizException.badRequest("个人区域仅本人可见，不支持单篇授权");
        }
        List<DocAcl> rows = new ArrayList<DocAcl>();
        Set<String> seen = new HashSet<String>();
        if (acls != null) {
            for (DocAclRequest req : acls) {
                if (req == null || StrUtil.isBlank(req.getSubjectType()) || StrUtil.isBlank(req.getSubjectName())) continue;
                DocAcl row = new DocAcl();
                row.setDocId(id);
                String subjectType = req.getSubjectType().trim().toUpperCase();
                String subjectName = req.getSubjectName().trim();
                if (!"USER".equals(subjectType) && !"LDAP_GROUP".equals(subjectType)) {
                    throw BizException.badRequest("主体类型只能是 USER 或 LDAP_GROUP");
                }
                if (subjectName.length() > 100) throw BizException.badRequest("授权对象名称最长 100 字");
                if (!seen.add(subjectType + "\u0000" + subjectName.toLowerCase(Locale.ROOT))) continue;
                row.setSubjectType(subjectType);
                row.setSubjectName(subjectName);
                row.setCreatedBy(operator);
                rows.add(row);
            }
        }
        docAclRepository.replace(id, rows);
        opLogService.record("DOC_ACL_UPDATE", id, operator, null, rows);
    }

    private List<Long> readableDocIds(String username, String globalRole, boolean ldapUser) {
        if (Constants.ROLE_ADMIN.equals(globalRole)) return null;
        List<String> groups = ldapUser && StrUtil.isNotBlank(username)
                ? ldapService.fetchGroups(username) : new ArrayList<String>();
        return docAclRepository.matchingDocIds(username, groups);
    }

    private void checkDocRead(Doc doc, String username, String globalRole, boolean ldapUser) {
        if (!docAclRepository.hasAny(doc.getId())) return;
        if (Constants.ROLE_ADMIN.equals(globalRole)) {
            opLogService.record("READ_RESTRICTED", doc.getId(), username, null, doc);
            return;
        }
        if (Constants.ROLE_EDITOR.equals(globalRole)) {
            try {
                spaceService.checkWrite(doc.getSpaceId(), username, globalRole, ldapUser);
                return;
            } catch (BizException ignored) {
                // Space editors retain content access; other editors must match the document ACL.
            }
        }
        List<Long> allowed = readableDocIds(username, globalRole, ldapUser);
        if (allowed == null || !allowed.contains(doc.getId())) {
            throw BizException.forbidden("该文档仅限单篇授权名单查看");
        }
    }

    private void auditAdminRestrictedReads(List<Doc> docs, String username, String globalRole) {
        if (!Constants.ROLE_ADMIN.equals(globalRole) || docs == null) return;
        for (Doc doc : docs) {
            if (docAclRepository.hasAny(doc.getId())) {
                opLogService.record("READ_RESTRICTED", doc.getId(), username, null, doc);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Doc restore(Long id, int versionNo, String operator, String globalRole, boolean ldapUser) {
        Doc d = mustGet(id);
        spaceService.checkWrite(d.getSpaceId(), operator, globalRole, ldapUser);
        checkScopeWrite(d, operator);
        DocVersion v = versionRepository.find(id, versionNo);
        if (v == null) {
            throw BizException.notFound("版本不存在");
        }
        Doc snapshot = copy(d);
        d.setTitle(v.getTitle());
        d.setContentMd(v.getContentMd());
        d.setContentJson(v.getContentJson());
        // 旧快照（snapshot_v=1）只有标题与正文，不能拿 null 覆盖当前摘要/系统/分类/处理角色/标签；
        // 完整快照（snapshot_v>=2）按快照全量恢复。
        if (snapshotVOf(v) >= Constants.SNAPSHOT_V_FULL) {
            // 与 update 一致：不能借回滚把文档挪到操作人范围外的系统
            List<String> scope = scopeOf(operator);
            if (scope != null && StrUtil.isNotBlank(v.getSystem()) && !scope.contains(v.getSystem())) {
                throw BizException.forbidden("该版本归属系统不在你的可见范围内，不能回滚到该版本");
            }
            d.setSummary(v.getSummary());
            d.setSystem(v.getSystem());
            d.setCategory(v.getCategory());
            d.setOwner(v.getOwner());
            d.setTags(v.getTags());
        }
        d.setUpdatedBy(operator);
        d.setUpdatedAt(new Date());
        docRepository.update(d);
        saveVersion(d, operator, nextVersionNo(id));
        opLogService.record("RESTORE", id, operator, snapshot, d);
        return d;
    }

    // ---------------- 私有 ----------------

    private Doc mustGet(Long id) {
        Doc d = docRepository.findById(id);
        if (d == null || Constants.STATUS_DELETED.equals(d.getStatus())) {
            throw BizException.notFound("文档不存在");
        }
        return d;
    }

    /**
     * 落一条完整内容快照：标题、摘要、系统、分类、处理角色、标签、正文一起存，
     * 回滚才能还原整条内容而不只是正文。
     */
    private void saveVersion(Doc d, String operator, int versionNo) {
        DocVersion v = new DocVersion();
        v.setDocId(d.getId());
        v.setVersionNo(versionNo);
        v.setSnapshotV(Constants.SNAPSHOT_V_FULL);
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setSystem(d.getSystem());
        v.setCategory(d.getCategory());
        v.setOwner(d.getOwner());
        v.setTags(d.getTags());
        v.setContentMd(d.getContentMd());
        v.setContentJson(d.getContentJson());
        v.setOperator(operator);
        versionRepository.insert(v);
    }

    /**
     * 旧行没有 snapshot_v（或为 1）时按「仅标题与正文」处理
     */
    private static int snapshotVOf(DocVersion v) {
        return v.getSnapshotV() == null ? Constants.SNAPSHOT_V_LEGACY : v.getSnapshotV();
    }

    private Integer nextVersionNo(Long docId) {
        DocVersion last = versionRepository.latest(docId);
        return last == null ? 1 : last.getVersionNo() + 1;
    }

    private String genDocNo(String docType) {
        String prefix;
        if (Constants.DOC_QA.equals(docType)) prefix = "QA";
        else if (Constants.DOC_REQUIREMENT.equals(docType)) prefix = "REQ";
        else if (Constants.DOC_TECHNICAL.equals(docType)) prefix = "TECH";
        else prefix = "DOC";

        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String sequenceKey = prefix + "-" + month;
        Integer sequence = jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            // LAST_INSERT_ID 在同一连接上返回本次原子递增后的值，避免并发创建拿到相同编号。
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO it_doc_no_seq (seq_key, current_no) VALUES (?, LAST_INSERT_ID(1)) " +
                            "ON DUPLICATE KEY UPDATE current_no = LAST_INSERT_ID(current_no + 1)")) {
                statement.setString(1, sequenceKey);
                statement.executeUpdate();
            }
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (!result.next()) throw new IllegalStateException("无法获取文档编号序号");
                return result.getInt(1);
            }
        });
        return prefix + "-" + month + String.format("%04d", sequence);
    }

    private Doc copy(Doc d) {
        Doc c = new Doc();
        c.setId(d.getId());
        c.setDocNo(d.getDocNo());
        c.setSpaceId(d.getSpaceId());
        c.setDocType(d.getDocType());
        c.setTitle(d.getTitle());
        c.setSystem(d.getSystem());
        c.setCategory(d.getCategory());
        c.setOwner(d.getOwner());
        c.setSummary(d.getSummary());
        c.setContentMd(d.getContentMd());
        c.setContentJson(d.getContentJson());
        c.setTags(d.getTags());
        c.setStatus(d.getStatus());
        c.setUpdatedBy(d.getUpdatedBy());
        return c;
    }
}
