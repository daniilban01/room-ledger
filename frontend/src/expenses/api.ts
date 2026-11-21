import { apiFetch, postJson } from '../api/client'

export type SplitType = 'EQUAL' | 'EXACT' | 'ITEMS'

export type UserSummary = { id: string; displayName: string }

export type ExpenseSummary = {
  id: string
  description: string
  totalAmount: number
  paidBy: UserSummary
  expenseDate: string
  splitType: SplitType
}

export type Expense = ExpenseSummary & {
  source: 'MANUAL' | 'RECEIPT'
  createdBy: UserSummary
  createdAt: string
  updatedAt: string
  version: number
  shares: { userId: string; displayName: string; amount: number }[]
  items: { id: string; name: string; amount: number; assignedTo: string[] }[]
}

export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ExpenseRequest = {
  description: string
  totalAmount: number
  paidBy: string
  expenseDate: string
  splitType: SplitType
  participants?: string[]
  shares?: { userId: string; amount: number }[]
  items?: { name: string; amount: number; assignedTo: string[] }[]
  version?: number
  source?: 'MANUAL' | 'RECEIPT'
}

// Nested under ['groups', groupId], so invalidating a group also refreshes its expenses.
export const expenseKeys = {
  list: (groupId: string) => ['groups', groupId, 'expenses'] as const,
  detail: (groupId: string, expenseId: string) => ['groups', groupId, 'expenses', expenseId] as const,
}

const base = (groupId: string) => `/api/groups/${groupId}/expenses`

export const expensesApi = {
  list: (groupId: string) => apiFetch<Page<ExpenseSummary>>(`${base(groupId)}?page=0&size=20`),
  get: (groupId: string, expenseId: string) => apiFetch<Expense>(`${base(groupId)}/${expenseId}`),
  create: (groupId: string, request: ExpenseRequest) => postJson<Expense>(base(groupId), request),
  update: (groupId: string, expenseId: string, request: ExpenseRequest) =>
    apiFetch<Expense>(`${base(groupId)}/${expenseId}`, { method: 'PUT', body: JSON.stringify(request) }),
  remove: (groupId: string, expenseId: string) =>
    apiFetch<void>(`${base(groupId)}/${expenseId}`, { method: 'DELETE' }),
}
