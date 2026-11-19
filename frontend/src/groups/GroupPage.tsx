import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { ExpenseList } from '../expenses/ExpenseList'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { groupKeys, groupsApi } from './api'

export function GroupPage() {
  // The route is /groups/:groupId, so the param is always present here.
  const groupId = useParams().groupId!
  const queryClient = useQueryClient()

  const groupQuery = useQuery({
    queryKey: groupKeys.detail(groupId),
    queryFn: () => groupsApi.get(groupId),
  })

  const regenerateMutation = useMutation({
    mutationFn: () => groupsApi.regenerateJoinCode(groupId),
    onSuccess: (group) => queryClient.setQueryData(groupKeys.detail(groupId), group),
  })

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col gap-6 px-4 py-8">
      <Link
        to="/"
        className="flex min-h-11 items-center self-start font-medium text-accent underline-offset-2 hover:underline"
      >
        ← Grupurile mele
      </Link>

      {groupQuery.isPending && <p>Se încarcă…</p>}
      {groupQuery.isError && (
        <ErrorText message={errorMessage(groupQuery.error, { 404: 'Grupul nu există sau nu ai acces la el.' })} />
      )}

      {groupQuery.data && (
        <>
          <h1 className="font-display text-3xl font-bold">{groupQuery.data.name}</h1>

          <section className="flex flex-col gap-3 rounded-[20px] bg-ink p-5 text-white">
            <h2 className="text-sm text-white/70">Cod de invitație</h2>
            <p className="font-display text-3xl font-bold tracking-[0.2em]">{groupQuery.data.joinCode}</p>
            <p className="text-sm text-white/70">Trimite codul colegilor ca să intre în grup.</p>
            {groupQuery.data.myRole === 'ADMIN' && (
              <Button
                variant="secondary"
                className="self-start"
                disabled={regenerateMutation.isPending}
                onClick={() => regenerateMutation.mutate()}
              >
                Generează cod nou
              </Button>
            )}
            <ErrorText message={regenerateMutation.isError ? errorMessage(regenerateMutation.error) : null} />
          </section>

          <ExpenseList groupId={groupId} />

          <Card>
            <h2 className="mb-3 font-display text-xl font-bold">Membri ({groupQuery.data.members.length})</h2>
            <ul className="flex flex-col divide-y divide-border">
              {groupQuery.data.members.map((member) => (
                <li key={member.userId} className="flex min-h-11 items-center justify-between py-2">
                  <span>{member.displayName}</span>
                  {member.role === 'ADMIN' && <span className="text-sm text-ink/60">Admin</span>}
                </li>
              ))}
            </ul>
          </Card>
        </>
      )}
    </main>
  )
}
