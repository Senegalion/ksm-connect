import { PublicLayout } from '../components/layout/PublicLayout'
import { ButtonLink } from '../components/ui/Button'
import { useSession } from '../hooks/useSession'

const STEPS = [
  ['Dołącz do wydarzenia', 'Zeskanuj kod QR od organizatora albo wpisz kod zaproszenia.'],
  ['Dodaj listę prezentów', 'Do trzech propozycji: link do produktu albo kilka słów opisu.'],
  ['Poznaj swoją osobę', 'Po losowaniu dostaniesz e-mail i SMS z imieniem osoby, którą obdarujesz.'],
] as const

export function LandingPage() {
  const { user } = useSession()
  return (
    <PublicLayout wide>
      <section className="max-w-2xl space-y-6 pb-12">
        <p className="text-sm font-medium text-accent-700">Katolickie Stowarzyszenie Młodzieży</p>
        <h1 className="text-[34px] leading-[1.1] sm:text-5xl">Zapisy i losowanie prezentów na Wigilię KSM</h1>
        <p className="text-lg leading-relaxed text-ink-700">
          Każdy kupuje prezent dla jednej osoby i każdy jeden dostaje. Nikt nie losuje siebie, a wynik zna tylko osoba, której dotyczy.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row">
          {user ? (
            <ButtonLink to="/dashboard" size="lg">Przejdź do wydarzeń</ButtonLink>
          ) : (
            <>
              <ButtonLink to="/register" size="lg">Załóż konto</ButtonLink>
              <ButtonLink to="/login" size="lg" variant="secondary">Zaloguj się</ButtonLink>
            </>
          )}
        </div>
      </section>

      <section aria-labelledby="how-heading" className="border-t border-line pt-8">
        <h2 id="how-heading" className="mb-6 text-sm font-medium tracking-wide text-ink-500 uppercase">Jak to działa</h2>
        <ol className="grid gap-8 sm:grid-cols-3">
          {STEPS.map(([title, text], index) => (
            <li key={title} className="space-y-2">
              <span className="font-mono text-sm text-accent-700">{String(index + 1).padStart(2, '0')}</span>
              <h3 className="text-lg">{title}</h3>
              <p className="text-[15px] leading-relaxed text-ink-500">{text}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="mt-12 border-t border-line pt-8">
        <p className="max-w-2xl text-[15px] leading-relaxed text-ink-700">
          <strong className="font-semibold">Prywatność.</strong> Organizator zarządza zapisami, ale aplikacja nie pokazuje mu, kto kogo wylosował.
          Dane wydarzenia usuwamy automatycznie 90 dni po jego zakończeniu.
        </p>
      </section>
    </PublicLayout>
  )
}
