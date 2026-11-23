import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { TextAreaField, TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { cn } from '../../components/ui/cn'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { eventService } from '../../services/eventService'
import type { SuggestionType, WishlistItem, WishlistView } from '../../types/api'
import { suggestionSchema, type SuggestionValues } from './schema'
import { SuggestionDisplay } from './SuggestionDisplay'

interface SuggestionCardProps {
  eventId: string
  position: number
  item?: WishlistItem
  editable: boolean
}

export function SuggestionCard({ eventId, position, item, editable }: SuggestionCardProps) {
  const [editingType, setEditingType] = useState<SuggestionType | null>(null)
  const queryClient = useQueryClient()
  const updateCache = (view: WishlistView) => queryClient.setQueryData(queryKeys.wishlist(eventId), view)

  const remove = useMutation({
    mutationFn: () => eventService.removeSuggestion(eventId, position),
    onSuccess: updateCache,
  })

  const title = `Prezent ${position}`

  return (
    <article
      aria-label={title}
      className={cn('flex flex-col gap-3 border-t-2 pt-4', item ? 'border-ink-900' : 'border-line')}
    >
      <header className="flex items-baseline justify-between gap-2">
        <h3 className="flex items-baseline gap-2 text-base">
          <span className="font-mono text-sm text-ink-500">{String(position).padStart(2, '0')}</span>
          {title}
        </h3>
        {item && editable && !editingType && (
          <div className="flex gap-4 text-sm">
            <button type="button" className="min-h-10 text-ink-700 hover:underline" onClick={() => setEditingType(item.type)} aria-label={`Edytuj ${title}`}>Edytuj</button>
            <button type="button" className="min-h-10 text-danger-700 hover:underline disabled:opacity-50" onClick={() => remove.mutate()} disabled={remove.isPending} aria-label={`Usuń ${title}`}>Usuń</button>
          </div>
        )}
      </header>

      {remove.isError && <Alert tone="error">{errorMessage(remove.error)}</Alert>}

      {editingType ? (
        <SuggestionEditor
          eventId={eventId}
          position={position}
          initialType={editingType}
          item={item}
          onDone={(view) => {
            if (view) {
              updateCache(view)
            }
            setEditingType(null)
          }}
        />
      ) : item ? (
        <SuggestionDisplay item={item} />
      ) : editable ? (
        <div className="space-y-2">
          <p className="text-sm text-ink-500">Puste. Dodaj:</p>
          <div className="grid grid-cols-2 gap-2">
            <Button variant="secondary" onClick={() => setEditingType('URL')}>Link</Button>
            <Button variant="secondary" onClick={() => setEditingType('DESCRIPTION')}>Opis</Button>
          </div>
        </div>
      ) : (
        <p className="text-sm text-ink-500">Nie dodano propozycji.</p>
      )}
    </article>
  )
}

interface SuggestionEditorProps {
  eventId: string
  position: number
  initialType: SuggestionType
  item?: WishlistItem
  onDone: (view?: WishlistView) => void
}

function SuggestionEditor({ eventId, position, initialType, item, onDone }: SuggestionEditorProps) {
  const form = useForm<SuggestionValues>({
    resolver: zodResolver(suggestionSchema),
    defaultValues: initialType === 'URL'
      ? { type: 'URL', url: item?.type === 'URL' ? item.url ?? '' : '' }
      : { type: 'DESCRIPTION', description: item?.type === 'DESCRIPTION' ? item.description ?? '' : '' },
  })
  const type = useWatch({ control: form.control, name: 'type' })
  const save = useMutation({
    mutationFn: (values: SuggestionValues) => eventService.saveSuggestion(eventId, position, values),
    onSuccess: (view) => onDone(view),
  })
  const errors = form.formState.errors as Partial<Record<'url' | 'description', { message?: string }>>

  const switchType = (next: SuggestionType) => {
    if (next === type) {
      return
    }
    form.reset(next === 'URL' ? { type: 'URL', url: '' } : { type: 'DESCRIPTION', description: '' })
  }

  return (
    <form noValidate onSubmit={form.handleSubmit((values) => save.mutate(values))} className="space-y-3">
      <div role="radiogroup" aria-label="Rodzaj propozycji" className="grid grid-cols-2 border border-line-strong">
        {(['URL', 'DESCRIPTION'] as const).map((option) => (
          <button
            key={option}
            type="button"
            role="radio"
            aria-checked={type === option}
            onClick={() => switchType(option)}
            className={cn('min-h-10 text-sm transition-colors', type === option ? 'bg-ink-900 font-medium text-white' : 'text-ink-700 hover:bg-muted')}
          >
            {option === 'URL' ? 'Link do produktu' : 'Opis prezentu'}
          </button>
        ))}
      </div>
      {type === 'URL' ? (
        <TextField
          label="Link do produktu"
          type="url"
          inputMode="url"
          autoComplete="off"
          autoCapitalize="none"
          spellCheck={false}
          placeholder="https://"
          error={errors.url?.message}
          autoFocus
          {...form.register('url')}
        />
      ) : (
        <TextAreaField
          label="Opis prezentu"
          placeholder="np. dobra gra planszowa dla 2–4 osób"
          maxLength={500}
          error={errors.description?.message}
          autoFocus
          {...form.register('description')}
        />
      )}
      {save.isError && <Alert tone="error">{errorMessage(save.error)}</Alert>}
      <div className="flex gap-2">
        <Button type="submit" loading={save.isPending} className="flex-1">Zapisz</Button>
        <Button variant="ghost" onClick={() => onDone()}>Anuluj</Button>
      </div>
    </form>
  )
}
