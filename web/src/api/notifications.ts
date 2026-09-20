import { workspaceToken } from './auth'

export interface NotificationItem {
  id: number
  notificationType: string
  title: string
  content: string
  objectType: string
  objectId: number
  route: string
  unread: boolean
  createdAt: string
}

function headers() {
  const token = workspaceToken()
  if (!token) throw new Error('请先选择企业工作空间')
  return { Authorization: `Bearer ${token}` }
}

async function response<T>(result: Response): Promise<T> {
  if (!result.ok) {
    const body = await result.json().catch(() => ({ message: '请求失败' }))
    throw new Error(body.message ?? '请求失败')
  }
  if (result.status === 204 || result.headers.get('content-length') === '0') return undefined as T
  const text = await result.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const notificationApi = {
  list: () => fetch('/api/v1/notifications', { headers: headers() }).then(response<NotificationItem[]>),
  unreadCount: () => fetch('/api/v1/notifications/unread-count', { headers: headers() }).then(response<{ count: number }>),
  markRead: (id: number) => fetch(`/api/v1/notifications/${id}/read`, { method: 'POST', headers: headers() }).then(response<void>),
  markAllRead: () => fetch('/api/v1/notifications/read-all', { method: 'POST', headers: headers() }).then(response<void>)
}
