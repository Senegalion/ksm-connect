import { PublicLayout } from '../components/layout/PublicLayout'

export function PrivacyPage() {
  return (
    <PublicLayout wide>
      <article className="mx-auto max-w-2xl space-y-5 leading-relaxed text-ink-700">
        <h1 className="text-3xl leading-tight">Informacja o przetwarzaniu danych osobowych</h1>
        <p className="rounded-md bg-warning-50 px-4 py-3 text-sm text-warning-700">
          Wersja robocza – treść wymaga weryfikacji przez administratora danych (zarząd KSM) przed uruchomieniem produkcyjnym.
        </p>
        <section className="space-y-2">
          <h2 className="text-xl font-semibold">Jakie dane zbieramy</h2>
          <p>Imię, nazwisko, adres e-mail, numer telefonu, informacje o zapisach na wydarzenia oraz propozycje prezentów, które sam(-a) dodajesz.</p>
        </section>
        <section className="space-y-2">
          <h2 className="text-xl font-semibold">W jakim celu</h2>
          <p>Wyłącznie w celu organizacji wydarzeń KSM, w tym zapisów, losowania osób do obdarowania i wysłania Ci wyniku losowania e-mailem oraz SMS-em.</p>
        </section>
        <section className="space-y-2">
          <h2 className="text-xl font-semibold">Kto ma dostęp</h2>
          <p>Organizator widzi listę uczestników wydarzenia (imię, nazwisko, e-mail, telefon) i status wysyłki powiadomień. Wynik losowania widzi tylko osoba, której dotyczy. Wiadomości wysyłamy za pośrednictwem dostawców poczty e-mail i SMS.</p>
        </section>
        <section className="space-y-2">
          <h2 className="text-xl font-semibold">Jak długo</h2>
          <p>Dane związane z wydarzeniem usuwamy automatycznie 90 dni po wydarzeniu. Niepotwierdzone konta usuwamy po 30 dniach. Konto możesz usunąć w każdej chwili w ustawieniach.</p>
        </section>
        <section className="space-y-2">
          <h2 className="text-xl font-semibold">Twoje prawa</h2>
          <p>Masz prawo dostępu do danych, ich sprostowania, usunięcia, ograniczenia przetwarzania oraz wniesienia skargi do Prezesa UODO.</p>
        </section>
      </article>
    </PublicLayout>
  )
}
