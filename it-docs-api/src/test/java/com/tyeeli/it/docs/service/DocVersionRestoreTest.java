package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.DocAclRepository;
import com.tyeeli.it.docs.repository.DocRepository;
import com.tyeeli.it.docs.repository.DocVersionRepository;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.repository.entity.DocVersion;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.service.impl.DocServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 版本回滚的内容快照语义回归测试：
 * 1. 旧快照（snapshot_v=1）只有标题与正文，回滚不能清空摘要/系统/分类/处理角色/标签；
 * 2. 完整快照（snapshot_v=2）回滚时全部内容字段按快照恢复；
 * 3. 完整快照的系统若在操作人可见范围外，回滚被拒绝（与 update 的规则一致）。
 */
@ExtendWith(MockitoExtension.class)
class DocVersionRestoreTest {

    private static final long DOC_ID = 1L;
    private static final String OPERATOR = "editor";

    @Mock
    private DocRepository docRepository;
    @Mock
    private DocAclRepository docAclRepository;
    @Mock
    private DocVersionRepository versionRepository;
    @Mock
    private SpaceService spaceService;
    @Mock
    private OpLogService opLogService;
    @Mock
    private UserService userService;
    @Mock
    private LdapService ldapService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DocServiceImpl docService;

    private Doc currentDoc() {
        Doc d = new Doc();
        d.setId(DOC_ID);
        d.setDocNo("QA-2026110001");
        d.setSpaceId(10L);
        d.setDocType(Constants.DOC_QA);
        d.setStatus(Constants.STATUS_ACTIVE);
        d.setTitle("新标题");
        d.setSummary("新摘要");
        d.setSystem("WMS");
        d.setCategory("新分类");
        d.setOwner("新处理人");
        d.setTags("新标签");
        d.setContentMd("新正文");
        return d;
    }

    private DocVersion snapshot(int snapshotV) {
        DocVersion v = new DocVersion();
        v.setDocId(DOC_ID);
        v.setVersionNo(1);
        v.setSnapshotV(snapshotV);
        v.setTitle("旧标题");
        v.setContentMd("旧正文");
        return v;
    }

    private void givenRestoreContext(DocVersion version) {
        when(docRepository.findById(DOC_ID)).thenReturn(currentDoc());
        when(versionRepository.find(DOC_ID, 1)).thenReturn(version);
        // 生成回滚后的新版本号
        DocVersion latest = new DocVersion();
        latest.setVersionNo(5);
        when(versionRepository.latest(DOC_ID)).thenReturn(latest);
    }

    @Test
    @DisplayName("旧快照：只恢复标题与正文，元数据保持当前值")
    void legacySnapshotKeepsCurrentMeta() {
        givenRestoreContext(snapshot(Constants.SNAPSHOT_V_LEGACY));

        Doc restored = docService.restore(DOC_ID, 1, OPERATOR, Constants.ROLE_EDITOR, false);

        assertEquals("旧标题", restored.getTitle());
        assertEquals("旧正文", restored.getContentMd());
        // 旧快照没有这些字段，不能把当前值清空
        assertEquals("新摘要", restored.getSummary());
        assertEquals("WMS", restored.getSystem());
        assertEquals("新分类", restored.getCategory());
        assertEquals("新处理人", restored.getOwner());
        assertEquals("新标签", restored.getTags());

        ArgumentCaptor<DocVersion> captor = ArgumentCaptor.forClass(DocVersion.class);
        verify(versionRepository).insert(captor.capture());
        assertEquals(Constants.SNAPSHOT_V_FULL, captor.getValue().getSnapshotV());
        assertEquals("新摘要", captor.getValue().getSummary());
        assertEquals("WMS", captor.getValue().getSystem());
    }

    @Test
    @DisplayName("完整快照：标题、摘要、系统、分类、处理角色、标签、正文全部恢复")
    void fullSnapshotRestoresAllContent() {
        DocVersion full = snapshot(Constants.SNAPSHOT_V_FULL);
        full.setSummary("旧摘要");
        full.setSystem("ERP");
        full.setCategory("旧分类");
        full.setOwner("旧处理人");
        full.setTags("旧标签");
        givenRestoreContext(full);

        Doc restored = docService.restore(DOC_ID, 1, OPERATOR, Constants.ROLE_EDITOR, false);

        assertEquals("旧标题", restored.getTitle());
        assertEquals("旧摘要", restored.getSummary());
        assertEquals("ERP", restored.getSystem());
        assertEquals("旧分类", restored.getCategory());
        assertEquals("旧处理人", restored.getOwner());
        assertEquals("旧标签", restored.getTags());
        assertEquals("旧正文", restored.getContentMd());
    }

    @Test
    @DisplayName("完整快照的系统在操作人范围外：拒绝回滚")
    void fullSnapshotOutsideScopeIsForbidden() {
        User scoped = new User();
        scoped.setUsername(OPERATOR);
        scoped.setRole(Constants.ROLE_EDITOR);
        scoped.setSysScope("WMS");
        when(userService.findByUsername(OPERATOR)).thenReturn(scoped);

        DocVersion full = snapshot(Constants.SNAPSHOT_V_FULL);
        full.setSystem("ERP");
        when(docRepository.findById(DOC_ID)).thenReturn(currentDoc());
        when(versionRepository.find(DOC_ID, 1)).thenReturn(full);

        BizException ex = assertThrows(BizException.class,
                () -> docService.restore(DOC_ID, 1, OPERATOR, Constants.ROLE_EDITOR, false));
        assertEquals(403, ex.getCode());
    }
}
