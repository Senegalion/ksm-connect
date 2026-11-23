import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { z } from 'zod'
import { TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Card, CardBody } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { errorMessage, isApiError } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { accountService } from '../../services/accountService'
import { authService } from '../../services/authService'
import type { SessionUser } from '../../types/api'

const profileSchema = z.object({
  firstName: z.string().trim().min(1, 'Podaj imię.').max(100),
  lastName: z.string().trim().min(1, 'Podaj nazwisko.').max(100),
  phoneNumber: z.string().trim().min(7, 'Podaj numer telefonu.').max(30),
})

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Podaj obecne hasło.'),
  newPassword: z.string().min(10, 'Nowe hasło musi mieć co najmniej 10 znaków.').max(128),
})

export function AccountSettings({ user }: { user: SessionUser }) {
  return (
    <div className="space-y-4">
      <ProfileCard user={user} />
      <PasswordCard />
      <DeleteAccountCard />
    </div>
  )
}

function ProfileCard({ user }: { user: SessionUser }) {
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof profileSchema>>({
    resolver: zodResolver(profileSchema),
    defaultValues: { firstName: user.firstName, lastName: user.lastName, phoneNumber: user.phoneNumber ?? '' },
  })
  const save = useMutation({
    mutationFn: accountService.update,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.session }),
    onError: (error) => {
      if (isApiError(error, 'INVALID_PHONE_NUMBER')) form.setError('phoneNumber', { message: errorMessage(error) })
    },
  })
  const { errors } = form.formState
  return (
    <Card>
      <CardBody>
        <form noValidate onSubmit={form.handleSubmit((values) => save.mutate(values))} className="space-y-4">
          <h2 className="text-xl font-semibold">Twoje dane</h2>
          <p className="text-sm text-ink-500">E-mail: <strong className="text-ink-900">{user.email}</strong></p>
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Imię" autoComplete="given-name" error={errors.firstName?.message} {...form.register('firstName')} />
            <TextField label="Nazwisko" autoComplete="family-name" error={errors.lastName?.message} {...form.register('lastName')} />
          </div>
          <TextField label="Telefon" type="tel" inputMode="tel" autoComplete="tel" error={errors.phoneNumber?.message} {...form.register('phoneNumber')} />
          {save.isSuccess && <Alert tone="success">Zapisano zmiany.</Alert>}
          {save.isError && !isApiError(save.error, 'INVALID_PHONE_NUMBER') && <Alert tone="error">{errorMessage(save.error)}</Alert>}
          <Button type="submit" loading={save.isPending}>Zapisz</Button>
        </form>
      </CardBody>
    </Card>
  )
}

function PasswordCard() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const form = useForm<z.infer<typeof passwordSchema>>({ resolver: zodResolver(passwordSchema), defaultValues: { currentPassword: '', newPassword: '' } })
  const change = useMutation({
    mutationFn: (values: z.infer<typeof passwordSchema>) => authService.changePassword(values.currentPassword, values.newPassword),
    onSuccess: () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    },
  })
  const { errors } = form.formState
  return (
    <Card>
      <CardBody>
        <form noValidate onSubmit={form.handleSubmit((values) => change.mutate(values))} className="space-y-4">
          <h2 className="text-xl font-semibold">Zmiana hasła</h2>
          <TextField label="Obecne hasło" type="password" autoComplete="current-password" error={errors.currentPassword?.message} {...form.register('currentPassword')} />
          <TextField label="Nowe hasło" type="password" autoComplete="new-password" error={errors.newPassword?.message} {...form.register('newPassword')} />
          {change.isError && <Alert tone="error">{errorMessage(change.error)}</Alert>}
          <p className="text-xs text-ink-500">Po zmianie hasła zostaniesz wylogowany(-a) ze wszystkich urządzeń.</p>
          <Button type="submit" variant="secondary" loading={change.isPending}>Zmień hasło</Button>
        </form>
      </CardBody>
    </Card>
  )
}

function DeleteAccountCard() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const remove = useMutation({
    mutationFn: () => accountService.delete(password),
    onSuccess: () => {
      queryClient.clear()
      navigate('/', { replace: true })
    },
  })
  return (
    <Card>
      <CardBody className="space-y-3">
        <h2 className="text-xl font-semibold">Usunięcie konta</h2>
        <p className="text-sm leading-relaxed text-ink-500">Usuniemy Twoje konto, zapisy i listy prezentów. Jeśli bierzesz udział w zakończonym losowaniu, dane zostaną usunięte automatycznie po okresie przechowywania.</p>
        <Button variant="ghost" className="text-danger-700" onClick={() => setOpen(true)}>Usuń konto</Button>
      </CardBody>
      <ConfirmDialog
        open={open}
        title="Usunąć konto na zawsze?"
        tone="danger"
        confirmLabel="Usuń konto"
        loading={remove.isPending}
        onConfirm={() => remove.mutate()}
        onClose={() => { setOpen(false); remove.reset() }}
      >
        <p>Tej operacji nie można cofnąć. Wpisz hasło, aby potwierdzić.</p>
        <TextField label="Hasło" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} />
        {remove.isError && <Alert tone="error">{errorMessage(remove.error)}</Alert>}
      </ConfirmDialog>
    </Card>
  )
}
