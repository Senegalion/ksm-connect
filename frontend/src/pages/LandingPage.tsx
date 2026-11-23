import { Link } from 'react-router'
import { Brand, KsmStripe } from '../components/layout/Brand'
import { PosterArt } from '../components/layout/PosterArt'
import { ButtonLink } from '../components/ui/Button'
import { useSession } from '../hooks/useSession'

const STEPS = [
  { title: 'Dołącz', text: 'Zeskanuj kod QR od organizatora albo wpisz kod zaproszenia.', tone: 'bg-ksm-yellow-400 text-ksm-blue-900' },
  { title: 'Wpisz marzenia', text: 'Do trzech propozycji prezentów: link do produktu albo kilka słów opisu.', tone: 'bg-ksm-red-600 text-white' },
  { title: 'Zdrap swój los', text: 'Po losowaniu dostaniesz e-mail i SMS, a w aplikacji zdrapiesz los z imieniem osoby, którą obdarujesz.', tone: 'bg-ksm-blue-700 text-white' },
] as const

export function LandingPage() {
  const { user } = useSession()
  return (
    <div className="min-h-dvh bg-paper">
      <section className="relative overflow-hidden bg-ksm-blue-800 text-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <Brand inverted />
          {!user && <Link to="/login" className="text-sm font-semibold text-white/85 hover:text-white">Zaloguj się</Link>}
        </div>
        <div className="mx-auto grid max-w-6xl items-center gap-6 px-4 pt-6 pb-16 md:grid-cols-[1.15fr_1fr] md:pt-10 md:pb-24">
          <div className="relative z-10 space-y-7">
            <p className="inline-flex animate-slide-up items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-[13px] font-semibold text-ksm-yellow-300 ring-1 ring-white/15">
              Katolickie Stowarzyszenie Młodzieży
            </p>
            <h1 className="display-wide animate-slide-up text-[42px] leading-[0.95] text-white sm:text-6xl lg:text-7xl" style={{ animationDelay: '80ms' }}>
              Wigilia<br />
              <span className="text-ksm-yellow-400">bez kartek</span><br />
              i kapelusza.
            </h1>
            <p className="max-w-lg animate-slide-up text-lg leading-relaxed text-white/85" style={{ animationDelay: '160ms' }}>
              Zapisy, listy prezentów i losowanie w jednym miejscu. Każdy obdarowuje jedną osobę, nikt nie losuje siebie, a wynik zna tylko Ty.
            </p>
            <div className="flex animate-slide-up flex-col gap-3 sm:flex-row" style={{ animationDelay: '240ms' }}>
              {user ? (
                <ButtonLink to="/dashboard" variant="yellow" size="lg">Przejdź do wydarzeń</ButtonLink>
              ) : (
                <>
                  <ButtonLink to="/register" variant="yellow" size="lg">Załóż konto</ButtonLink>
                  <ButtonLink to="/join" variant="onDark" size="lg">Mam kod zaproszenia</ButtonLink>
                </>
              )}
            </div>
          </div>
          <PosterArt className="mx-auto -mb-24 w-full max-w-[420px] md:-mr-10 md:mb-0 md:max-w-none" />
        </div>
        <KsmStripe className="relative z-10 h-2" />
      </section>

      <section aria-labelledby="how-heading" className="mx-auto max-w-6xl px-4 py-16 md:py-24">
        <h2 id="how-heading" className="display-wide max-w-xl text-3xl leading-tight sm:text-4xl">Trzy kroki do prezentu.</h2>
        <ol className="mt-10 grid gap-4 md:grid-cols-3">
          {STEPS.map(({ title, text, tone }, index) => (
            <li key={title} className={`relative overflow-hidden rounded-2xl p-6 ${tone}`}>
              <span className="display-wide absolute -top-4 -right-1 text-[120px] leading-none opacity-20" aria-hidden>{index + 1}</span>
              <h3 className="relative text-2xl text-current">{title}</h3>
              <p className="relative mt-3 max-w-[30ch] leading-relaxed opacity-90">{text}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-16">
        <div className="flex flex-col gap-6 rounded-2xl bg-muted p-6 md:flex-row md:items-center md:justify-between md:p-8">
          <div className="max-w-2xl space-y-2">
            <h2 className="text-2xl">Twój los jest tylko Twój.</h2>
            <p className="leading-relaxed text-ink-700">
              Organizator zarządza zapisami, ale aplikacja nie pokazuje mu, kto kogo wylosował. Dane wydarzenia usuwamy automatycznie 90 dni po jego zakończeniu.
            </p>
          </div>
          {!user && <ButtonLink to="/register" size="lg">Dołącz do zabawy</ButtonLink>}
        </div>
      </section>

      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-wrap gap-x-6 gap-y-1 px-4 py-6 text-[13px] text-ink-500">
          <span>KSM Connect</span>
          <Link to="/privacy" className="hover:text-ink-900 hover:underline">Informacja o przetwarzaniu danych</Link>
        </div>
      </footer>
    </div>
  )
}
