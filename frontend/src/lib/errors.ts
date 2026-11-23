import { ApiError } from './api'

const MESSAGES: Record<string, string> = {
  NETWORK_ERROR: 'Brak połączenia z serwerem. Sprawdź internet i spróbuj ponownie.',
  INTERNAL_ERROR: 'Coś poszło nie tak po naszej stronie. Spróbuj ponownie za chwilę.',
  UNKNOWN_ERROR: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
  RATE_LIMITED: 'Zbyt wiele prób. Odczekaj chwilę i spróbuj ponownie.',
  VALIDATION_FAILED: 'Sprawdź poprawność wypełnionych pól.',
  MALFORMED_REQUEST: 'Nieprawidłowe dane formularza.',
  CONFLICT: 'Dane zmieniły się w międzyczasie. Odśwież stronę i spróbuj ponownie.',
  CSRF_INVALID: 'Sesja wygasła. Odśwież stronę i spróbuj ponownie.',
  UNAUTHENTICATED: 'Zaloguj się, aby kontynuować.',
  FORBIDDEN: 'Nie masz uprawnień do tej operacji.',
  NOT_FOUND: 'Nie znaleziono strony lub zasobu.',
  INVALID_CREDENTIALS: 'Nieprawidłowy e-mail lub hasło. Po kilku nieudanych próbach logowanie jest czasowo blokowane.',
  EMAIL_NOT_VERIFIED: 'Najpierw potwierdź adres e-mail – link wysłaliśmy w wiadomości powitalnej.',
  INVALID_OR_EXPIRED_TOKEN: 'Link jest nieprawidłowy lub wygasł. Poproś o nowy.',
  WEAK_PASSWORD: 'Hasło jest zbyt łatwe do odgadnięcia. Użyj co najmniej 10 znaków, najlepiej kilku słów.',
  INVALID_PHONE_NUMBER: 'Podaj prawidłowy numer telefonu, np. 600 100 200.',
  ACCOUNT_IN_COMPLETED_DRAW: 'Konto bierze udział w zakończonym losowaniu. Dane zostaną usunięte automatycznie po zakończeniu okresu przechowywania.',
  INVALID_INVITATION: 'Ten kod zaproszenia jest nieprawidłowy albo wygasł. Poproś organizatora o aktualny kod.',
  REGISTRATION_NOT_OPEN: 'Zapisy na to wydarzenie są zamknięte.',
  PARTICIPANT_LIMIT_REACHED: 'Osiągnięto limit uczestników tego wydarzenia.',
  PARTICIPATION_NOT_FOUND: 'Nie jesteś zapisany(-a) na to wydarzenie.',
  WITHDRAWAL_NOT_ALLOWED: 'Rezygnacja jest możliwa tylko w trakcie zapisów.',
  EVENT_NOT_FOUND: 'Nie znaleziono wydarzenia.',
  WISHLIST_LOCKED: 'Lista prezentów jest już zamknięta i nie można jej edytować.',
  INVALID_URL: 'Podaj pełny adres strony zaczynający się od https:// lub http://.',
  URL_REQUIRED: 'Wklej link do produktu.',
  DESCRIPTION_REQUIRED: 'Opisz, jaki prezent sprawiłby Ci radość.',
  INVALID_DESCRIPTION: 'Opis jest za długi lub zawiera niedozwolone znaki.',
  SUGGESTION_HAS_BOTH_URL_AND_DESCRIPTION: 'Wybierz link albo opis – nie oba naraz.',
  INVALID_SUGGESTION_POSITION: 'Nieprawidłowy numer propozycji.',
  EVENT_NAME_REQUIRED: 'Podaj nazwę wydarzenia.',
  INVALID_TIMEZONE: 'Nieprawidłowa strefa czasowa.',
  INVALID_REGISTRATION_WINDOW: 'Zamknięcie zapisów musi nastąpić po ich otwarciu.',
  DRAW_TIME_REQUIRED: 'Dla losowania automatycznego podaj datę i godzinę.',
  DRAW_BEFORE_REGISTRATION_CLOSE: 'Losowanie nie może odbyć się przed zamknięciem zapisów.',
  INVALID_PARTICIPANT_LIMIT: 'Limit uczestników musi wynosić co najmniej 2.',
  EVENT_NOT_CONFIGURABLE: 'Tego wydarzenia nie można już edytować.',
  INVALID_EVENT_TRANSITION: 'Ta zmiana statusu nie jest możliwa na tym etapie.',
  INVITATION_NOT_ALLOWED: 'Dla tego wydarzenia nie można już wygenerować zaproszenia.',
  REMOVAL_NOT_ALLOWED: 'Uczestników można usuwać tylko przed losowaniem.',
  DRAW_ALREADY_COMPLETED: 'Losowanie zostało już przeprowadzone. Wyników nie można zmienić.',
  REGISTRATION_NOT_CLOSED: 'Przed losowaniem zamknij zapisy.',
  NOT_ENOUGH_PARTICIPANTS: 'Do losowania potrzeba co najmniej dwóch uczestników.',
  DRAW_CONFIRMATION_REQUIRED: 'Potwierdź przeprowadzenie losowania.',
  NOTIFICATION_NOT_RETRYABLE: 'Tego powiadomienia nie można ponowić.',
  NOTIFICATION_NOT_FOUND: 'Nie znaleziono powiadomienia.',
}

const FALLBACK_MESSAGE = 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.'

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return MESSAGES[error.code] ?? FALLBACK_MESSAGE
  }
  return FALLBACK_MESSAGE
}

export function isApiError(error: unknown, code: string): boolean {
  return error instanceof ApiError && error.code === code
}
