<template>
  <div class="page personal-page">
    <header class="personal-head">
      <div>
        <h1>个人区域</h1>
        <p>你一个人的工作台：先把问题记成草稿，确认后再发布到公共知识库。</p>
      </div>
      <el-button type="primary" @click="$router.push('/personal/edit')">新建草稿</el-button>
    </header>

    <div class="personal-stats">
      <div class="pstat">
        <span>我的草稿</span>
        <b>{{ list.length }}</b>
      </div>
      <div class="pstat">
        <span>可编辑空间</span>
        <b>{{ user.spaces.length }}</b>
      </div>
      <div class="pstat">
        <span>当前角色</span>
        <b class="role">{{ roleLabel(user.role) }}</b>
      </div>
    </div>

    <div class="personal-body">
      <section class="kb-card drafts-card">
        <div class="drafts-head">
          <h3>我的草稿</h3>
          <span class="drafts-hint">只有你能看到，发布后才进入公共空间</span>
        </div>

        <el-table v-if="list.length" :data="list" v-loading="loading">
          <el-table-column label="标题" min-width="260">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push('/personal/edit/' + row.id)">{{ row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column v-if="!isNarrow" label="类型" width="90">
            <template #default="{ row }">
              <span class="type-tag">{{ docTypeLabel(row.docType) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="!isNarrow" label="更新时间" width="150">
            <template #default="{ row }">{{ (row.updatedAt || '').substring(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="操作" :width="isNarrow ? 200 : 260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text @click="$router.push('/personal/edit/' + row.id)">编辑</el-button>
              <el-button size="small" text type="success" @click="openPublish(row)">发布</el-button>
              <el-button size="small" text @click="$router.push('/doc/' + row.id)">预览</el-button>
              <el-popconfirm title="确认删除？" @confirm="del(row)">
                <template #reference><el-button size="small" text type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <div v-else-if="!loading" class="drafts-empty">
          <p>还没有草稿。遇到问题时先记一笔，比事后回忆更省时间。</p>
          <el-button @click="$router.push('/personal/edit')">写第一篇草稿</el-button>
        </div>
      </section>

      <aside class="personal-side">
        <div class="kb-card side-card">
          <h4>我的权限</h4>
          <dl class="perm-list">
            <dt>账号</dt><dd>{{ user.displayName || user.username }}</dd>
            <dt>全局角色</dt><dd>{{ roleLabel(user.role) }}</dd>
          </dl>
          <p v-if="user.spaces.length" class="perm-sub">可写入的空间</p>
          <ul v-if="user.spaces.length" class="space-list">
            <li v-for="s in user.spaces" :key="s.id">
              <span class="space-name">{{ s.spaceName }}</span>
              <span class="space-vis">{{ visibilityLabel(s.visibility) }}</span>
            </li>
          </ul>
          <p v-else class="perm-tip">当前账号没有可写入的公共空间，草稿可以交给管理员发布。</p>
        </div>

        <div class="kb-card side-card">
          <h4>关于个人区域</h4>
          <ul class="note-list">
            <li>草稿只有你自己能看到，管理员也读不到。</li>
            <li>发布时选择目标空间，内容会进入公共知识库并留痕。</li>
            <li>发布后要到工作台继续编辑，权限按所在空间判定。</li>
          </ul>
        </div>
      </aside>
    </div>

    <el-dialog v-model="pubVisible" title="发布到公共空间" width="380">
      <p class="pub-tip">发布后内容对空间可见范围内的同事开放。</p>
      <el-select v-model="targetSpace" placeholder="选择目标空间" style="width:100%">
        <el-option v-for="s in user.spaces" :key="s.id" :label="s.spaceName" :value="s.id" />
      </el-select>
      <template #footer>
        <el-button @click="pubVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!targetSpace" @click="doPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api'
import { roleLabel, docTypeLabel, visibilityLabel } from '../../constants/labels'
import { useUserStore } from '../../stores/user'
import { useNarrowScreen } from '../../utils/responsive'

const isNarrow = useNarrowScreen()
const user = useUserStore()
const list = ref([])
const loading = ref(false)
const pubVisible = ref(false)
const targetSpace = ref(null)
const pubDoc = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/docs', { params: { spaceId: user.personalSpaceId, includeDisabled: true, page: 1, size: 100 } })
    list.value = res.data.list || []
  } finally {
    loading.value = false
  }
}

