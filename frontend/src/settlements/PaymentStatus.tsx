import { Button } from '../ui/Button'
import type { SettlementStatus } from './api'

const pills: Record<SettlementStatus, { label: string; className: string }> = {
  PENDING: { label: 'Așteaptă confirmarea', className: 'bg-page text-ink/70' },
  CONFIRMED: { label: 'Primit', className: 'bg-accent/10 text-accent' },
  REJECTED: { label: 'Respins', className: 'bg-negative/10 text-negative' },
}

type Props = {
  status: SettlementStatus
  /** True when the logged-in user is the one receiving the money: only they can confirm or reject. */
  canAnswer: boolean
  busy?: boolean
  onConfirm?: () => void
  onReject?: () => void
}

/** A status label, or (for the receiver of a pending payment) the two answer buttons. */
export function PaymentStatus({ status, canAnswer, busy = false, onConfirm, onReject }: Props) {
  if (status === 'PENDING' && canAnswer) {
    return (
      <div className="flex gap-2">
        <Button size="sm" disabled={busy} onClick={onConfirm}>
          Am primit
        </Button>
        <Button size="sm" variant="secondary" disabled={busy} onClick={onReject}>
          Nu am primit
        </Button>
      </div>
    )
  }
  const pill = pills[status]
  return (
    <span className={`inline-flex self-start rounded-full px-3 py-1 text-xs font-semibold ${pill.className}`}>
      {pill.label}
    </span>
  )
}
