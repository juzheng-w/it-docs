<template>
  <div>
    <div class="page" style="max-width:860px">
      <div class="page-toolbar">
        <h1 class="page-title" style="margin:0">基础数据</h1>
        <span class="grow"></span>
        <el-button type="primary" size="small" @click="openCreate">新增{{ currentLabel }}</el-button>
      </div>

      <div class="kb-card">
        <el-tabs v-model="active">
          <el-tab-pane v-for="t in META_TYPES" :key="t.type" :name="t.type" :label="t.label">
            <el-table :data="rowsOf(t.type)" v-loading="loading" stripe>
              <el-table-column label="名称" min-width="180">
                <template #default="{ row }">
                  <el-tag :color="metaColor(row)" style="color: var(--kb-on-primary);border:none" effect="dark">{{ row.name }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column v-if="t.type === 'CATEGORY'" label="归属系统" min-width="120">
                <template #default="{ row }">
                  <el-tag v-if="sysOf(row)" size="small" :color="metaColor(sysOf(row))"
                          style="color: var(--kb-on-primary);border:none">{{ sysOf(row).name }}</el-tag>
                  <span v-else style="color:var(--kb-text-muted);font-size:12px">通用</span>
                </template>
              </el-table-column>
              <el-table-column v-if="!isNarrow" label="颜色" width="120">
                <template #default="{ row }">
                  <span class="color-dot" :style="{ background: metaColor(row) }"></span>
                  <span style="font-size:12px;color:var(--kb-text-muted);margin-left:6px">{{ row.color || '默认' }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="!isNarrow" prop="sortNo" label="排序" width="80" />
              <el-table-column label="操作" width="130">
                <template #default="{ row }">
                  <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-popconfirm title="删除后不再出现在下拉里，历史文档不受影响。确定？" @confirm="del(row)">
                    <template #reference>
                      <el-button size="small" text type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <p class="hint" style="margin:10px 0 0">
              问题记录的{{ t.label }}下拉选项来自这里；{{ t.type === 'CATEGORY' ? '绑定系统的分类只在该系统的下拉出现，通用分类所有系统可选' : '改颜色只影响展示，不影响已保存的文档内容' }}。标签不在这里维护，编辑文档时直接填写。角色、内容类型等系统名称随程序发布，不在此处修改
            </p>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dlgVisible" :title="dlgForm.id ? '编辑' + currentLabel : '新增' + currentLabel" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="dlgForm.name" maxlength="50" placeholder="同类型内唯一" />
        </el-form-item>
        <el-form-item v-if="dlgForm.type === 'CATEGORY'" label="归属系统">
          <el-select v-model="dlgForm.parentId" clearable placeholder="通用（所有系统可选）" style="width:100%">
            <el-option v-for="m in all.BIZ_SYSTEM" :key="m.id" :label="m.name" :value="m.id">
              <span class="opt-dot" :style="{ background: metaColor(m) }"></span>{{ m.name }}
            </el-option>
          </el-select>
          <div class="hint">选了系统：编辑问题记录时只有选该系统才出现这个分类；留空=通用</div>
        </el-form-item>
        <el-form-item label="颜色">
          <div style="display:flex;gap:10px;align-items:center">
            <el-color-picker v-model="dlgForm.color" :show-alpha="false" />
            <el-button v-if="dlgForm.color" size="small" text @click="dlgForm.color = ''">清除（默认灰）</el-button>
          </div>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="dlgForm.sortNo" :min="0" :max="9999" />
          <span class="hint" style="margin-left:8px">小的在前</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'
import { fetchMeta, invalidateMeta, metaColor, META_TYPES } from '../../api/meta'
import { useNarrowScreen } from '../../utils/responsive'

const isNarrow = useNarrowScreen()

const active = ref('BIZ_SYSTEM')
const loading = ref(false)
const saving = ref(false)
const all = reactive({ BIZ_SYSTEM: [], CATEGORY: [] })

const currentLabel = computed(() => {
  const t = META_TYPES.find(x => x.type === active.value)
  return t ? t.label : ''
})

const rowsOf = type => all[type] || []

// 分类行 → 所属业务系统字典项
function sysOf(row) {
  if (!row.parentId) return null
  return all.BIZ_SYSTEM.find(m => m.id === row.parentId) || null
}

async function load(type) {
  const t = type || active.value
  loading.value = true
  try {
    const res = await api.get('/meta', { params: { type: t } })
    all[t] = res.data || []
  } finally {
    loading.value = false
  }
}

// 切 tab 加载对应字典；分类 tab 还要保证业务系统列表在（归属显示/绑定选择用）
watch(active, t => {
  load(t)
  if (t === 'CATEGORY' && !all.BIZ_SYSTEM.length) {
    load('BIZ_SYSTEM')
  }
})

const dlgVisible = ref(false)
const dlgForm = reactive({ id: null, type: '', parentId: null, name: '', color: '', sortNo: 0 })

function openCreate() {
  Object.assign(dlgForm, { id: null, type: active.value, parentId: null, name: '', color: '', sortNo: 0 })
  dlgVisible.value = true
}

function openEdit(row) {
  Object.assign(dlgForm, {
    id: row.id, type: row.type, parentId: row.parentId || null,
    name: row.name, color: row.color || '', sortNo: row.sortNo
  })
  dlgVisible.value = true
}

async function save() {
  if (!(dlgForm.name || '').trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: dlgForm.name, color: dlgForm.color, sortNo: dlgForm.sortNo,
      parentId: dlgForm.type === 'CATEGORY' ? (dlgForm.parentId || null) : null
    }
    if (dlgForm.id) {
      await api.put('/meta/' + dlgForm.id, payload)
    } else {
      await api.post('/meta', { type: dlgForm.type, ...payload })
    }
    ElMessage.success('已保存')
    invalidateMeta()
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function del(row) {
  await api.delete('/meta/' + row.id)
  ElMessage.success('已删除')
  invalidateMeta()
  load()
}

onMounted(() => {
  load('BIZ_SYSTEM')
})
</script>

<style scoped>
.color-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  vertical-align: middle;
  border: 1px solid var(--kb-dot-border);
}
.opt-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 8px;
  border: 1px solid var(--kb-dot-border);
}
</style>
