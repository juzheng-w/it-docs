<template>
  <div>
    <div class="page spaces-page">
      <div class="spaces-toolbar">
        <p class="spaces-hint">
          公开=免登录可看；内部=登录即可看；受限=仅授权名单可见。停用不删数据。
          个人空间随用户自动创建、仅本人可见，不在此页管理。
        </p>
        <el-button type="primary" @click="openCreate">新建空间</el-button>
      </div>

      <div v-loading="loading" class="space-list">
        <div v-for="row in list" :key="row.id" class="space-row" :class="{ off: row.status === 'DISABLED' }">
          <div class="space-main">
            <div class="space-title">
              <strong>{{ row.spaceName }}</strong>
              <span v-if="!isNarrow" class="space-code">{{ row.spaceCode }}</span>
              <span class="pill public">{{ spaceTypeLabel(row.spaceType) }}</span>
              <span class="pill" :class="visMeta(row.visibility).tag">{{ visMeta(row.visibility).label }}</span>
              <span v-if="row.status === 'DISABLED'" class="pill warn">已停用</span>
            </div>
            <p v-if="row.description" class="space-desc">{{ row.description }}</p>
          </div>

          <div class="space-edit">
            <span class="space-edit-label">可编辑：</span>
            <template v-if="editorsOf(row.id).length">
              <span v-for="name in editorsOf(row.id).slice(0, 5)" :key="name" class="editor-chip">{{ name }}</span>
              <span v-if="editorsOf(row.id).length > 5" class="editor-chip more">+{{ editorsOf(row.id).length - 5 }}</span>
            </template>
            <span v-else class="space-edit-empty">按角色权限</span>
            <el-button size="small" @click="openAcl(row)">配置</el-button>
          </div>

          <div class="space-actions">
            <el-button size="small" text type="primary" :disabled="row.status === 'DISABLED'"
                       @click="openEdit(row)">编辑</el-button>
            <el-popconfirm
              :title="row.status === 'DISABLED' ? '恢复后查阅端与工作台立即可见。确定？' : '停用后所有列表不可见（数据保留），仅本页可恢复。确定？'"
              @confirm="toggleStatus(row)">
              <template #reference>
                <el-button size="small" text :type="row.status === 'DISABLED' ? 'success' : 'danger'">
                  {{ row.status === 'DISABLED' ? '恢复' : '停用' }}
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <el-empty v-if="!loading && !list.length" description="还没有公共空间" />
      </div>
    </div>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dlgVisible" :title="dlgForm.id ? '编辑空间' : '新建空间'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="dlgForm.name" maxlength="50" placeholder="最长 50 字" />
        </el-form-item>
        <el-form-item v-if="!dlgForm.id" label="编码" required>
          <el-input v-model="dlgForm.code" maxlength="50" placeholder="2~50 位字母数字 _ -，创建后不可改" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="dlgForm.visibility" style="width:100%">
            <el-option v-for="v in VIS_OPTIONS" :key="v.value" :label="v.label" :value="v.value">
              <span>{{ v.label }}</span>
              <span style="float:right;color:var(--kb-text-muted);font-size:12px">{{ v.hint }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dlgForm.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 授权名单（ACL） -->
    <el-dialog v-model="aclVisible" :title="`授权名单：${aclSpace?.spaceName || ''}`" width="620px">
      <p class="hint" style="margin:0 0 10px">
        受限空间：名单内可看（只读成员只读 / 编辑成员可写）；内部与公开空间：名单只控制「谁能编辑」。
        用户按用户名直授；域组按 AD 组授权（填组 CN 或完整 DN，直接归属才命中，变更约 5 分钟生效）。
        全部行提交后保存，未列出的授权会被移除。
      </p>
      <el-table :data="aclRows" size="small" stripe>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-select v-model="row.subjectType" style="width:100%">
              <el-option label="用户" value="USER" />
              <el-option label="域组" value="LDAP_GROUP" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="用户 / 域组">
          <template #default="{ row, $index }">
            <el-select v-if="row.subjectType !== 'LDAP_GROUP'" v-model="row.subjectName" filterable allow-create default-first-option
                       style="width:100%" placeholder="从成员选择或输入用户名">
              <el-option v-for="u in userOptions" :key="u.username" :label="`${u.displayName || u.username}（${u.username}）`"
                         :value="u.username" />
            </el-select>
            <el-input v-else v-model="row.subjectName" style="width:100%"
                      placeholder="AD 组名，如 IT-Admins（填 CN 或完整 DN 均可）" />
          </template>
        </el-table-column>
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-select v-model="row.role" style="width:100%">
              <el-option label="可编辑" value="EDITOR" />
              <el-option label="只读" value="VIEWER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="" width="70">
          <template #default="{ $index }">
            <el-button size="small" text type="danger" @click="aclRows.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button size="small" style="margin-top:8px" @click="aclRows.push({ subjectType: 'USER', subjectName: '', role: 'VIEWER' })">+ 添加授权</el-button>
      <template #footer>
        <el-button @click="aclVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAcl">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'
import { spaceTypeLabel } from '../../constants/labels'
import { useNarrowScreen } from '../../utils/responsive'

const isNarrow = useNarrowScreen()
const VIS_OPTIONS = [
  { value: 'PUBLIC', label: '公开', tag: 'success', hint: '免登录可看' },
  { value: 'INTERNAL', label: '内部', tag: 'primary', hint: '登录可看' },
  { value: 'RESTRICTED', label: '受限', tag: 'danger', hint: '仅授权名单' }
]

const list = ref([])
const loading = ref(false)
const saving = ref(false)

const visMeta = v => VIS_OPTIONS.find(x => x.value === v) || { label: v || '-', tag: 'info' }

// 可编辑名单，用于行内 chips；空间数量很少，逐个取即可
const aclMap = reactive({})

function editorsOf(id) {
  return aclMap[id] || []
}

async function loadAclSummaries(rows) {
  if (rows.length > 12) return
  await Promise.all(rows.map(async r => {
    try {
      const res = await api.get('/spaces/' + r.id + '/acl')
      aclMap[r.id] = (res.data || []).filter(a => a.role === 'EDITOR').map(a => a.subjectName)
    } catch {
      aclMap[r.id] = []
    }
  }))
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/spaces', { params: { scope: 'all' } })
    // 个人空间由系统按用户自动创建、不可修改，列在这里只是噪音
    list.value = (res.data || []).filter(s => s.spaceType !== 'PERSONAL')
    loadAclSummaries(list.value)
  } finally {
    loading.value = false
  }
}

