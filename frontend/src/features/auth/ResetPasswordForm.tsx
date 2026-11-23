import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation } from 'react-router'
import { TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button, ButtonLink } from '../../components/ui/Button'
import { errorMessage, isApiError } from '../../lib/errors'
import { authService } from '../../services/authService'
import { resetPasswordSchema, type ResetPasswordValues } from './schemas'
import { tokenFromHash } from './TokenFromHash'

export function ResetPasswordForm() {
  const location = useLocation()
  const [token] = useState(() => tokenFromHash(location.hash))
  const form = useForm<ResetPasswordValues>({ resolver: zodResolver(resetPasswordSchema), defaultValues: { password: '', confirmPassword: '' } })
  const reset = useMutation({
    mutationFn: (values: ResetPasswordValues) => authService.confirmPasswordReset(token ?? '', values.password),
    onError: (error) => {
      if (isApiError(error, 'WEAK_PASSWORD')) {
        form.setError('password', { message: errorMessage(error) })
      }
    },
  })

  useEffect(() => {
    if (token) {
      window.history.replaceState(null, '', window.location.pathname)
    }
  }, [token])

  if (!token) {
    return <Alert tone="error">Link jest niekompletny. Poproś o nowy link do zmiany hasła.</Alert>
  }
  if (reset.isSuccess) {
    return (
      <div className="space-y-4">
        <Alert tone="success" title="Hasło zmienione">Ze względów bezpieczeństwa wylogowaliśmy Cię ze wszystkich urządzeń.</Alert>
        <ButtonLink to="/login" fullWidth size="lg">Zaloguj się</ButtonLink>
      </div>
    )
  }
  return (
    <form noValidate onSubmit={form.handleSubmit((values) => reset.mutate(values))} className="space-y-4">
      {reset.isError && !isApiError(reset.error, 'WEAK_PASSWORD') && <Alert tone="error">{errorMessage(reset.error)}</Alert>}
      <TextField label="Nowe hasło" type="password" autoComplete="new-password" hint="Co najmniej 10 znaków." error={form.formState.errors.password?.message} {...form.register('password')} />
      <TextField label="Powtórz hasło" type="password" autoComplete="new-password" error={form.formState.errors.confirmPassword?.message} {...form.register('confirmPassword')} />
      <Button type="submit" fullWidth size="lg" loading={reset.isPending}>Ustaw nowe hasło</Button>
    </form>
  )
}
