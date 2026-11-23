import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { MailCheck } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { CheckboxField } from '../../components/forms/CheckboxField'
import { TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { errorMessage } from '../../lib/errors'
import { authService } from '../../services/authService'
import { applyServerErrors } from './applyServerErrors'
import { registerSchema, type RegisterValues } from './schemas'

const SERVER_FIELD_MESSAGES = {
  INVALID_PHONE_NUMBER: 'Podaj prawidłowy numer telefonu, np. 600 100 200.',
  WEAK_PASSWORD: 'To hasło jest zbyt łatwe. Spróbuj kilku słów, np. „zimowa wigilia przy choince”.',
}

export function RegisterForm() {
  const form = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { firstName: '', lastName: '', email: '', phoneNumber: '', password: '', privacyNoticeAccepted: false },
  })
  const register = useMutation({
    mutationFn: authService.register,
    onError: (error) => applyServerErrors(error, form.setError, SERVER_FIELD_MESSAGES),
  })
  const resend = useMutation({ mutationFn: () => authService.resendVerification(form.getValues('email')) })

  if (register.isSuccess) {
    return (
      <div className="space-y-4 text-center">
        <div className="mx-auto flex size-14 items-center justify-center rounded-2xl bg-pine-50 text-pine-700">
          <MailCheck className="size-7" aria-hidden />
        </div>
        <h2 className="text-xl font-semibold">Sprawdź swoją skrzynkę</h2>
        <p className="text-sm leading-relaxed text-ink-500">
          Wysłaliśmy wiadomość na adres <strong className="text-ink-900">{form.getValues('email')}</strong>. Kliknij link w wiadomości, aby potwierdzić konto. Link jest ważny przez 24 godziny.
        </p>
        <Button variant="secondary" fullWidth onClick={() => resend.mutate()} loading={resend.isPending} disabled={resend.isSuccess}>
          {resend.isSuccess ? 'Wysłano ponownie' : 'Nie dotarło? Wyślij ponownie'}
        </Button>
        <Link to="/login" className="block text-sm font-semibold text-wine-700">Przejdź do logowania</Link>
      </div>
    )
  }

  const { errors } = form.formState
  const serverErrorHandledInline = register.error && Object.keys(errors).length > 0

  return (
    <form noValidate onSubmit={form.handleSubmit((values) => register.mutate(values))} className="space-y-4">
      {register.isError && !serverErrorHandledInline && <Alert tone="error">{errorMessage(register.error)}</Alert>}
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField label="Imię" autoComplete="given-name" error={errors.firstName?.message} {...form.register('firstName')} />
        <TextField label="Nazwisko" autoComplete="family-name" error={errors.lastName?.message} {...form.register('lastName')} />
      </div>
      <TextField label="E-mail" type="email" inputMode="email" autoComplete="email" autoCapitalize="none" error={errors.email?.message} {...form.register('email')} />
      <TextField
        label="Numer telefonu"
        type="tel"
        inputMode="tel"
        autoComplete="tel"
        placeholder="600 100 200"
        hint="Wyślemy na niego SMS z wynikiem losowania."
        error={errors.phoneNumber?.message}
        {...form.register('phoneNumber')}
      />
      <TextField
        label="Hasło"
        type="password"
        autoComplete="new-password"
        hint="Co najmniej 10 znaków. Najłatwiej zapamiętać kilka słów."
        error={errors.password?.message}
        {...form.register('password')}
      />
      <CheckboxField
        label={<>Zapoznałem(-am) się z <Link to="/privacy" target="_blank" className="font-semibold text-wine-700 underline underline-offset-4">informacją o przetwarzaniu danych osobowych</Link>.</>}
        error={errors.privacyNoticeAccepted?.message}
        {...form.register('privacyNoticeAccepted')}
      />
      <Button type="submit" fullWidth size="lg" loading={register.isPending}>Załóż konto</Button>
      <p className="text-center text-sm text-ink-500">
        Masz już konto? <Link to="/login" className="font-semibold text-wine-700 underline-offset-4 hover:underline">Zaloguj się</Link>
      </p>
    </form>
  )
}
