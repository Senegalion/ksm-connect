import { useMutation, useQueryClient } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useSession } from '../../hooks/useSession'
import { authService } from '../../services/authService'
import { cn } from '../ui/cn'
import { Brand, KsmStripe } from './Brand'

interface NavItem {
  to: string
  label: string
  end?: boolean
}

export function AppShell() {
  const { user, isAdmin } = useSession()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const logout = useMutation({
    mutationFn: authService.logout,
    onSettled: () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    },
  })

  const items: NavItem[] = [
    { to: '/dashboard', label: 'Wydarzenia', end: true },
    { to: '/join', label: 'Dołącz' },
    ...(isAdmin ? [{ to: '/admin', label: 'Organizator' }] : []),
    { to: '/account', label: 'Konto' },
  ]

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-20 bg-paper/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between gap-6 px-4">
          <div className="flex items-center gap-8">
            <Brand to="/dashboard" />
            <nav aria-label="Główna nawigacja" className="hidden h-16 items-stretch gap-6 md:flex">
              {items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn('flex items-center border-b-[3px] pt-[3px] text-[15px] font-semibold transition-colors',
                      isActive ? 'border-ksm-yellow-400 text-ksm-blue-800' : 'border-transparent text-ink-500 hover:text-ksm-blue-800')
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-4 text-sm">
            <span className="hidden items-center gap-2 text-ink-700 sm:inline-flex">
              <span className="flex size-8 items-center justify-center rounded-full bg-ksm-yellow-400 text-[13px] font-bold text-ksm-blue-900" aria-hidden>
                {user?.firstName.charAt(0)}{user?.lastName.charAt(0)}
              </span>
              {user?.firstName}
            </span>
            <button type="button" onClick={() => logout.mutate()} className="min-h-10 font-semibold text-ink-500 hover:text-ksm-red-600" aria-label="Wyloguj">
              Wyloguj
            </button>
          </div>
        </div>
        <KsmStripe className="h-1" />
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 pt-8 pb-28 md:pb-16">
        <Outlet />
      </main>

      <nav aria-label="Nawigacja dolna" className="fixed inset-x-0 bottom-0 z-20 bg-ksm-blue-900 pb-[env(safe-area-inset-bottom)] md:hidden">
        <ul className="flex">
          {items.map((item) => (
            <li key={item.to} className="flex-1">
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn('flex min-h-15 items-center justify-center border-t-[3px] text-[13px] font-semibold transition-colors',
                    isActive ? 'border-ksm-yellow-400 text-white' : 'border-transparent text-white/60')
                }
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
