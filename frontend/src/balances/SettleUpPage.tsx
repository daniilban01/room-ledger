import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { centsFromApi, formatAmount } from '../format/format'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { balanceKeys, balancesApi } from './api'

/** Suggested transfers that settle the whole group (computed by DebtSimplifier on the server). */
export function SettleUpPage() {
  const groupId = useParams().groupId!
  const meQuery = useMe()
  const transfersQuery = useQuery({
    queryKey: balanceKeys.simplified(groupId),
    queryFn: () => balancesApi.simplified(groupId),
  })

  const meId = meQuery.data?.id
  const transfers = transfersQuery.data?.transfers ?? []
  const name = (user: { id: string; displayName: string }) => (user.id === meId ? 'Tu' : user.displayName)

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-8">
      <Link
        to={`/groups/${groupId}`}
        className="flex min-h-11 items-center self-start font-medium text-accent underline-offset-2 hover:underline"
      >
        ← Înapoi la grup
      </Link>
      <h1 className="font-display text-3xl font-bold">Decontare</h1>

      {transfersQuery.isPending && <p>Se încarcă…</p>}
      {transfersQuery.isError && <ErrorText message={errorMessage(transfersQuery.error)} />}

      {transfersQuery.data && transfers.length === 0 && (
        <Card>
          <p className="font-medium">Toată lumea e la zi. Nu e nimic de decontat.</p>
        </Card>
      )}

      {transfers.length > 0 && (
        <Card>
          <h2 className="mb-1 font-display text-xl font-bold">Transferuri sugerate</h2>
          <p className="mb-3 text-sm text-ink/70">
            {transfers.length === 1 ? 'Un singur transfer' : `${transfers.length} transferuri`} ajung ca toată lumea
            să fie la zi.
          </p>
          <ul className="flex flex-col divide-y divide-border">
            {transfers.map((transfer) => {
              const involvesMe = transfer.from.id === meId || transfer.to.id === meId
              return (
                <li
                  key={`${transfer.from.id}-${transfer.to.id}`}
                  className={`flex min-h-14 items-center justify-between gap-4 py-3 ${involvesMe ? 'font-medium' : ''}`}
                >
                  <span>
                    {name(transfer.from)} <span aria-hidden="true">→</span>
                    <span className="sr-only">îi dă lui</span> {name(transfer.to)}
                  </span>
                  <span className="shrink-0 font-display font-bold">{formatAmount(centsFromApi(transfer.amount))}</span>
                </li>
              )
            })}
          </ul>
        </Card>
      )}
    </main>
  )
}
