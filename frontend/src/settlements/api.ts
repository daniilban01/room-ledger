import { apiFetch, postJson } from '../api/client'
import type { UserSummary } from '../expenses/api'

export type SettlementStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED'

export type Settlement = {
  id: string
  from: UserSummary
  to: UserSummary
  amount: number
  note: string | null
  status: SettlementStatus
  createdAt: string
  resolvedAt: string | null
  version: number
}

export type OwedExpense = {
  expenseId: string
  description: string
  expenseDate: string
  paidBy: UserSummary
  share: number
}

export type SettlementDetails = { settlement: Settlement; payerOwesFrom: OwedExpense[] }

// Nested under ['groups', groupId]: any group refresh also refreshes payments.
export const settlementKeys = {
  list: (groupId: string) => ['groups', groupId, 'settlements'] as const,
  detail: (groupId: string, settlementId: string) => ['groups', groupId, 'settlements', settlementId] as const,
}

const base = (groupId: string) => `/api/groups/${groupId}/settlements`

export const settlementsApi = {
  list: (groupId: string) => apiFetch<Settlement[]>(base(groupId)),
  get: (groupId: string, settlementId: string) => apiFetch<SettlementDetails>(`${base(groupId)}/${settlementId}`),
  /** Called by the payer ("I sent it", PENDING) or by the receiver ("I received it", CONFIRMED). */
  create: (groupId: string, fromUserId: string, toUserId: string, amount: number, note?: string) =>
    postJson<Settlement>(base(groupId), { fromUserId, toUserId, amount, note }),
  confirm: (groupId: string, settlementId: string) => postJson<Settlement>(`${base(groupId)}/${settlementId}/confirm`),
  reject: (groupId: string, settlementId: string) => postJson<Settlement>(`${base(groupId)}/${settlementId}/reject`),
}

/** Pending payments that only the logged-in user can answer. Used for the badge on the "Plăți" tab. */
export function waitingForAnswer(settlements: Settlement[], meId: string | undefined): Settlement[] {
  return settlements.filter((settlement) => settlement.status === 'PENDING' && settlement.to.id === meId)
}
