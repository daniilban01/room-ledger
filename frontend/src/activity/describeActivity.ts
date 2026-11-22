import { centsFromApi, formatAmount } from '../format/format'
import type { ActivityEvent } from './api'

function amount(event: ActivityEvent): string {
  return formatAmount(centsFromApi(Number(event.payload.amount ?? 0)))
}

/** One readable Romanian sentence per event, built from the snapshot stored in the payload. */
export function describeActivity(event: ActivityEvent): string {
  const actor = event.actor.displayName
  const { description, fromName, toName, groupName, oldName, newName } = event.payload

  switch (event.type) {
    case 'GROUP_CREATED':
      return `${actor} a creat grupul „${groupName}”`
    case 'GROUP_RENAMED':
      return `${actor} a redenumit grupul din „${oldName}” în „${newName}”`
    // Normally never shown (a deleted group is no longer reachable), but the type must be handled.
    case 'GROUP_DELETED':
      return `${actor} a șters grupul „${groupName}”`
    case 'MEMBER_JOINED':
      return `${actor} a intrat în grup`
    case 'EXPENSE_ADDED':
      return `${actor} a adăugat „${description}” · ${amount(event)}`
    case 'EXPENSE_EDITED':
      return `${actor} a modificat „${description}” · ${amount(event)}`
    case 'EXPENSE_DELETED':
      return `${actor} a șters „${description}” · ${amount(event)}`
    // Who clicked the button does not matter to the reader: a payment always reads "X sent Y".
    // Its status (waiting / received / rejected) is shown next to it.
    case 'SETTLEMENT_MARKED_PAID':
    case 'SETTLEMENT_RECORDED':
      return `${fromName} a trimis ${amount(event)} către ${toName}`
    case 'SETTLEMENT_CONFIRMED':
      return `${toName} a confirmat plata de ${amount(event)} de la ${fromName}`
    case 'SETTLEMENT_REJECTED':
      return `${toName} a respins plata de ${amount(event)} de la ${fromName}`
  }
}
