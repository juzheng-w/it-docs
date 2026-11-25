<template>
  <div class="page">
    <h1 class="page-title">个人中心</h1>

    <!-- 身份信息 -->
    <div class="kb-card">
      <template v-if="me">
        <div class="profile-row"><span class="k">用户名</span><span>{{ me.username }}</span></div>
        <div class="profile-row"><span class="k">姓名</span><span>{{ me.displayName || me.username }}</span></div>
        <div class="profile-row">
          <span class="k">账号来源</span>
          <span>{{ me.authSource === 'LDAP' ? '域账号（密码在 AD 域管理）' : '本地账号' }}</span>
        </div>
        <div class="profile-row">
          <span class="k">角色</span>
          <el-tag size="small" :type="roleTagType(me.role)">{{ roleLabel(me.role) }}</el-tag>
        </div>
        <div class="profile-row">
          <span class="k">个人区域</span>
          <el-link type="primary" @click="$router.push('/personal')">进入我的个人区域（仅自己可见）</el-link>
        </div>
      </template>
      <el-skeleton v-else :rows="4" animated />
    </div>

    <!-- 修改密码：仅本地账号 -->
    <div class="kb-card">
      <h3 style="margin:0 0 6px">修改密码</h3>
      <div class="hint">新密码至少 8 位，且需同时包含字母与数字。</div>
      <template v-if="me && me.authSource === 'LDAP'">
        <el-alert type="info" :closable="false" show-icon style="margin-top:12px"
                  title="域账号的密码在 AD 域控上统一管理，请使用域密码修改流程（Ctrl+Alt+Del），此处不支持。" />
      </template>
      <el-form v-else label-width="90px" style="margin-top:12px;max-width:420px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.old" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.next" type="password" show-password maxlength="64" placeholder="至少 8 位，含字母与数字" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password maxlength="64" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="changePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../../api'
import { roleLabel, roleTagType } from '../../constants/labels'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const user = useUserStore()
const me = ref(null)
const saving = ref(false)
const pwdForm = ref({ old: '', next: '', confirm: '' })

onMounted(async () => {
  const res = await api.get('/users/me')
  me.value = res.data
})

async function changePwd() {
  const f = pwdForm.value
  if (!f.old || !f.next) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }
  if (f.next !== f.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (f.next.length < 8 || !/[a-zA-Z]/.test(f.next) || !/[0-9]/.test(f.next)) {
    ElMessage.warning('新密码至少 8 位，且需同时包含字母与数字')
    return
  }
  saving.value = true
  try {
    await api.put('/users/me/password', { oldPassword: f.old, newPassword: f.next })
    // 改密后旧 JWT 立即失效（token_version 自增），必须重新登录
    ElMessage.success('密码已修改，请重新登录')
    user.logout()
    router.replace('/login')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-row { display: flex; gap: 14px; padding: 7px 0; font-size: 14px; }
.profile-row .k { width: 80px; color: var(--kb-text-muted); flex-shrink: 0; }
.hint { font-size: 12px; color: var(--kb-text-muted); }
</style>
