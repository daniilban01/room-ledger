import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../api/client'

type Health = { status: string }

export function HealthStatus() {
  const { data, isPending, isError } = useQuery({
    queryKey: ['health'],
    queryFn: () => apiFetch<Health>('/actuator/health'),
  })

  let text = 'Se verifică serverul…'
  let dotClass = 'bg-border'
  if (isError || (data && data.status !== 'UP')) {
    text = 'Serverul nu răspunde'
    dotClass = 'bg-negative'
  } else if (!isPending) {
    text = 'Serverul funcționează'
    dotClass = 'bg-accent'
  }

  return (
    <div role="status" className="flex items-center gap-3 rounded-2xl border border-border bg-surface p-4">
      <span aria-hidden="true" className={`size-3 rounded-full ${dotClass}`} />
      <span>{text}</span>
    </div>
  )
}
