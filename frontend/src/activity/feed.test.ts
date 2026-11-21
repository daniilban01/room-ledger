import { describe, expect, it } from 'vitest'
import type { Settlement } from '../settlements/api'
import type { ActivityEvent } from './api'
import { buildFeed } from './feed'

// Midday UTC, so the calendar day is the same in any European time zone the tests might run in.
const event = (id: string, type: ActivityEvent['type'], createdAt: string, payload: Record<string, string> = {}) =>
  ({ id, type, actor: { id: 'a', displayName: 'Ana' }, payload, createdAt }) satisfies ActivityEvent

const settlement = (id: string, status: Settlement['status']) =>
  ({ id, status }) as Settlement

describe('buildFeed', () => {
  const marked = event('e2', 'SETTLEMENT_MARKED_PAID', '2026-11-20T12:00:00Z', { settlementId: 's1', toUserId: 'a' })
  const confirmed = event('e3', 'SETTLEMENT_CONFIRMED', '2026-11-20T13:00:00Z', { settlementId: 's1' })
  const expense = event('e1', 'EXPENSE_ADDED', '2026-11-19T12:00:00Z')

  it('shows a payment once, with its current status', () => {
    const days = buildFeed([confirmed, marked, expense], [settlement('s1', 'CONFIRMED')])

    const items = days.flatMap((day) => day.items)
    expect(items.map((item) => item.event.id)).toEqual(['e2', 'e1'])
    expect(items[0]).toMatchObject({ kind: 'payment', settlementId: 's1', status: 'CONFIRMED' })
  })

  it('groups events by day, newest day first', () => {
    const days = buildFeed([marked, expense], [])

    expect(days.map((day) => day.dayKey)).toEqual(['2026-11-20', '2026-11-19'])
  })

  it('falls back to the status from the event before settlements load', () => {
    const [day] = buildFeed([marked], [])

    expect(day.items[0]).toMatchObject({ status: 'PENDING' })
  })
})
