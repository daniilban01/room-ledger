import { apiFetch, postJson } from '../api/client'

export type GroupRole = 'ADMIN' | 'MEMBER'

export type GroupSummary = {
  id: string
  name: string
  myRole: GroupRole
}

export type Member = {
  userId: string
  displayName: string
  role: GroupRole
  joinedAt: string
}

export type GroupDetails = {
  id: string
  name: string
  joinCode: string
  myRole: GroupRole
  members: Member[]
}

// Query keys in one place: ['groups'] is a prefix of ['groups', id],
// so invalidating ['groups'] refreshes both the list and every group page.
export const groupKeys = {
  all: ['groups'] as const,
  detail: (groupId: string) => ['groups', groupId] as const,
}

export const groupsApi = {
  list: () => apiFetch<GroupSummary[]>('/api/groups'),
  get: (groupId: string) => apiFetch<GroupDetails>(`/api/groups/${groupId}`),
  create: (name: string) => postJson<GroupDetails>('/api/groups', { name }),
  join: (joinCode: string) => postJson<GroupDetails>('/api/groups/join', { joinCode }),
  regenerateJoinCode: (groupId: string) => postJson<GroupDetails>(`/api/groups/${groupId}/join-code`),
  rename: (groupId: string, name: string) =>
    apiFetch<GroupDetails>(`/api/groups/${groupId}`, { method: 'PATCH', body: JSON.stringify({ name }) }),
  remove: (groupId: string) => apiFetch<void>(`/api/groups/${groupId}`, { method: 'DELETE' }),
}
