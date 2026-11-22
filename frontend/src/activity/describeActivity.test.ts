import { describe, expect, it } from 'vitest'
import type { ActivityEvent } from './api'
import { describeActivity } from './describeActivity'

const event = (type: ActivityEvent['type'], payload: Record<string, string>): ActivityEvent => ({
  id: '1',
  type,
  actor: { id: 'a', displayName: 'Ana' },
  payload,
  createdAt: '2026-11-20T10:00:00Z',
})

const plain = (text: string) => text.replace(/\s/g, ' ')

describe('describeActivity', () => {
  it('describes an added expense with its amount', () => {
    expect(plain(describeActivity(event('EXPENSE_ADDED', { description: 'Cumpărături', amount: '1234.50' })))).toBe(
      'Ana a adăugat „Cumpărături” · 1.234,50 lei',
    )
  })

  it('describes a payment the same way, whoever recorded it', () => {
    const payload = { fromName: 'Bogdan', toName: 'Ana', amount: '15.00' }
    const expected = 'Bogdan a trimis 15,00 lei către Ana'
    expect(plain(describeActivity(event('SETTLEMENT_MARKED_PAID', payload)))).toBe(expected)
    expect(plain(describeActivity(event('SETTLEMENT_RECORDED', payload)))).toBe(expected)
  })

  it('describes a rename with the old and the new name', () => {
    expect(describeActivity(event('GROUP_RENAMED', { oldName: 'Apartament', newName: 'Apartament 12' }))).toBe(
      'Ana a redenumit grupul din „Apartament” în „Apartament 12”',
    )
  })
})
