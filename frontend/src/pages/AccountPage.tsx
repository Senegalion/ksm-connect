import { AccountSettings } from '../features/account/AccountSettings'
import { useSession } from '../hooks/useSession'

export function AccountPage() {
  const { user } = useSession()
  if (!user) return null
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-semibold">Konto</h1>
      <AccountSettings user={user} />
    </div>
  )
}
