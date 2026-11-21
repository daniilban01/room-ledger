import type { ReactNode } from 'react'

type Props = { children: ReactNode; className?: string; padded?: boolean }

/** padded={false} for lists whose rows bring their own padding (so the dividers reach the edges). */
export function Card({ children, className = '', padded = true }: Props) {
  return (
    <div className={`rounded-[20px] border border-border bg-surface ${padded ? 'p-5' : 'overflow-hidden'} ${className}`}>
      {children}
    </div>
  )
}