// ---------------- 新建 / 编辑 ----------------
const dlgVisible = ref(false)
const dlgForm = reactive({ id: null, name: '', code: '', visibility: 'INTERNAL', description: '' })

function openCreate() {
  Object.assign(dlgForm, { id: null, name: '', code: '', visibility: 'INTERNAL', description: '' })
  dlgVisible.value = true
}

function openEdit(row) {
  Object.assign(dlgForm, {
    id: row.id, name: row.spaceName, code: row.spaceCode,
    visibility: row.visibility || 'INTERNAL', description: row.description || ''
  })
  dlgVisible.value = true
}

async function save() {
  if (!(dlgForm.name || '').trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  if (!dlgForm.id && !(dlgForm.code || '').trim()) {
    ElMessage.warning('编码不能为空')
    return
  }
  saving.value = true
  try {
    if (dlgForm.id) {
      await api.put('/spaces/' + dlgForm.id, {
        name: dlgForm.name, visibility: dlgForm.visibility, description: dlgForm.description
      })
    } else {
      await api.post('/spaces', {
        name: dlgForm.name, code: dlgForm.code, visibility: dlgForm.visibility, description: dlgForm.description
      })
    }
    ElMessage.success('已保存')
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------- 停用 / 恢复 ----------------
async function toggleStatus(row) {
  const target = row.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED'
  await api.patch('/spaces/' + row.id + '/status', { status: target })
  ElMessage.success(target === 'DISABLED' ? '已停用' : '已恢复')
  load()
}

// ---------------- 授权名单 ----------------
const aclVisible = ref(false)
const aclSpace = ref(null)
const aclRows = ref([])
const userOptions = ref([])

function openAcl(row) {
  aclSpace.value = row
  aclRows.value = []
  aclVisible.value = true
  api.get('/spaces/' + row.id + '/acl').then(res => {
    aclRows.value = (res.data || []).map(a => ({ subjectType: a.subjectType || 'USER', subjectName: a.subjectName, role: a.role }))
  })
  if (!userOptions.value.length) {
    api.get('/users', { params: { page: 1, size: 200 } }).then(res => {
      userOptions.value = res.data.list || []
    }).catch(() => {})
  }
}

async function saveAcl() {
  const items = aclRows.value
    .filter(r => (r.subjectName || '').trim())
    .map(r => ({
      subjectType: r.subjectType === 'LDAP_GROUP' ? 'LDAP_GROUP' : 'USER',
      subjectName: r.subjectName.trim(),
      role: r.role
    }))
  saving.value = true
  try {
    await api.put('/spaces/' + aclSpace.value.id + '/acl', items)
    ElMessage.success('授权已保存')
    aclVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.spaces-page { max-width: 1100px; }
.spaces-toolbar { display: flex; align-items: center; gap: 16px; margin-bottom: 14px; }
.spaces-hint { flex: 1; margin: 0; color: var(--kb-text-muted); font-size: 13px; line-height: 1.6; }
.space-list { min-height: 120px; }
.space-row {
  display: flex; align-items: center; gap: 20px; flex-wrap: wrap;
  margin-bottom: 10px; padding: 14px 18px;
  background: var(--kb-surface); border: 1px solid var(--kb-border); border-radius: 4px;
}
.space-row.off { opacity: .62; }
.space-main { flex: 1; min-width: 240px; }
.space-title { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.space-title strong { font-size: 15px; font-weight: 600; color: var(--kb-text); }
.space-code { color: var(--kb-text-faint); font-size: 12px; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
.pill {
  padding: 1px 7px; border-radius: 3px; font-size: 11px; line-height: 18px;
  background: var(--kb-surface-2); color: var(--kb-text-muted);
}
.pill.public { background: var(--kb-green-bg); color: var(--kb-green); }
.pill.primary { background: var(--kb-primary-soft); color: var(--kb-primary); }
.pill.success { background: var(--kb-green-bg); color: var(--kb-green); }
.pill.danger { background: var(--kb-red-bg); color: var(--kb-red); }
.pill.warn { background: var(--kb-orange-bg); color: var(--kb-orange); }
.space-desc { margin: 6px 0 0; color: var(--kb-text-muted); font-size: 12px; line-height: 1.6; }
.space-edit { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; flex: 1 1 260px; }
.space-edit-label { color: var(--kb-text-muted); font-size: 12.5px; }
.space-edit-empty { color: var(--kb-text-faint); font-size: 12.5px; margin-right: 4px; }
.editor-chip {
  padding: 1px 8px; border-radius: 10px; font-size: 11px;
  border: 1px solid var(--kb-border); color: var(--kb-text-2); background: var(--kb-surface-2);
}
.editor-chip.more { color: var(--kb-text-muted); }
.space-actions { flex: 0 0 auto; }
@media (max-width: 768px) {
  .spaces-toolbar { align-items: flex-start; }
  .spaces-hint { font-size: 12px; }
  .space-row { padding: 12px 14px; gap: 10px; }
  .space-edit { flex: 1 1 100%; justify-content: flex-start; }
}
</style>
