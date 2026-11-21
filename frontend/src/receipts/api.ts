import { apiFetch } from '../api/client'

export type ReceiptWarningCode = 'ITEMS_DROPPED' | 'TOTAL_MISSING' | 'TOTAL_MISMATCH'

export type ReceiptDraft = {
  storeName: string | null
  date: string | null
  items: { name: string; amount: number }[]
  total: number | null
  itemsTotal: number
  warnings: { code: ReceiptWarningCode; message: string }[]
}

export const receiptsApi = {
  parse: (groupId: string, image: Blob) => {
    const form = new FormData()
    form.append('image', image, 'receipt.jpg')
    return apiFetch<ReceiptDraft>(`/api/groups/${groupId}/receipts/parse`, { method: 'POST', body: form })
  },
}
