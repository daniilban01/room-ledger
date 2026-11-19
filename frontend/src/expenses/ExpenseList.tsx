import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { centsFromApi, formatAmount, formatDate } from '../format/format'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { expenseKeys, expensesApi } from './api'

export function ExpenseList({ groupId }: { groupId: string }) {
  const expensesQuery = useQuery({
    queryKey: expenseKeys.list(groupId),
    queryFn: () => expensesApi.list(groupId),
  })

  return (
    <Card>
      <div className="mb-3 flex items-center justify-between gap-4">
        <h2 className="font-display text-xl font-bold">Cheltuieli</h2>
        <Link
          to={`/groups/${groupId}/expenses/new`}
          className="flex min-h-11 items-center rounded-xl bg-accent px-4 font-medium text-white hover:bg-accent/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
        >
          + Adaugă
        </Link>
      </div>

      {expensesQuery.isPending && <p>Se încarcă…</p>}
      {expensesQuery.isError && <ErrorText message={errorMessage(expensesQuery.error)} />}
      {expensesQuery.data?.content.length === 0 && <p className="text-ink/70">Nicio cheltuială încă.</p>}

      <ul className="flex flex-col divide-y divide-border">
        {expensesQuery.data?.content.map((expense) => (
          <li key={expense.id}>
            <Link
              to={`/groups/${groupId}/expenses/${expense.id}`}
              className="flex min-h-14 items-center justify-between gap-4 py-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
            >
              <span className="flex flex-col">
                <span className="font-medium">{expense.description}</span>
                <span className="text-sm text-ink/60">
                  {formatDate(expense.expenseDate)} · a plătit {expense.paidBy.displayName}
                </span>
              </span>
              <span className="shrink-0 font-display font-bold">{formatAmount(centsFromApi(expense.totalAmount))}</span>
            </Link>
          </li>
        ))}
      </ul>

      {expensesQuery.data && expensesQuery.data.totalElements > expensesQuery.data.content.length && (
        <p className="mt-3 text-sm text-ink/60">
          Se afișează cele mai recente {expensesQuery.data.content.length} din {expensesQuery.data.totalElements}.
        </p>
      )}
    </Card>
  )
}
