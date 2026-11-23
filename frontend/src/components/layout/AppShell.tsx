import { useMutation, useQueryClient } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useSession } from '../../hooks/useSession'
import { authService } from '../../services/authService'
import { cn } from '../ui/cn'
import { Brand } from './Brand'

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
      <header className="border-b border-line bg-paper">
        <div className="mx-auto flex h-14 max-w-5xl items-center justify-between gap-6 px-4">
          <div className="flex items-center gap-8">
            <Brand to="/dashboard" />
            <nav aria-label="Główna nawigacja" className="hidden h-14 items-stretch gap-6 md:flex">
              {items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn('flex items-center border-b-2 text-[15px] transition-colors',
                      isActive ? 'border-ink-900 font-medium text-ink-900' : 'border-transparent text-ink-500 hover:text-ink-900')
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-4 text-sm">
            <span className="hidden text-ink-500 sm:inline">{user?.firstName} {user?.lastName}</span>
            <button type="button" onClick={() => logout.mutate()} className="min-h-10 text-ink-700 underline-offset-4 hover:underline" aria-label="Wyloguj">
              Wyloguj
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 pt-8 pb-28 md:pb-16">
        <Outlet />
      </main>

      <nav aria-label="Nawigacja dolna" className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-paper pb-[env(safe-area-inset-bottom)] md:hidden">
        <ul className="flex">
          {items.map((item) => (
            <li key={item.to} className="flex-1">
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn('flex min-h-14 items-center justify-center border-t-2 text-[13px]',
                    isActive ? 'border-ink-900 font-semibold text-ink-900' : 'border-transparent text-ink-500')
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
