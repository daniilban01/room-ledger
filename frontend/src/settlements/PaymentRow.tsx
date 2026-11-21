import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { centsFromApi, formatAmount, formatDateTime } from '../format/format'
import { Avatar } from '../ui/Avatar'
import type { Settlement } from './api'
import { PaymentStatus } from './PaymentStatus'

type Props = {
  groupId: string
  settlement: Settlement
  meId: string | undefined
  /** Buttons shown under the row. Kept outside the link: a button inside a link is invalid HTML. */
  actions?: ReactNode
}

export function PaymentRow({ groupId, settlement, meId, actions }: Props) {
  const name = (user: Settlement['from']) => (user.id === meId ? 'Tu' : user.displayName)

  return (
    <li>
      <Link
        to={`/groups/${groupId}/payments/${settlement.id}`}
        className="flex gap-3 px-4 py-3 hover:bg-page focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-accent"
      >
        <Avatar name={settlement.from.displayName} id={settlement.from.id} />
        <span className="flex min-w-0 flex-1 flex-col gap-1">
          <span className="flex items-baseline justify-between gap-3">
            <span className="truncate">
              {name(settlement.from)} <span aria-hidden="true">→</span>
              <span className="sr-only">către</span> {name(settlement.to)}
            </span>
            <span className="shrink-0 font-display font-bold">{formatAmount(centsFromApi(settlement.amount))}</span>
          </span>
          {settlement.note && <span className="truncate text-sm text-ink/70">„{settlement.note}”</span>}
          {!actions && (
            <span className="flex items-center gap-2">
              <PaymentStatus status={settlement.status} canAnswer={false} />
              <span className="text-xs text-ink/50">{formatDateTime(settlement.resolvedAt ?? settlement.createdAt)}</span>
            </span>
          )}
        </span>
      </Link>
      {actions && <div className="px-4 pb-3 pl-17">{actions}</div>}
    </li>
  )
}
