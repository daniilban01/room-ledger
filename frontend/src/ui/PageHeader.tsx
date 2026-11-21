import type { ReactNode } from 'react'
import { Link } from 'react-router'

type Props = { title: string; backTo?: string; action?: ReactNode }

export function PageHeader({ title, backTo, action }: Props) {
  return (
    <header className="flex items-center gap-2">
      {backTo && (
        <Link
          to={backTo}
          aria-label="Înapoi"
          className="-ml-2 grid size-11 shrink-0 place-items-center rounded-full text-ink hover:bg-surface focus-visible:outline-2 focus-visible:outline-accent"
        >
          <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth={2} aria-hidden="true">
            <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
      )}
      <h1 className="min-w-0 flex-1 truncate font-display text-2xl font-bold">{title}</h1>
      {action}
    </header>
  )
}
