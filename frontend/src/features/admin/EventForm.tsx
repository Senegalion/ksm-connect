import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { TextAreaField, TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { cn } from '../../components/ui/cn'
import { errorMessage } from '../../lib/errors'
import { DEFAULT_TIMEZONE, eventFormSchema, type EventFormValues } from './eventFormSchema'

interface EventFormProps {
  initialValues: EventFormValues
  submitLabel: string
  submitting: boolean
  error: unknown
  onSubmit: (values: EventFormValues) => void
}

export function EventForm({ initialValues, submitLabel, submitting, error, onSubmit }: EventFormProps) {
  const form = useForm<EventFormValues>({ resolver: zodResolver(eventFormSchema), defaultValues: initialValues })
  const { errors } = form.formState
  const drawMode = useWatch({ control: form.control, name: 'drawMode' })

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
      {error ? <Alert tone="error">{errorMessage(error)}</Alert> : null}
      <fieldset className="space-y-4">
        <legend className="mb-2 font-display text-lg font-semibold">Podstawowe informacje</legend>
        <TextField label="Nazwa wydarzenia" placeholder="np. Wigilia KSM 2026" error={errors.name?.message} {...form.register('name')} />
        <TextAreaField label="Opis" hint="Widoczny dla uczestników po wpisaniu kodu zaproszenia." error={errors.description?.message} {...form.register('description')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Miejsce" placeholder="np. Sala parafialna" error={errors.location?.message} {...form.register('location')} />
          <TextField label="Data i godzina spotkania" type="datetime-local" error={errors.startsAt?.message} {...form.register('startsAt')} />
        </div>
      </fieldset>

      <fieldset className="space-y-4">
        <legend className="mb-2 font-display text-lg font-semibold">Zapisy</legend>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Otwarcie zapisów (opcjonalnie)" type="datetime-local" error={errors.registrationOpensAt?.message} {...form.register('registrationOpensAt')} />
          <TextField label="Zamknięcie zapisów (opcjonalnie)" type="datetime-local" hint="Po tym czasie zapisy zamkną się automatycznie." error={errors.registrationClosesAt?.message} {...form.register('registrationClosesAt')} />
        </div>
        <TextField label="Limit uczestników (opcjonalnie)" type="number" inputMode="numeric" min={2} error={errors.participantLimit?.message} {...form.register('participantLimit')} />
      </fieldset>

      <fieldset className="space-y-4">
        <legend className="mb-2 font-display text-lg font-semibold">Losowanie</legend>
        <div role="radiogroup" aria-label="Tryb losowania" className="grid gap-2 sm:grid-cols-2">
          {([
            ['MANUAL', 'Ręcznie', 'Uruchamiasz losowanie przyciskiem po zamknięciu zapisów.'],
            ['SCHEDULED', 'Automatycznie', 'System zamknie zapisy i wylosuje pary o wskazanej godzinie.'],
          ] as const).map(([value, label, description]) => (
            <label
              key={value}
              className={cn('flex cursor-pointer gap-3 rounded-2xl border p-4 transition', drawMode === value ? 'border-wine-600 bg-wine-50' : 'border-cream-300 bg-white')}
            >
              <input type="radio" value={value} className="mt-1 size-4 accent-wine-700" {...form.register('drawMode')} />
              <span>
                <span className="block font-semibold">{label}</span>
                <span className="block text-sm text-ink-500">{description}</span>
              </span>
            </label>
          ))}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label={drawMode === 'SCHEDULED' ? 'Data i godzina losowania' : 'Planowana data losowania (informacyjnie)'}
            type="datetime-local"
            error={errors.drawAt?.message}
            {...form.register('drawAt')}
          />
          <TextField
            label="Edycja list prezentów do (opcjonalnie)"
            type="datetime-local"
            hint="Bez daty listy zamykają się w chwili losowania."
            error={errors.wishlistDeadline?.message}
            {...form.register('wishlistDeadline')}
          />
        </div>
        <p className="text-xs text-ink-500">Wszystkie godziny w strefie {DEFAULT_TIMEZONE.replace('_', ' ')}.</p>
      </fieldset>

      <Button type="submit" size="lg" loading={submitting} className="w-full sm:w-auto">{submitLabel}</Button>
    </form>
  )
}
