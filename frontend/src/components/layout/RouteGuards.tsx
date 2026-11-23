import { Navigate, Outlet, useLocation, useSearchParams } from 'react-router'
import { peekPendingInvitation } from '../../lib/invitation'
import { safeNextPath } from '../../lib/navigation'
import { useSession } from '../../hooks/useSession'
import { PageLoader } from '../ui/Spinner'

export function RequireAuth() {
  const { isLoading, user } = useSession()
  const location = useLocation()
  if (isLoading) {
    return <PageLoader />
  }
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  return <Outlet />
}

export function RequireAdmin() {
  const { isLoading, isAdmin } = useSession()
  if (isLoading) {
    return <PageLoader />
  }
  return isAdmin ? <Outlet /> : <Navigate to="/dashboard" replace />
}

export function RedirectIfAuthenticated() {
  const { isLoading, user } = useSession()
  const [params] = useSearchParams()
  if (isLoading) {
    return <PageLoader />
  }
  if (!user) {
    return <Outlet />
  }
  const pendingCode = peekPendingInvitation()
  const target = pendingCode ? `/join#${encodeURIComponent(pendingCode)}` : safeNextPath(params.get('next'))
  return <Navigate to={target} replace />
}
