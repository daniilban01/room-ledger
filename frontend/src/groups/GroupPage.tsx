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
      <PageHeader
        title={groupQuery.data?.name ?? ''}
        backTo="/"
        action={
          groupQuery.data?.myRole === 'ADMIN' && (
            <Link
              to={`/groups/${groupId}/settings`}
              aria-label="Setări grup"
              className="-mr-2 grid size-11 shrink-0 place-items-center rounded-full text-ink hover:bg-surface focus-visible:outline-2 focus-visible:outline-accent"
            >
              <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth={2} aria-hidden="true">
                <circle cx="12" cy="12" r="3" />
                <path
                  d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"
                  strokeLinejoin="round"
                />
              </svg>
            </Link>
          )
        }
      />

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
