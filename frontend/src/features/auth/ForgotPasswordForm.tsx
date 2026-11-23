import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { errorMessage } from '../../lib/errors'
import { authService } from '../../services/authService'
import { emailOnlySchema, type EmailOnlyValues } from './schemas'

export function ForgotPasswordForm() {
  const form = useForm<EmailOnlyValues>({ resolver: zodResolver(emailOnlySchema), defaultValues: { email: '' } })
  const request = useMutation({ mutationFn: (values: EmailOnlyValues) => authService.requestPasswordReset(values.email) })

  if (request.isSuccess) {
    return (
      <div className="space-y-4">
        <Alert tone="success" title="Sprawdź skrzynkę">
          Jeśli konto o tym adresie istnieje, wysłaliśmy link do ustawienia nowego hasła. Link jest ważny przez godzinę.
        </Alert>
        <Link to="/login" className="block text-center text-sm font-medium text-accent-700 hover:underline">Wróć do logowania</Link>
      </div>
    )
  }

  return (
    <form noValidate onSubmit={form.handleSubmit((values) => request.mutate(values))} className="space-y-4">
      {request.isError && <Alert tone="error">{errorMessage(request.error)}</Alert>}
      <TextField label="E-mail" type="email" inputMode="email" autoComplete="email" autoCapitalize="none" error={form.formState.errors.email?.message} {...form.register('email')} />
      <Button type="submit" fullWidth size="lg" loading={request.isPending}>Wyślij link</Button>
      <Link to="/login" className="block text-center text-sm font-medium text-accent-700 hover:underline">Wróć do logowania</Link>
    </form>
  )
}
