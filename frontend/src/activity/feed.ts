import { localDateKey } from '../format/format'
import type { Settlement, SettlementStatus } from '../settlements/api'
import type { ActivityEvent } from './api'

export type FeedItem =
  | { kind: 'event'; event: ActivityEvent }
  | { kind: 'payment'; event: ActivityEvent; settlementId: string; toUserId: string; status: SettlementStatus }

export type FeedDay = { dayKey: string; items: FeedItem[] }

/**
 * Turns the raw activity log into what the user sees.
 *
 * The backend log is append-only and complete (a payment produces "marked as paid" and later
 * "confirmed"/"rejected"): that is the audit trail. For reading, one payment is one row: we keep the
 * event that created it and show its CURRENT status from the settlements list, dropping the
 * confirmed/rejected events. Events are grouped by day; the input is already newest first.
 */
export function buildFeed(events: ActivityEvent[], settlements: Settlement[]): FeedDay[] {
  const statusById = new Map(settlements.map((settlement) => [settlement.id, settlement.status]))
  const days: FeedDay[] = []

  for (const event of events) {
    if (event.type === 'SETTLEMENT_CONFIRMED' || event.type === 'SETTLEMENT_REJECTED') {
      continue
    }

    let item: FeedItem = { kind: 'event', event }
    if (event.type === 'SETTLEMENT_MARKED_PAID' || event.type === 'SETTLEMENT_RECORDED') {
      const settlementId = event.payload.settlementId
      // Until the settlements list has loaded, fall back to the status the event was created with.
      const fallback: SettlementStatus = event.type === 'SETTLEMENT_RECORDED' ? 'CONFIRMED' : 'PENDING'
      item = {
        kind: 'payment',
        event,
        settlementId,
        toUserId: event.payload.toUserId,
        status: statusById.get(settlementId) ?? fallback,
      }
    }

    const dayKey = localDateKey(new Date(event.createdAt))
    const lastDay = days.at(-1)
    if (lastDay?.dayKey === dayKey) {
      lastDay.items.push(item)
    } else {
      days.push({ dayKey, items: [item] })
    }
  }
  return days
}
