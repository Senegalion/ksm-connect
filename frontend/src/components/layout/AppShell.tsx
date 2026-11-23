import { useMutation, useQueryClient } from '@tanstack/react-query'
import { CalendarHeart, LogOut, QrCode, Settings2, UserRound } from 'lucide-react'
import type { ReactNode } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useSession } from '../../hooks/useSession'
import { authService } from '../../services/authService'
import { cn } from '../ui/cn'
import { Brand } from './Brand'

interface NavItem {
  to: string
  label: string
  icon: ReactNode
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
    { to: '/dashboard', label: 'Wydarzenia', icon: <CalendarHeart className="size-5" aria-hidden />, end: true },
    { to: '/join', label: 'Dołącz', icon: <QrCode className="size-5" aria-hidden /> },
    ...(isAdmin ? [{ to: '/admin', label: 'Organizator', icon: <Settings2 className="size-5" aria-hidden /> }] : []),
    { to: '/account', label: 'Konto', icon: <UserRound className="size-5" aria-hidden /> },
  ]

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-20 border-b border-cream-200/80 bg-cream-100/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-5xl items-center justify-between gap-4 px-4">
          <Brand to="/dashboard" />
          <nav aria-label="Główna nawigacja" className="hidden items-center gap-1 md:flex">
            {items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn('flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-semibold transition-colors',
                    isActive ? 'bg-white text-wine-700 shadow-sm' : 'text-ink-700 hover:bg-cream-200/70')
                }
              >
                {item.icon}
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <span className="hidden text-sm text-ink-500 lg:inline">{user?.firstName}</span>
            <button
              type="button"
              onClick={() => logout.mutate()}
              className="flex size-10 items-center justify-center rounded-xl text-ink-700 hover:bg-cream-200/70"
              aria-label="Wyloguj"
              title="Wyloguj"
            >
              <LogOut className="size-5" aria-hidden />
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 pt-6 pb-28 md:pb-12">
        <Outlet />
      </main>

      <nav
        aria-label="Nawigacja dolna"
        className="fixed inset-x-0 bottom-0 z-20 border-t border-cream-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
      >
        <ul className="mx-auto flex max-w-md justify-around">
          {items.map((item) => (
            <li key={item.to} className="flex-1">
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn('flex min-h-16 flex-col items-center justify-center gap-1 text-[11px] font-semibold',
                    isActive ? 'text-wine-700' : 'text-ink-500')
                }
              >
                {item.icon}
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
