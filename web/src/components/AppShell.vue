<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi, currentDisplayName, type Workspace } from '@/api/auth'
import { notificationApi, type NotificationItem } from '@/api/notifications'

defineProps<{ title: string; subtitle?: string }>()

const route = useRoute()
const router = useRouter()
const workspace = ref<Workspace | null>(null)
const displayName = ref(currentDisplayName())
const loadError = ref('')
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const notificationOpen = ref(false)
let notificationTimer: number | undefined

const roleName = computed(() => {
  const labels: Record<string, string> = {
    ENTERPRISE_ADMIN: '企业管理员', CONTRACT_OWNER: '合同负责人', FINANCE: '财务人员',
    PROJECT_OWNER: '项目负责人', PROJECT_MANAGER: '项目负责人', LEGAL: '法务人员', MEMBER: '普通成员'
  }
  return workspace.value?.roleCodes.map(code => labels[code] ?? code).join(' · ') ?? '企业成员'
})

const navItems = [
  { label: '工作台', icon: '⌂', path: '/dashboard' },
  { label: '合同管理', icon: '▤', path: '/contracts' },
  { label: '履约任务', icon: '✓', path: '/tasks' },
  { label: '回款管理', icon: '¥', path: '/receivables' },
  { label: '风险中心', icon: '!', path: '/risks' },
  { label: '组织与设置', icon: '⚙', path: '/organization' }
]

onMounted(async () => {
  try { workspace.value = await authApi.currentWorkspace() }
  catch (cause) { loadError.value = cause instanceof Error ? cause.message : '工作空间不可用' }
  await loadNotifications()
  notificationTimer = window.setInterval(loadNotificationCount, 30000)
})
onBeforeUnmount(() => { if (notificationTimer) window.clearInterval(notificationTimer) })

async function loadNotificationCount() {
  try { unreadCount.value = (await notificationApi.unreadCount()).count } catch { /* 工作空间切换时忽略轮询失败 */ }
}

async function loadNotifications() {
  try {
    ;[notifications.value, unreadCount.value] = await Promise.all([
      notificationApi.list(), notificationApi.unreadCount().then(value => value.count)
    ])
  } catch { notifications.value = []; unreadCount.value = 0 }
}

async function toggleNotifications() {
  notificationOpen.value = !notificationOpen.value
  if (notificationOpen.value) await loadNotifications()
}

async function openNotification(item: NotificationItem) {
  if (item.unread) await notificationApi.markRead(item.id)
  notificationOpen.value = false
  await router.push(item.route)
}

async function readAll() {
  await notificationApi.markAllRead()
  notifications.value = notifications.value.map(item => ({ ...item, unread: false }))
  unreadCount.value = 0
}

function notificationTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function switchWorkspace() {
  localStorage.removeItem('contract-workspace-token')
  router.push('/workspaces')
}

function logout() {
  authApi.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-mark">契</span>
        <span><strong>契约智控</strong><small>Contract Guard</small></span>
      </div>
      <button class="tenant-switcher" type="button" @click="switchWorkspace">
        <span class="tenant-icon">企</span>
        <span><strong>{{ workspace?.tenantName ?? '读取企业中…' }}</strong><small>{{ roleName }} · {{ displayName }}</small></span>
        <b>⌄</b>
      </button>
      <nav class="sidebar-nav" aria-label="主导航">
        <template v-for="item in navItems" :key="item.label">
          <RouterLink v-if="item.path" :to="item.path" :class="{ active: route.path.startsWith(item.path) }">
            <span class="nav-icon">{{ item.icon }}</span><span>{{ item.label }}</span>
          </RouterLink>
          <span v-else class="nav-disabled" :title="`${item.label}将在后续里程碑开放`">
            <span class="nav-icon">{{ item.icon }}</span><span>{{ item.label }}</span><small>规划中</small>
          </span>
        </template>
      </nav>
      <div class="sidebar-user">
        <span class="avatar">{{ displayName.slice(0, 1) }}</span>
        <span><strong>{{ displayName }}</strong><small>{{ roleName }}</small></span>
        <button type="button" title="退出登录" @click="logout">↗</button>
      </div>
    </aside>
    <div class="app-main">
      <header class="app-topbar">
        <div><h1>{{ title }}</h1><p v-if="subtitle">{{ subtitle }}</p></div>
        <div class="topbar-actions"><slot name="actions" /><div class="notification-center"><button class="notification-trigger" type="button" title="站内通知" @click="toggleNotifications">♢<b v-if="unreadCount">{{ unreadCount > 99 ? '99+' : unreadCount }}</b></button><section v-if="notificationOpen" class="notification-popover"><header><div><strong>站内通知</strong><small>仅显示当前企业和当前成员</small></div><button v-if="unreadCount" class="text-button" @click="readAll">全部已读</button></header><div v-if="!notifications.length" class="notification-empty">暂无通知</div><button v-for="item in notifications" v-else :key="item.id" class="notification-item" :class="{ unread: item.unread }" @click="openNotification(item)"><i></i><span><strong>{{ item.title }}</strong><small>{{ item.content }}</small><time>{{ notificationTime(item.createdAt) }}</time></span></button></section></div></div>
      </header>
      <main class="app-content">
        <p v-if="loadError" class="feedback error">{{ loadError }}</p>
        <slot />
      </main>
    </div>
  </div>
</template>
