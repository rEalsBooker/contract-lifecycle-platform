<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, currentDisplayName, type Workspace } from '@/api/auth'

const router = useRouter()
const workspaces = ref<Workspace[]>([])
const error = ref('')
const loading = ref(true)
const enteringId = ref<number | null>(null)

onMounted(async () => {
  try { workspaces.value = await authApi.listWorkspaces() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '无法读取工作空间' }
  finally { loading.value = false }
})

async function enter(workspace: Workspace) {
  enteringId.value = workspace.membershipId
  try {
    await authApi.switchWorkspace(workspace.membershipId)
    await router.push('/dashboard')
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '无法进入工作空间' }
  finally { enteringId.value = null }
}

function backToLogin() { authApi.logout(); router.push('/login') }
</script>

<template>
  <main class="auth-layout workspace-page">
    <section class="brand-panel">
      <div class="brand-lockup"><span class="brand-mark light">契</span><span><strong>契约智控</strong><small>Contract Guard</small></span></div>
      <div class="brand-message"><h1>你属于多个企业<br />请选择本次进入的工作空间</h1><p>系统只列出当前账号的有效成员关系。切换企业后，仅加载该企业授权范围内的数据。</p></div>
      <div class="security-note"><span>盾</span><p><strong>企业间数据完全隔离</strong><br />不同企业的合同、财务与 AI 会话互不可见</p></div>
    </section>
    <section class="login-panel">
      <div class="workspace-picker">
        <h2>选择工作空间</h2>
        <p class="muted">当前账号：{{ currentDisplayName() }} · 共 {{ workspaces.length }} 条有效成员关系</p>
        <p v-if="loading" class="loading-state">正在读取工作空间…</p>
        <p v-else-if="error" class="feedback error">{{ error }}</p>
        <div v-else-if="workspaces.length === 0" class="empty-state"><span>企</span><h3>暂无可进入的企业</h3><p>请联系企业管理员检查成员关系是否有效。</p></div>
        <div v-else class="workspace-list">
          <button v-for="item in workspaces" :key="item.membershipId" class="workspace-card" :disabled="enteringId !== null" @click="enter(item)">
            <span class="tenant-icon large">企</span>
            <span class="workspace-info"><strong>{{ item.tenantName }}</strong><small>{{ item.roleCodes.join(' · ') }} · 有效成员</small></span>
            <span class="enter-link">{{ enteringId === item.membershipId ? '进入中…' : '进入' }} →</span>
          </button>
        </div>
        <button class="text-button back-login" type="button" @click="backToLogin">← 返回登录</button>
        <p class="workspace-footnote">仅列出有效企业，错误提示不会暴露其他企业信息</p>
      </div>
    </section>
  </main>
</template>
