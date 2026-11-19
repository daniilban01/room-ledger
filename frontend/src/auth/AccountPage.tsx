import { errorMessage } from '../api/errorMessage'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { useAuth } from './useAuth'
import { useMe } from './useMe'

export function AccountPage() {
  const { logout } = useAuth()
  const meQuery = useMe()

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-8">
      <h1 className="font-display text-3xl font-bold">Cont</h1>

      {meQuery.isPending && <p>Se încarcă…</p>}
      {meQuery.isError && <ErrorText message={errorMessage(meQuery.error)} />}
      {meQuery.data && (
        <Card className="flex flex-col gap-1">
          <p className="font-display text-xl font-bold">{meQuery.data.displayName}</p>
          <p className="text-ink/70">{meQuery.data.email}</p>
        </Card>
      )}

      <Button variant="secondary" onClick={logout}>
        Ieși din cont
      </Button>
    </main>
  )
}
