import type { ReactNode } from 'react'

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-6 px-4 py-10">
      <div>
        <p className="font-display text-lg font-bold text-accent">RoomLedger</p>
        <h1 className="font-display text-3xl font-bold">{title}</h1>
      </div>
      {children}
    </main>
  )
}