function openPublish(row) {
  pubDoc.value = row
  targetSpace.value = null
  pubVisible.value = true
}

async function doPublish() {
  if (!targetSpace.value) {
    ElMessage.warning('选择目标空间')
    return
  }
  await api.put(`/docs/${pubDoc.value.id}/space`, { spaceId: targetSpace.value })
  ElMessage.success('已发布')
  pubVisible.value = false
  load()
}

async function del(row) {
  await api.delete('/docs/' + row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  // 每次进来刷新一次：空间授权可能刚被管理员改过
  try {
    await user.fetchMe(api)
  } catch { /* 拿不到时用本地缓存兜底 */ }
  load()
})
</script>

<style scoped>
.personal-page { max-width: 1280px; }

.personal-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;
  margin-bottom: 18px;
}
.personal-head h1 { margin: 0; color: var(--kb-text); font-size: 24px; line-height: 1.3; letter-spacing: -.02em; }
.personal-head p { margin: 6px 0 0; color: var(--kb-text-muted); font-size: 13px; }

.personal-stats {
  display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px;
  margin-bottom: 14px;
}
.pstat {
  display: flex; flex-direction: column; gap: 4px;
  padding: 14px 18px; background: var(--kb-surface);
  border: 1px solid var(--kb-border); border-radius: 8px;
}
.pstat span { color: var(--kb-text-muted); font-size: 12px; }
.pstat b { color: var(--kb-text); font-size: 24px; font-weight: 600; line-height: 1.25; }
.pstat b.role { font-size: 18px; padding-top: 3px; }

.personal-body { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 14px; align-items: start; }

.drafts-card { padding: 0 0 6px; overflow: hidden; }
.drafts-head {
  display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
  padding: 14px 20px; border-bottom: 1px solid var(--kb-border);
}
.drafts-head h3 { margin: 0; font-size: 15px; font-weight: 600; color: var(--kb-text); }
.drafts-hint { color: var(--kb-text-muted); font-size: 12px; }
.type-tag {
  display: inline-block; padding: 1px 8px; border-radius: 10px;
  background: var(--kb-surface-2); color: var(--kb-text-muted); font-size: 11px;
}
.drafts-empty {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 56px 20px; text-align: center;
}
.drafts-empty p { margin: 0; color: var(--kb-text-muted); font-size: 13px; }

.side-card { padding: 16px 18px; }
.side-card h4 { margin: 0 0 10px; font-size: 14px; color: var(--kb-text); }
.perm-list { display: grid; grid-template-columns: auto 1fr; gap: 6px 12px; margin: 0; font-size: 13px; }
.perm-list dt { color: var(--kb-text-muted); }
.perm-list dd { margin: 0; color: var(--kb-text); }
.perm-sub { margin: 14px 0 6px; color: var(--kb-text-muted); font-size: 12px; }
.perm-tip { margin: 12px 0 0; color: var(--kb-text-muted); font-size: 12.5px; line-height: 1.65; }
.space-list { margin: 0; padding: 0; list-style: none; }
.space-list li {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 6px 0; border-top: 1px solid var(--kb-border); font-size: 13px;
}
.space-list li:first-child { border-top: 0; }
.space-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--kb-text-2); }
.space-vis { flex: 0 0 auto; color: var(--kb-text-faint); font-size: 11px; }

.note-list { margin: 0; padding-left: 16px; color: var(--kb-text-muted); font-size: 12.5px; line-height: 1.75; }
.note-list li { margin-bottom: 4px; }

.pub-tip { margin: 0 0 12px; color: var(--kb-text-muted); font-size: 13px; }

@media (max-width: 1000px) {
  .personal-body { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .personal-head h1 { font-size: 21px; }
  .personal-head p { font-size: 12px; line-height: 1.5; }
  .personal-stats { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
  .pstat { padding: 11px 12px; }
  .pstat b { font-size: 20px; }
  .pstat b.role { font-size: 15px; }
  .drafts-head { padding: 12px 14px; }
}
</style>
