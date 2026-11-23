import { PublicLayout } from '../components/layout/PublicLayout'
import { PageLoader } from '../components/ui/Spinner'
import { useSession } from '../hooks/useSession'
import { JoinPage } from './JoinPage'
import { AppShell } from '../components/layout/AppShell'

export function JoinRoute() {
  const { user, isLoading } = useSession()
  if (isLoading) {
    return <PageLoader />
  }
  return user ? <AppShell /> : (
    <PublicLayout>
      <JoinPage />
    </PublicLayout>
  )
}
