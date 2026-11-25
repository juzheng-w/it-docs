<template>
  <div>
    <div class="page">
      <div class="kb-card user-toolbar">
        <el-input v-model="keyword" placeholder="用户名 / 姓名搜索" clearable class="user-search"
                  @keyup.enter="load(1)" @clear="load(1)">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="authSource" placeholder="账号来源" clearable class="user-source" @change="load(1)">
          <el-option label="本地账号" value="LOCAL" />
          <el-option label="域账号" value="LDAP" />
        </el-select>
        <span class="grow"></span>
        <el-button type="primary" @click="openCreate">新建账号</el-button>
        <el-button @click="openImport">从 AD 导入</el-button>
      </div>

      <p class="users-hint">
        域账号首次登录自动开通（默认只读，可在此提权）；本地账号由管理员创建，首登强制改密。
      </p>

      <div class="kb-card table-card">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="username" label="用户名" min-width="150" />
          <el-table-column prop="displayName" label="姓名" min-width="120" />
          <el-table-column label="来源" width="90">
            <template #default="{ row }">
              <span class="src" :class="row.authSource === 'LOCAL' ? 'local' : 'ldap'">
                {{ authSourceLabel(row.authSource) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="全局角色" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="roleTagType(row.role)" effect="plain">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="92">
            <template #default="{ row }">
              <span class="status" :class="{ off: row.status !== 'ACTIVE' }">
                <i class="dot"></i>{{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最近登录" width="160">
            <template #default="{ row }">{{ (row.lastLoginAt || '—').toString().substring(0, 16).replace('T', ' ') }}</template>
          </el-table-column>
          <el-table-column label="操作" :width="isNarrow ? 200 : 210" fixed="right" class-name="op-cell">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openEdit(row)">改角色</el-button>
              <el-button v-if="row.authSource === 'LOCAL'" size="small" text type="warning" @click="resetPwd(row)">重置密码</el-button>
              <el-popconfirm
                :title="row.status === 'ACTIVE' ? '确认停用该账号？' : '确认启用该账号？'"
                @confirm="toggleStatus(row)">
                <template #reference>
                  <el-button size="small" text :type="row.status === 'ACTIVE' ? 'danger' : 'success'">
                    {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="total > 20" layout="prev, pager, next, total" :total="total"
                       :page-size="20" :current-page="page" @current-change="load"
                       class="user-pagination" />
      </div>
    </div>

    <!-- 新建账号 -->
    <el-dialog v-model="createVisible" title="新建账号" width="440px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="账号来源" required>
          <el-radio-group v-model="createForm.authSource">
            <el-radio value="LDAP">域账号（AD 密码登录）</el-radio>
            <el-radio value="LOCAL">本地账号（临时密码）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="字母/数字/点，如 zhangsan" maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="createForm.displayName" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="createForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="编辑" value="EDITOR" />
            <el-option label="只读" value="VIEWER" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见系统">
          <el-select v-model="createForm.sysScope" multiple clearable filterable style="width:100%"
                     placeholder="不选 = 全部系统可见">
            <el-option v-for="m in systems" :key="m.id" :label="m.name" :value="m.name" />
          </el-select>
          <div style="font-size:12px;color:var(--kb-text-muted);line-height:1.4;margin-top:4px">数据权限：限定后只能查看/维护所选系统的问题记录，无系统归属的普通文档不受限</div>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" maxlength="100" placeholder="选填" />
        </el-form-item>
        <el-alert v-if="createForm.authSource === 'LDAP'" type="info" :closable="false" show-icon
                  title="域账号不设密码，创建后用户直接用 AD 域密码登录（须先在系统设置配好 LDAP）" />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑成员 -->
    <el-dialog v-model="editVisible" title="编辑成员" width="440px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input :model-value="editRow?.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.displayName" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="编辑" value="EDITOR" />
            <el-option label="只读" value="VIEWER" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见系统">
          <el-select v-model="editForm.sysScope" multiple clearable filterable style="width:100%"
                     placeholder="不选 = 全部系统可见">
            <el-option v-for="m in systems" :key="m.id" :label="m.name" :value="m.name" />
          </el-select>
          <div style="font-size:12px;color:var(--kb-text-muted);line-height:1.4;margin-top:4px">数据权限：限定后只能查看/维护所选系统的问题记录，无系统归属的普通文档不受限</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 从 AD 导入 -->
    <el-dialog v-model="importVisible" title="从 AD 导入域账号" width="560px">
      <div style="display:flex;gap:8px;margin-bottom:10px">
        <el-input v-model="importKeyword" placeholder="登录名 / 姓名，留空列出全部" clearable
                  @keyup.enter="doSearch()">
          <template #append><el-button @click="doSearch()">搜索</el-button></template>
        </el-input>
        <el-select v-model="importRole" style="width:150px">
          <el-option label="只读" value="VIEWER" />
          <el-option label="编辑" value="EDITOR" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="importSysScope" multiple clearable filterable collapse-tags
                   style="flex:1;min-width:180px" placeholder="可见系统：不选 = 全部">
          <el-option v-for="m in systems" :key="m.id" :label="m.name" :value="m.name" />
        </el-select>
      </div>
      <el-alert v-if="!importDone" type="info" :closable="false" show-icon style="margin-bottom:10px"
                title="需在系统设置里配好「LDAP 查询账号」；导入的账号不设密码，用户直接用 AD 域密码登录" />
      <el-table v-if="importDone" ref="importTable" :data="importList" height="300" v-loading="searching" stripe
                row-key="username" @selection-change="onSelChange">
        <el-table-column type="selection" width="42" reserve-selection :selectable="row => !row.exists" />
        <el-table-column prop="username" label="登录名" min-width="130" />
        <el-table-column prop="displayName" label="姓名" min-width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.exists" size="small" type="info" effect="plain">已在库</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="importDone && !searching && !importList.length" description="没搜到域用户" :image-size="60" />
      <div v-if="importDone" style="display:flex;justify-content:space-between;align-items:center;margin-top:10px">
        <span style="font-size:12px;color:var(--el-text-color-secondary)">共 {{ importTotal }} 人（勾选跨页保留）</span>
        <el-pagination small layout="prev, pager, next" :total="importTotal"
                       :page-size="20" :current-page="importPage" @current-change="onImportPage" />
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!selected.length" @click="doImport">
          导入{{ selected.length ? `（${selected.length} 人）` : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 临时密码（一次性展示） -->
    <el-dialog v-model="tempPwdVisible" title="临时密码" width="420px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon
                title="明文只显示这一次，请立即复制并线下告知本人"
                style="margin-bottom:14px" />
      <div style="display:flex;gap:8px;align-items:center">
        <el-input :model-value="tempPwd" readonly style="font-size:16px;font-weight:600" />
        <el-button type="primary" @click="copyTempPwd">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="tempPwdVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '../../api'
import { fetchMeta } from '../../api/meta'
import { authSourceLabel, roleLabel, roleTagType } from '../../constants/labels'
import { useNarrowScreen } from '../../utils/responsive'

const isNarrow = useNarrowScreen()
const systems = ref([])

const keyword = ref('')
const authSource = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const saving = ref(false)

const createVisible = ref(false)
const createForm = ref({ username: '', displayName: '', role: 'VIEWER', email: '', sysScope: [] })
const editVisible = ref(false)
const editRow = ref(null)
const editForm = ref({ displayName: '', email: '', role: '', sysScope: [] })
const tempPwdVisible = ref(false)
const tempPwd = ref('')


async function load(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res = await api.get('/users', {
      params: { keyword: keyword.value || undefined, authSource: authSource.value || undefined, page: p, size: 20 }
    })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.value = { authSource: 'LDAP', username: '', displayName: '', role: 'VIEWER', email: '', sysScope: [] }
  createVisible.value = true
}

async function submitCreate() {
  const f = createForm.value
  if (!f.username.trim() || !f.displayName.trim()) {
    ElMessage.warning('用户名和姓名必填')
    return
  }
  saving.value = true
  try {
    const res = await api.post('/users', {
      authSource: f.authSource,
      username: f.username.trim(),
      displayName: f.displayName.trim(),
      role: f.role,
      email: f.email.trim() || undefined,
      sysScope: f.sysScope
    })
    createVisible.value = false
    if (f.authSource === 'LDAP') {
      ElMessage.success('域档案已创建，用户可直接用 AD 密码登录')
    } else {
      tempPwd.value = res.data.tempPassword || ''
      tempPwdVisible.value = true
    }
    load(1)
  } finally {
    saving.value = false
  }
}

function openEdit(row) {
  editRow.value = row
  editForm.value = {
    displayName: row.displayName || '',
    email: row.email || '',
    role: row.role,
    sysScope: row.sysScope || []
  }
  editVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    await api.patch('/users/' + editRow.value.id, {
      displayName: editForm.value.displayName.trim() || undefined,
      email: editForm.value.email.trim() || undefined,
      role: editForm.value.role,
      sysScope: editForm.value.sysScope
    })
    ElMessage.success('已保存')
    editVisible.value = false
    load(page.value)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const to = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await api.patch('/users/' + row.id, { status: to })
  ElMessage.success(to === 'DISABLED' ? '已停用，立即生效' : '已启用')
  load(page.value)
}

async function resetPwd(row) {
  const res = await api.put('/users/' + row.id + '/password')
  tempPwd.value = res.data.message || ''
  tempPwdVisible.value = true
}

function copyTempPwd() {
  navigator.clipboard.writeText(tempPwd.value)
    .then(() => ElMessage.success('已复制'))
    .catch(() => ElMessage.warning('复制失败，请手动选择复制'))
}

// ---------------- 从 AD 导入 ----------------
const importVisible = ref(false)
const importKeyword = ref('')
const importRole = ref('VIEWER')
const importSysScope = ref([])
const importList = ref([])
const importDone = ref(false)
const searching = ref(false)
const importing = ref(false)
const selected = ref([])
const importPage = ref(1)
const importTotal = ref(0)
const importTable = ref(null)

function openImport() {
  importKeyword.value = ''
  importList.value = []
  selected.value = []
  importPage.value = 1
  importTotal.value = 0
  importDone.value = false
  importVisible.value = true
  // 清掉上一轮 reserve-selection 保留的勾选
  importTable.value && importTable.value.clearSelection()
}

// reset=true：点搜索/回车，回到第 1 页；false：翻页
async function doSearch(reset = true) {
  if (reset) {
    importPage.value = 1
  }
  searching.value = true
  importDone.value = true
  try {
    const res = await api.get('/users/ldap', {
      params: { keyword: importKeyword.value.trim(), page: importPage.value, pageSize: 20 }
    })
    importList.value = (res.data && res.data.list) || []
    importTotal.value = (res.data && res.data.total) || 0
  } catch (e) {
    // 拦截器已提示（503 未配置 LDAP 等）
  } finally {
    searching.value = false
  }
}

function onImportPage(p) {
  importPage.value = p
  doSearch(false)
}

function onSelChange(rows) {
  selected.value = rows
}

async function doImport() {
  importing.value = true
  try {
    const res = await api.post('/users/import-ldap', {
      role: importRole.value,
      sysScope: importSysScope.value,
      users: selected.value.map(u => ({ username: u.username, displayName: u.displayName, email: u.email }))
    })
    ElMessage.success(res.data.message)
    importVisible.value = false
    load(1)
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  load()
  fetchMeta('BIZ_SYSTEM').then(l => { systems.value = l }).catch(() => {})
})
</script>

<style scoped>
.user-toolbar { display:flex; align-items:center; flex-wrap:wrap; gap:10px; margin-bottom:12px; padding:12px 16px; }
.user-toolbar .grow { flex:1; }
.user-search { width:300px; }
.user-source { width:150px; }
.users-hint { margin:0 0 10px; color:var(--kb-text-muted); font-size:13px; line-height:1.6; }
.table-card { padding:14px 16px 16px; }
.src { font-size:12px; color:var(--kb-text-muted); }
.src.ldap { color:var(--kb-text-2); }
.status { display:inline-flex; align-items:center; gap:5px; font-size:12.5px; color:var(--kb-green); }
.status .dot { width:6px; height:6px; border-radius:50%; background:currentColor; }
.status.off { color:var(--kb-text-faint); }
.table-card :deep(.op-cell .el-button + .el-button) { margin-left:6px; }
.user-pagination { margin-top:16px; justify-content:flex-end; }
@media (max-width:768px) {
  .user-search, .user-source { width:100%; }
  .user-toolbar .grow { display:none; }
  .user-toolbar .el-button { flex:1; margin-left:0 !important; }
}
</style>
