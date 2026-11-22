import { apiFetch } from '../api/client'
import type { Page, UserSummary } from '../expenses/api'

export type ActivityType =
  | 'GROUP_CREATED'
  | 'GROUP_RENAMED'
  | 'GROUP_DELETED'
  | 'MEMBER_JOINED'
  | 'EXPENSE_ADDED'
  | 'EXPENSE_EDITED'
  | 'EXPENSE_DELETED'
  | 'SETTLEMENT_MARKED_PAID'
  | 'SETTLEMENT_RECORDED'
  | 'SETTLEMENT_CONFIRMED'
  | 'SETTLEMENT_REJECTED'

export type ActivityEvent = {
  id: string
  type: ActivityType
  actor: UserSummary
  // Snapshot written by the backend; amounts are strings ("120.50") to keep them exact.
  payload: Record<string, string>
  createdAt: string
}

export const activityKeys = {
  list: (groupId: string) => ['groups', groupId, 'activity'] as const,
}

export const activityApi = {
  list: (groupId: string, page: number) =>
    apiFetch<Page<ActivityEvent>>(`/api/groups/${groupId}/activity?page=${page}&size=30`),
}
