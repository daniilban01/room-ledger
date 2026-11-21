import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { centsFromApi, formatAmount, formatDate } from '../format/format'
import { Avatar } from '../ui/Avatar'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { Section } from '../ui/Section'
import { expenseKeys, expensesApi } from './api'

export function ExpenseList({ groupId }: { groupId: string }) {
  const expensesQuery = useQuery({
    queryKey: expenseKeys.list(groupId),
    queryFn: () => expensesApi.list(groupId),
  })
  const expenses = expensesQuery.data?.content ?? []

  const addLink = (
    <Link
      to={`/groups/${groupId}/expenses/new`}
      className="flex min-h-11 items-center rounded-full px-3 text-sm font-semibold text-accent hover:bg-accent/10 focus-visible:outline-2 focus-visible:outline-accent"
    >
      + Adaugă
    </Link>
  )

  return (
    <Section title="Cheltuieli" action={addLink}>
      {expensesQuery.isPending && <p className="px-1 text-ink/60">Se încarcă…</p>}
      {expensesQuery.isError && <ErrorText message={errorMessage(expensesQuery.error)} />}

      {expensesQuery.data && expenses.length === 0 && (
        <Card>
          <p className="text-ink/70">Nicio cheltuială încă. Adaugă prima, de exemplu cumpărăturile de azi.</p>
        </Card>
      )}

      {expenses.length > 0 && (
        <Card padded={false}>
          <ul className="divide-y divide-border">
            {expenses.map((expense) => (
              <li key={expense.id}>
                <Link
                  to={`/groups/${groupId}/expenses/${expense.id}`}
                  className="flex min-h-16 items-center gap-3 px-4 py-3 hover:bg-page focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-accent"
                >
                  <Avatar name={expense.paidBy.displayName} id={expense.paidBy.id} />
                  <span className="flex min-w-0 flex-1 flex-col">
                    <span className="truncate font-medium">{expense.description}</span>
                    <span className="truncate text-sm text-ink/60">
                      {expense.paidBy.displayName} a plătit · {formatDate(expense.expenseDate)}
                    </span>
                  </span>
                  <span className="shrink-0 font-display font-bold">{formatAmount(centsFromApi(expense.totalAmount))}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {expensesQuery.data && expensesQuery.data.totalElements > expenses.length && (
        <p className="px-1 text-sm text-ink/60">
          Se afișează cele mai recente {expenses.length} din {expensesQuery.data.totalElements}.
        </p>
      )}
    </Section>
  )
}
