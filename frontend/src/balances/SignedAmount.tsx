import { formatSignedAmount } from '../format/format'

type Props = { cents: number; onDark?: boolean; className?: string }

/** Positive in blue with "+", negative in orange with "−": the sign carries the meaning, color only reinforces it. */
export function SignedAmount({ cents, onDark = false, className = '' }: Props) {
  let color = onDark ? 'text-white' : 'text-ink'
  if (cents > 0) {
    color = onDark ? 'text-positive-on-dark' : 'text-accent'
  } else if (cents < 0) {
    color = onDark ? 'text-negative-on-dark' : 'text-negative'
  }
  return <span className={`font-display font-bold whitespace-nowrap ${color} ${className}`}>{formatSignedAmount(cents)}</span>
}
