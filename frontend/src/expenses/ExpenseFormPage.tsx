import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { groupKeys, groupsApi } from '../groups/api'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { expenseKeys, expensesApi } from './api'
import { ExpenseForm } from './ExpenseForm'

/** Loads everything the form needs, then renders it. Used for both /expenses/new and /expenses/:id. */
export function ExpenseFormPage() {
  const params = useParams()
  const groupId = params.groupId!
  const expenseId = params.expenseId

  const meQuery = useMe()
  const groupQuery = useQuery({ queryKey: groupKeys.detail(groupId), queryFn: () => groupsApi.get(groupId) })
  const expenseQuery = useQuery({
    queryKey: expenseKeys.detail(groupId, expenseId ?? ''),
    queryFn: () => expensesApi.get(groupId, expenseId!),
    enabled: expenseId !== undefined,
    // Refetching on tab focus would remount the form with someone else's newer version and
    // silently throw away the user's unsaved edits. Instead we keep the version they started
    // from; if it is outdated, saving returns 409 and we tell them.
    refetchOnWindowFocus: false,
  })

  const error = meQuery.error ?? groupQuery.error ?? expenseQuery.error
  const loading = meQuery.isPending || groupQuery.isPending || (expenseId !== undefined && expenseQuery.isPending)

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-6">
      <PageHeader title={expenseId ? 'Editează cheltuiala' : 'Cheltuială nouă'} backTo={`/groups/${groupId}`} />

      {error && <ErrorText message={errorMessage(error, { 404: 'Cheltuiala nu există sau a fost ștearsă.' })} />}
      {!error && loading && <p>Se încarcă…</p>}
      {!error && !loading && meQuery.data && groupQuery.data && (
        // The key remounts the form (fresh initial state) if a newer version of the expense arrives.
        <ExpenseForm
          key={expenseQuery.data?.version ?? 'new'}
          me={meQuery.data}
          group={groupQuery.data}
          expense={expenseQuery.data}
        />
      )}
    </main>
  )
}
