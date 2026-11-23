import { PublicLayout } from '../components/layout/PublicLayout'
import { ButtonLink } from '../components/ui/Button'

export function NotFoundPage() {
  return (
    <PublicLayout>
      <div className="space-y-4 py-16 text-center">
        <p className="font-display text-6xl font-semibold text-wine-700">404</p>
        <h1 className="text-2xl font-semibold">Nie znaleziono strony</h1>
        <ButtonLink to="/">Strona główna</ButtonLink>
      </div>
    </PublicLayout>
  )
}
