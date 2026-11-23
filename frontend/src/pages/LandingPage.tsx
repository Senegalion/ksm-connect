import { Gift, ListChecks, Lock, QrCode } from 'lucide-react'
import { PublicLayout } from '../components/layout/PublicLayout'
import { ButtonLink } from '../components/ui/Button'
import { useSession } from '../hooks/useSession'

const STEPS = [
  { icon: QrCode, title: 'Dołącz kodem', text: 'Zeskanuj QR albo wpisz kod od organizatora.' },
  { icon: ListChecks, title: 'Dodaj listę prezentów', text: 'Do trzech propozycji – link do produktu lub krótki opis.' },
  { icon: Gift, title: 'Poznaj swoją osobę', text: 'Po losowaniu dostaniesz e-mail i SMS z imieniem osoby, którą obdarujesz.' },
]

export function LandingPage() {
  const { user } = useSession()
  return (
    <PublicLayout wide>
      <section className="grid items-center gap-10 py-6 md:grid-cols-[1.1fr_1fr] md:py-14">
        <div className="animate-rise space-y-6">
          <p className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1 text-xs font-semibold text-wine-700 shadow-sm">
            Katolickie Stowarzyszenie Młodzieży
          </p>
          <h1 className="text-4xl leading-[1.05] font-semibold sm:text-5xl md:text-6xl">
            Wigilijne losowanie prezentów <span className="text-wine-700 italic">bez kartek i kapelusza</span>
          </h1>
          <p className="max-w-xl text-lg leading-relaxed text-ink-700">
            KSM Connect zbiera zapisy, listy prezentów i losuje pary tak, żeby każdy obdarował jedną osobę – i nikt nie wylosował siebie.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            {user ? (
              <ButtonLink to="/dashboard" size="lg">Przejdź do wydarzeń</ButtonLink>
            ) : (
              <>
                <ButtonLink to="/register" size="lg">Załóż konto</ButtonLink>
                <ButtonLink to="/login" size="lg" variant="secondary">Mam już konto</ButtonLink>
              </>
            )}
          </div>
        </div>
        <div className="relative mx-auto w-full max-w-sm animate-reveal px-2 sm:px-0">
          <div className="absolute -inset-2 -rotate-2 rounded-[2rem] bg-gold-100 sm:-inset-4 sm:-rotate-3" aria-hidden />
          <div className="relative space-y-4 rounded-[2rem] border border-cream-200 bg-white p-6 shadow-xl">
            <p className="text-sm text-ink-500">Przygotowujesz prezent dla</p>
            <p className="font-display text-3xl font-semibold text-wine-700">Kasia N.</p>
            <ul className="space-y-2 text-sm">
              <li className="rounded-xl bg-cream-50 px-3 py-2.5">📚 Książka o astronomii</li>
              <li className="rounded-xl bg-cream-50 px-3 py-2.5">🎲 Gra planszowa dla 2–4 osób</li>
              <li className="rounded-xl bg-cream-50 px-3 py-2.5">🧣 Ciepły szalik w kolorze butelkowej zieleni</li>
            </ul>
          </div>
        </div>
      </section>

      <section aria-label="Jak to działa" className="grid gap-4 py-8 sm:grid-cols-3">
        {STEPS.map(({ icon: Icon, title, text }, index) => (
          <div key={title} className="space-y-2 rounded-[var(--radius-card)] border border-cream-200 bg-white p-5">
            <div className="flex items-center gap-3">
              <span className="flex size-10 items-center justify-center rounded-xl bg-wine-50 text-wine-700"><Icon className="size-5" aria-hidden /></span>
              <span className="text-xs font-semibold text-ink-300">Krok {index + 1}</span>
            </div>
            <h2 className="text-lg font-semibold">{title}</h2>
            <p className="text-sm leading-relaxed text-ink-500">{text}</p>
          </div>
        ))}
      </section>

      <section className="flex items-start gap-3 rounded-[var(--radius-card)] bg-pine-700 p-5 text-white sm:p-6">
        <Lock className="mt-0.5 size-5 shrink-0" aria-hidden />
        <p className="text-sm leading-relaxed text-white/90">
          Wynik losowania widzisz tylko Ty. Organizator zarządza wydarzeniem, ale aplikacja nie pokazuje mu, kto kogo wylosował.
        </p>
      </section>
    </PublicLayout>
  )
}
