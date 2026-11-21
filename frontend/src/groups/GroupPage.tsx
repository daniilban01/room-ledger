import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { balanceKeys, balancesApi } from '../balances/api'
import { BalanceSummaryCard } from '../balances/BalanceSummaryCard'
import { MemberBalances } from '../balances/MemberBalances'
import { ExpenseList } from '../expenses/ExpenseList'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { groupKeys, groupsApi } from './api'
import { InviteCard } from './InviteCard'

export function GroupPage() {
  // The route is /groups/:groupId, so the param is always present here.
  const groupId = useParams().groupId!
  const meId = useMe().data?.id

  const groupQuery = useQuery({
    queryKey: groupKeys.detail(groupId),
    queryFn: () => groupsApi.get(groupId),
  })
  const balancesQuery = useQuery({
    queryKey: balanceKeys.balances(groupId),
    queryFn: () => balancesApi.get(groupId),
  })

  return (
    // Extra bottom padding so the floating button never covers the last card.
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 pt-6 pb-20">
      <PageHeader title={groupQuery.data?.name ?? ''} backTo="/" />

      {groupQuery.isPending && <p className="text-ink/60">Se încarcă…</p>}
      {groupQuery.isError && (
        <ErrorText message={errorMessage(groupQuery.error, { 404: 'Grupul nu există sau nu ai acces la el.' })} />
      )}

      {groupQuery.data && (
        <>
          <BalanceSummaryCard groupId={groupId} myBalance={balancesQuery.data?.myBalance} />
          {balancesQuery.isError && <ErrorText message={errorMessage(balancesQuery.error)} />}

          {balancesQuery.data && <MemberBalances members={balancesQuery.data.members} meId={meId} />}
          <ExpenseList groupId={groupId} />
          <InviteCard group={groupQuery.data} />

          {/* Floating action button, above the bottom navigation, where the thumb reaches on a phone. */}
          <Link
            to={`/groups/${groupId}/receipt`}
            className="fixed right-4 bottom-20 flex min-h-14 items-center gap-2 rounded-full bg-accent px-5 font-semibold text-white shadow-lg shadow-accent/30 hover:bg-accent/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent sm:right-[calc(50%-14rem+1rem)]"
          >
            <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth={2} aria-hidden="true">
              <path
                d="M4 8a2 2 0 0 1 2-2h1.5l1.5-2h6l1.5 2H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8z"
                strokeLinejoin="round"
              />
              <circle cx="12" cy="12.5" r="3.5" />
            </svg>
            Scanează bon
          </Link>
        </>
      )}
    </main>
  )
}
