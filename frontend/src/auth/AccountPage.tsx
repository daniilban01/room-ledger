import { errorMessage } from '../api/errorMessage'
import { Avatar } from '../ui/Avatar'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { useAuth } from './useAuth'
import { useMe } from './useMe'

export function AccountPage() {
  const { logout } = useAuth()
  const meQuery = useMe()

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-6">
      <PageHeader title="Cont" />

      {meQuery.isPending && <p className="text-ink/60">Se încarcă…</p>}
      {meQuery.isError && <ErrorText message={errorMessage(meQuery.error)} />}
      {meQuery.data && (
        <Card className="flex items-center gap-4">
          <Avatar name={meQuery.data.displayName} id={meQuery.data.id} />
          <div className="min-w-0">
            <p className="truncate font-semibold">{meQuery.data.displayName}</p>
            <p className="truncate text-ink/60">{meQuery.data.email}</p>
          </div>
        </Card>
      )}

      <Button variant="secondary" onClick={logout}>
        Ieși din cont
      </Button>
    </main>
  )
}
