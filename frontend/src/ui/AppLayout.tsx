import { useQuery } from '@tanstack/react-query'
import { Link, Outlet, useLocation } from 'react-router'
import { useMe } from '../auth/useMe'
import { settlementKeys, settlementsApi, waitingForAnswer } from '../settlements/api'

type Tab = { to: string; label: string; active: boolean; badge?: number }

/** Layout for the logged-in pages: page content plus a bottom navigation bar (thumb-friendly on phones). */
export function AppLayout() {
  const { pathname } = useLocation()
  const meId = useMe().data?.id

  // Payments and activity belong to a group, so their tabs only appear while you are inside one.
  const groupId = /^\/groups\/([^/]+)/.exec(pathname)?.[1]
  const onPayments = pathname.includes('/payments')
  const onActivity = pathname.endsWith('/activity')
  const onAccount = pathname.startsWith('/account')

  // Same query (same key) as the payments page, so this costs no extra request once that page loaded.
  const settlementsQuery = useQuery({
    queryKey: settlementKeys.list(groupId ?? ''),
    queryFn: () => settlementsApi.list(groupId!),
    enabled: groupId !== undefined,
  })
  const toAnswer = groupId ? waitingForAnswer(settlementsQuery.data ?? [], meId).length : 0

  const tabs: Tab[] = groupId
    ? [
        { to: `/groups/${groupId}`, label: 'Grup', active: !onPayments && !onActivity },
        { to: `/groups/${groupId}/payments`, label: 'Plăți', active: onPayments, badge: toAnswer },
        { to: `/groups/${groupId}/activity`, label: 'Activitate', active: onActivity },
      ]
    : [
        { to: '/', label: 'Grupuri', active: !onAccount },
        { to: '/account', label: 'Cont', active: onAccount },
      ]

  return (
    <>
      {/* Bottom padding so the fixed bar never covers the end of the page. */}
      <div className="pb-24">
        <Outlet />
      </div>
      <nav aria-label="Navigare principală" className="fixed inset-x-0 bottom-0 border-t border-border bg-surface">
        <ul className="mx-auto flex max-w-md">
          {tabs.map((tab) => (
            <li key={tab.label} className="flex-1">
              <Link
                to={tab.to}
                aria-current={tab.active ? 'page' : undefined}
                className={`flex min-h-16 items-center justify-center gap-1.5 font-medium focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-accent ${
                  tab.active ? 'text-accent' : 'text-ink/60'
                }`}
              >
                {tab.label}
                {tab.badge ? (
                  <span className="grid min-w-5 place-items-center rounded-full bg-negative px-1.5 text-xs font-semibold text-white">
                    {tab.badge}
                    <span className="sr-only"> de confirmat</span>
                  </span>
                ) : null}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </>
  )
}
