import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { balanceKeys, balancesApi } from '../balances/api'
import { BalanceSummaryCard } from '../balances/BalanceSummaryCard'
import { MemberBalances } from '../balances/MemberBalances'
import { ExpenseList } from '../expenses/ExpenseList'
import { ErrorText } from '../ui/ErrorText'
import { groupKeys, groupsApi } from './api'
import { InviteCard } from './InviteCard'

export function GroupPage() {
  // The route is /groups/:groupId, so the param is always present here.
  const groupId = useParams().groupId!
  const meQuery = useMe()

  const groupQuery = useQuery({
    queryKey: groupKeys.detail(groupId),
    queryFn: () => groupsApi.get(groupId),
  })
  const balancesQuery = useQuery({
    queryKey: balanceKeys.balances(groupId),
    queryFn: () => balancesApi.get(groupId),
  })

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-8">
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

          <BalanceSummaryCard groupId={groupId} myBalance={balancesQuery.data?.myBalance} />
          {balancesQuery.isError && <ErrorText message={errorMessage(balancesQuery.error)} />}
          {balancesQuery.data && <MemberBalances members={balancesQuery.data.members} meId={meQuery.data?.id} />}

          <ExpenseList groupId={groupId} />

          <InviteCard group={groupQuery.data} />
        </>
      )}
    </main>
  )
}
