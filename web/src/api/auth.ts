export interface LoginResponse { accessToken: string; tokenType: string; expiresAt: string; displayName: string }
export interface Workspace { membershipId: number; tenantId: number; tenantName: string; roleCodes: string[] }
export interface WorkspaceToken extends LoginResponse, Workspace { }

const IDENTITY_TOKEN_KEY = 'contract-identity-token'
const WORKSPACE_TOKEN_KEY = 'contract-workspace-token'
const DISPLAY_NAME_KEY = 'contract-display-name'

async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers ?? {})
    }
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: '服务暂不可用' }))
    throw new Error(body.message ?? '请求失败')
  }
  return response.json() as Promise<T>
}

export const authApi = {
  async login(username: string, password: string) {
    const result = await request<LoginResponse>('/api/v1/auth/login', {
      method: 'POST', body: JSON.stringify({ username, password })
    })
    localStorage.setItem(IDENTITY_TOKEN_KEY, result.accessToken)
    localStorage.setItem(DISPLAY_NAME_KEY, result.displayName)
    return result
  },
  listWorkspaces() {
    return request<Workspace[]>('/api/v1/workspaces', {}, localStorage.getItem(IDENTITY_TOKEN_KEY) ?? undefined)
  },
  async switchWorkspace(membershipId: number) {
    const result = await request<WorkspaceToken>(`/api/v1/workspaces/${membershipId}/switch`, {
      method: 'POST'
    }, localStorage.getItem(IDENTITY_TOKEN_KEY) ?? undefined)
    localStorage.setItem(WORKSPACE_TOKEN_KEY, result.accessToken)
    return result
  },
  currentWorkspace() {
    return request<Workspace>('/api/v1/me', {}, localStorage.getItem(WORKSPACE_TOKEN_KEY) ?? undefined)
  },
  logout() {
    localStorage.removeItem(IDENTITY_TOKEN_KEY)
    localStorage.removeItem(WORKSPACE_TOKEN_KEY)
    localStorage.removeItem(DISPLAY_NAME_KEY)
  }
}

export function workspaceToken() {
  return localStorage.getItem(WORKSPACE_TOKEN_KEY) ?? undefined
}

export function currentDisplayName() {
  return localStorage.getItem(DISPLAY_NAME_KEY) ?? '企业成员'
}
