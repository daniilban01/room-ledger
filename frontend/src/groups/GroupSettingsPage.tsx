import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { activityKeys } from '../activity/api'
import { errorMessage } from '../api/errorMessage'
import { balanceKeys, balancesApi } from '../balances/api'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { Section } from '../ui/Section'
import { TextField } from '../ui/TextField'
import { groupKeys, groupsApi, type GroupDetails } from './api'

export function GroupSettingsPage() {
  const groupId = useParams().groupId!

  const groupQuery = useQuery({
    queryKey: groupKeys.detail(groupId),
    queryFn: () => groupsApi.get(groupId),
  })
  const group = groupQuery.data

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 pt-6 pb-8">
      <PageHeader title="Setări grup" backTo={`/groups/${groupId}`} />

      {groupQuery.isPending && <p className="text-ink/60">Se încarcă…</p>}
      {groupQuery.isError && (
        <ErrorText message={errorMessage(groupQuery.error, { 404: 'Grupul nu există sau nu ai acces la el.' })} />
      )}

      {group && group.myRole !== 'ADMIN' && (
        <p className="text-ink/70">Doar administratorii grupului pot schimba setările.</p>
      )}

      {group?.myRole === 'ADMIN' && (
        <>
          {/* key: if the name changes elsewhere, the form starts again from the new name. */}
          <RenameCard key={group.name} group={group} />
          <DeleteCard group={group} />
        </>
      )}
    </main>
  )
}

function RenameCard({ group }: { group: GroupDetails }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState(group.name)
  const [saved, setSaved] = useState(false)

  const renameMutation = useMutation({
    mutationFn: (newName: string) => groupsApi.rename(group.id, newName),
    onSuccess: (updated) => {
      queryClient.setQueryData(groupKeys.detail(group.id), updated)
      // exact: refresh the group list, not every query that starts with ['groups'].
      void queryClient.invalidateQueries({ queryKey: groupKeys.all, exact: true })
      void queryClient.invalidateQueries({ queryKey: activityKeys.list(group.id) })
      setSaved(true)
    },
  })

  const trimmed = name.trim()
  const unchanged = trimmed === group.name

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (trimmed && !unchanged) {
      renameMutation.mutate(trimmed)
    }
  }

  return (
    <Section title="Nume">
      <Card>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <TextField
            label="Numele grupului"
            value={name}
            maxLength={100}
            required
            onChange={(event) => {
              setName(event.target.value)
              setSaved(false)
            }}
          />
          <div className="flex items-center gap-3">
            <Button type="submit" disabled={!trimmed || unchanged || renameMutation.isPending}>
              Salvează
            </Button>
            {saved && unchanged && <p className="text-sm text-ink/60">Salvat.</p>}
          </div>
          <ErrorText message={renameMutation.isError ? errorMessage(renameMutation.error) : null} />
        </form>
      </Card>
    </Section>
  )
}

function DeleteCard({ group }: { group: GroupDetails }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)
  const [typedName, setTypedName] = useState('')

  const balancesQuery = useQuery({
    queryKey: balanceKeys.balances(group.id),
    queryFn: () => balancesApi.get(group.id),
  })
  const hasOpenDebts = balancesQuery.data?.members.some((member) => member.balance !== 0) ?? false

  const deleteMutation = useMutation({
    mutationFn: () => groupsApi.remove(group.id),
    onSuccess: () => {
      navigate('/', { replace: true })
      // Drop everything cached for this group (details, expenses, balances, payments, activity):
      // it no longer exists, and refetching any of it would only return 404.
      queryClient.removeQueries({ queryKey: groupKeys.detail(group.id) })
      void queryClient.invalidateQueries({ queryKey: groupKeys.all, exact: true })
    },
  })

  return (
    <Section title="Zonă periculoasă">
      <Card className="flex flex-col gap-3">
        {!confirming ? (
          <>
            <p className="text-ink/70">
              Grupul dispare pentru toți membrii, împreună cu cheltuielile, plățile și istoricul lui.
            </p>
            <Button variant="danger" className="self-start" onClick={() => setConfirming(true)}>
              Șterge grupul
            </Button>
          </>
        ) : (
          <form
            className="flex flex-col gap-3"
            onSubmit={(event) => {
              event.preventDefault()
              deleteMutation.mutate()
            }}
          >
            {hasOpenDebts && (
              <p className="rounded-xl bg-negative/10 p-3 text-sm text-negative">
                Atenție: în grup mai sunt datorii nedecontate. După ștergere nu le mai vede nimeni.
              </p>
            )}
            <TextField
              label={`Scrie „${group.name}” ca să confirmi`}
              value={typedName}
              autoComplete="off"
              onChange={(event) => setTypedName(event.target.value)}
            />
            <div className="flex flex-wrap gap-3">
              <Button
                type="submit"
                variant="danger"
                disabled={typedName.trim() !== group.name || deleteMutation.isPending}
              >
                Șterge definitiv
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setConfirming(false)
                  setTypedName('')
                }}
              >
                Renunță
              </Button>
            </div>
            <ErrorText message={deleteMutation.isError ? errorMessage(deleteMutation.error) : null} />
          </form>
        )}
      </Card>
    </Section>
  )
}
