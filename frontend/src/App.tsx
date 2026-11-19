import { HealthStatus } from './health/HealthStatus'

export default function App() {
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-6 px-4">
      <h1 className="font-display text-4xl font-bold">RoomLedger</h1>
      <p>Cheltuieli comune între colegii de apartament.</p>
      <HealthStatus />
    </main>
  )
}
