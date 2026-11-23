import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router'
import { TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { errorMessage, isApiError } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { authService } from '../../services/authService'
import { loginSchema, type LoginValues } from './schemas'

export function LoginForm() {
  const [params] = useSearchParams()
  const queryClient = useQueryClient()
  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } })

  const login = useMutation({
    mutationFn: (values: LoginValues) => authService.login(values.email, values.password),
    onSuccess: (session) => queryClient.setQueryData(queryKeys.session, session),
  })

  const next = params.get('next')
  const registerLink = next ? `/register?next=${encodeURIComponent(next)}` : '/register'
  const resend = useMutation({ mutationFn: () => authService.resendVerification(form.getValues('email')) })

  return (
    <form noValidate onSubmit={form.handleSubmit((values) => login.mutate(values))} className="space-y-4">
      {login.isError && (
        <Alert tone="error">
          {errorMessage(login.error)}
          {isApiError(login.error, 'EMAIL_NOT_VERIFIED') && (
            <button type="button" className="mt-1 block font-semibold underline underline-offset-4" onClick={() => resend.mutate()} disabled={resend.isPending}>
              {resend.isSuccess ? 'Wysłaliśmy nowy link.' : 'Wyślij link ponownie'}
            </button>
          )}
        </Alert>
      )}
      <TextField
        label="E-mail"
        type="email"
        autoComplete="email"
        inputMode="email"
        autoCapitalize="none"
        error={form.formState.errors.email?.message}
        {...form.register('email')}
      />
      <TextField
        label="Hasło"
        type="password"
        autoComplete="current-password"
        error={form.formState.errors.password?.message}
        {...form.register('password')}
      />
      <div className="flex justify-end">
        <Link to="/forgot-password" className="text-sm font-semibold text-wine-700 underline-offset-4 hover:underline">
          Nie pamiętam hasła
        </Link>
      </div>
      <Button type="submit" fullWidth size="lg" loading={login.isPending}>Zaloguj się</Button>
      <p className="text-center text-sm text-ink-500">
        Nie masz konta?{' '}
        <Link to={registerLink} className="font-semibold text-wine-700 underline-offset-4 hover:underline">
          Załóż konto
        </Link>
      </p>
    </form>
  )
}
