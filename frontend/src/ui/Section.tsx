import type { ReactNode } from 'react'

type Props = { title: string; action?: ReactNode; children: ReactNode }

/** A small label above the content (like grouped lists in phone settings) instead of a big heading in every card. */
export function Section({ title, action, children }: Props) {
  return (
    <section className="flex flex-col gap-2">
      <div className="flex min-h-11 items-center justify-between gap-4 px-1">
        <h2 className="text-sm font-semibold text-ink/60">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}
