import { Link, Outlet, useLocation } from 'react-router'

const tabs = [
  { to: '/', label: 'Grupuri', isActive: (path: string) => !path.startsWith('/account') },
  { to: '/account', label: 'Cont', isActive: (path: string) => path.startsWith('/account') },
]

/** Layout for the logged-in pages: page content plus a bottom navigation bar (thumb-friendly on phones). */
export function AppLayout() {
  const { pathname } = useLocation()

  return (
    <>
      {/* Bottom padding so the fixed bar never covers the end of the page. */}
      <div className="pb-24">
        <Outlet />
      </div>
      <nav aria-label="Navigare principală" className="fixed inset-x-0 bottom-0 border-t border-border bg-surface">
        <ul className="mx-auto flex max-w-md">
          {tabs.map((tab) => {
            const active = tab.isActive(pathname)
            return (
              <li key={tab.to} className="flex-1">
                <Link
                  to={tab.to}
                  aria-current={active ? 'page' : undefined}
                  className={`flex min-h-16 items-center justify-center font-medium focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-accent ${
                    active ? 'text-accent' : 'text-ink/60'
                  }`}
                >
                  {tab.label}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>
    </>
  )
}
