import { apiFetch } from '../api/client'
import type { UserSummary } from '../expenses/api'

export type MemberBalance = { userId: string; displayName: string; balance: number }

export type GroupBalances = { myBalance: number; members: MemberBalance[] }

export type Transfer = { from: UserSummary; to: UserSummary; amount: number }

// Nested under ['groups', groupId]: saving an expense invalidates the group, which refreshes balances too.
export const balanceKeys = {
  balances: (groupId: string) => ['groups', groupId, 'balances'] as const,
  simplified: (groupId: string) => ['groups', groupId, 'balances', 'simplified'] as const,
}

export const balancesApi = {
  get: (groupId: string) => apiFetch<GroupBalances>(`/api/groups/${groupId}/balances`),
  simplified: (groupId: string) =>
    apiFetch<{ transfers: Transfer[] }>(`/api/groups/${groupId}/balances/simplified`),
}
