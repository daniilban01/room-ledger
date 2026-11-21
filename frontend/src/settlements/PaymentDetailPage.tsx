import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { centsFromApi, formatAmount, formatDate, formatDateTime } from '../format/format'
import { Avatar } from '../ui/Avatar'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { Section } from '../ui/Section'
import { settlementKeys, settlementsApi } from './api'
import { PaymentStatus } from './PaymentStatus'
import { useSettlementActions } from './useSettlementActions'

export function PaymentDetailPage() {
  const params = useParams()
  const groupId = params.groupId!
  const settlementId = params.settlementId!
  const meId = useMe().data?.id
  const actions = useSettlementActions(groupId)

  const detailsQuery = useQuery({
    queryKey: settlementKeys.detail(groupId, settlementId),
    queryFn: () => settlementsApi.get(groupId, settlementId),
  })

  const settlement = detailsQuery.data?.settlement
  const owed = detailsQuery.data?.payerOwesFrom ?? []

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-6">
      <PageHeader title="Plată" backTo={`/groups/${groupId}/payments`} />

      {detailsQuery.isPending && <p className="text-ink/60">Se încarcă…</p>}
      {detailsQuery.isError && (
        <ErrorText message={errorMessage(detailsQuery.error, { 404: 'Plata nu există sau nu ai acces la ea.' })} />
      )}
      <ErrorText message={actions.errorText} />

      {settlement && (
        <>
          <Card className="flex flex-col items-center gap-4 text-center">
            <div className="flex items-center gap-3">
              <Avatar name={settlement.from.displayName} id={settlement.from.id} />
              <span aria-hidden="true" className="text-ink/40">
                →
              </span>
              <Avatar name={settlement.to.displayName} id={settlement.to.id} />
            </div>
            <p className="font-display text-4xl font-bold">{formatAmount(centsFromApi(settlement.amount))}</p>
            <p className="text-ink/70">
              <span className="font-medium text-ink">{settlement.from.displayName}</span> a trimis către{' '}
              <span className="font-medium text-ink">{settlement.to.displayName}</span>
            </p>
            {settlement.note && <p className="rounded-xl bg-page px-4 py-2 italic">„{settlement.note}”</p>}
            <div className="flex justify-center">
              <PaymentStatus
                status={settlement.status}
                canAnswer={settlement.to.id === meId}
                busy={actions.busy}
                onConfirm={() => actions.confirm(settlement.id)}
                onReject={() => actions.reject(settlement.id)}
              />
            </div>
          </Card>

          <Section title="Detalii">
            <Card padded={false}>
              <dl className="divide-y divide-border">
                <div className="flex justify-between gap-4 px-4 py-3">
                  <dt className="text-ink/60">Trimisă</dt>
                  <dd>{formatDateTime(settlement.createdAt)}</dd>
                </div>
                {settlement.resolvedAt && (
                  <div className="flex justify-between gap-4 px-4 py-3">
                    <dt className="text-ink/60">{settlement.status === 'REJECTED' ? 'Respinsă' : 'Confirmată'}</dt>
                    <dd>{formatDateTime(settlement.resolvedAt)}</dd>
                  </div>
                )}
              </dl>
            </Card>
          </Section>

          <Section title="Din ce vine suma">
            {/* A simplified transfer can combine many expenses (and even other people), so we do not
                pretend it comes from one expense: we show where the payer owes money. */}
            <p className="px-1 text-sm text-ink/70">
              Plățile sunt calculate din soldul total al grupului, așa că nu corespund unei singure cheltuieli.
              {owed.length > 0
                ? ` Acestea sunt cheltuielile în care ${settlement.from.displayName} are de dat bani:`
                : ` Acum ${settlement.from.displayName} nu mai are de dat bani din nicio cheltuială.`}
            </p>
            {owed.length > 0 && (
              <Card padded={false}>
                <ul className="divide-y divide-border">
                  {owed.map((expense) => (
                    <li key={expense.expenseId}>
                      <Link
                        to={`/groups/${groupId}/expenses/${expense.expenseId}`}
                        className="flex items-center gap-3 px-4 py-3 hover:bg-page focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-accent"
                      >
                        <span className="flex min-w-0 flex-1 flex-col">
                          <span className="truncate font-medium">{expense.description}</span>
                          <span className="truncate text-sm text-ink/60">
                            a plătit {expense.paidBy.displayName} · {formatDate(expense.expenseDate)}
                          </span>
                        </span>
                        <span className="shrink-0 text-right">
                          <span className="block font-display font-bold">{formatAmount(centsFromApi(expense.share))}</span>
                          <span className="block text-xs text-ink/50">de dat</span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </Card>
            )}
          </Section>
        </>
      )}
    </main>
  )
}
