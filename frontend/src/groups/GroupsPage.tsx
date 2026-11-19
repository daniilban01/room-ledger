import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useAuth } from '../auth/useAuth'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { TextField } from '../ui/TextField'
import { groupKeys, groupsApi, type GroupDetails } from './api'

export function GroupsPage() {
  const { logout } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [newGroupName, setNewGroupName] = useState('')
  const [joinCode, setJoinCode] = useState('')

  const groupsQuery = useQuery({ queryKey: groupKeys.all, queryFn: groupsApi.list })

  // After creating or joining we already have the full group, so we put it in the cache
  // and the group page opens without another request.
  const openGroup = (group: GroupDetails) => {
    queryClient.setQueryData(groupKeys.detail(group.id), group)
    void queryClient.invalidateQueries({ queryKey: groupKeys.all, exact: true })
    navigate(`/groups/${group.id}`)
  }

  const createMutation = useMutation({ mutationFn: groupsApi.create, onSuccess: openGroup })
  const joinMutation = useMutation({ mutationFn: groupsApi.join, onSuccess: openGroup })

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col gap-6 px-4 py-8">
      <header className="flex items-center justify-between gap-4">
        <h1 className="font-display text-3xl font-bold">Grupurile mele</h1>
        <Button variant="secondary" onClick={logout}>
          Ieși
        </Button>
      </header>

      <section aria-label="Lista grupurilor" className="flex flex-col gap-3">
        {groupsQuery.isPending && <p>Se încarcă…</p>}
        {groupsQuery.isError && <ErrorText message={errorMessage(groupsQuery.error)} />}
        {groupsQuery.data?.length === 0 && (
          <p className="text-ink/70">Nu ești în niciun grup încă. Creează unul sau intră cu un cod.</p>
        )}
        {groupsQuery.data?.map((group) => (
          <Link
            key={group.id}
            to={`/groups/${group.id}`}
            className="flex min-h-14 items-center justify-between rounded-[20px] border border-border bg-surface px-5 py-4 font-medium hover:border-accent focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
          >
            <span>{group.name}</span>
            {group.myRole === 'ADMIN' && <span className="text-sm text-ink/60">Admin</span>}
          </Link>
        ))}
      </section>

      <Card>
        <form
          className="flex flex-col gap-3"
          onSubmit={(event) => {
            event.preventDefault()
            createMutation.mutate(newGroupName)
          }}
        >
          <h2 className="font-display text-xl font-bold">Grup nou</h2>
          <TextField
            label="Nume grup"
            placeholder="ex. Apartament Str. Lalelelor"
            required
            maxLength={100}
            value={newGroupName}
            onChange={(event) => setNewGroupName(event.target.value)}
          />
          <ErrorText message={createMutation.isError ? errorMessage(createMutation.error) : null} />
          <Button type="submit" disabled={createMutation.isPending}>
            Creează grup
          </Button>
        </form>
      </Card>

      <Card>
        <form
          className="flex flex-col gap-3"
          onSubmit={(event) => {
            event.preventDefault()
            joinMutation.mutate(joinCode)
          }}
        >
          <h2 className="font-display text-xl font-bold">Intră într-un grup</h2>
          <TextField
            label="Cod de invitație"
            placeholder="ex. K7M2QX9P"
            autoCapitalize="characters"
            autoComplete="off"
            required
            maxLength={16}
            value={joinCode}
            onChange={(event) => setJoinCode(event.target.value)}
          />
          <ErrorText
            message={joinMutation.isError ? errorMessage(joinMutation.error, { 404: 'Codul nu este valid.' }) : null}
          />
          <Button type="submit" variant="secondary" disabled={joinMutation.isPending}>
            Intră în grup
          </Button>
        </form>
      </Card>
    </main>
  )
}
