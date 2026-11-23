import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link2, PenLine, Plus, Trash2 } from 'lucide-react'
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
      className={cn(
        'flex flex-col gap-3 rounded-[var(--radius-card)] border p-4 transition sm:p-5',
        item ? 'border-cream-200 bg-white shadow-[var(--shadow-card)]' : 'border-dashed border-cream-300 bg-cream-50/60',
      )}
    >
      <header className="flex items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 font-display text-lg font-semibold">
          <span className={cn('flex size-7 items-center justify-center rounded-full text-sm font-sans', item ? 'bg-wine-700 text-white' : 'bg-cream-200 text-ink-500')}>{position}</span>
          {title}
        </h3>
        {item && editable && !editingType && (
          <div className="flex gap-1">
            <Button size="sm" variant="ghost" onClick={() => setEditingType(item.type)} aria-label={`Edytuj ${title}`}>Edytuj</Button>
            <Button size="sm" variant="ghost" onClick={() => remove.mutate()} loading={remove.isPending} aria-label={`Usuń ${title}`} icon={<Trash2 className="size-4" aria-hidden />} />
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
        <div className="grid grid-cols-2 gap-2">
          <Button variant="secondary" onClick={() => setEditingType('URL')} icon={<Link2 className="size-4" aria-hidden />}>Link</Button>
          <Button variant="secondary" onClick={() => setEditingType('DESCRIPTION')} icon={<PenLine className="size-4" aria-hidden />}>Opis</Button>
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
      <div role="radiogroup" aria-label="Rodzaj propozycji" className="grid grid-cols-2 gap-1 rounded-xl bg-cream-200/70 p-1">
        {(['URL', 'DESCRIPTION'] as const).map((option) => (
          <button
            key={option}
            type="button"
            role="radio"
            aria-checked={type === option}
            onClick={() => switchType(option)}
            className={cn('min-h-10 rounded-lg text-sm font-semibold transition', type === option ? 'bg-white text-wine-700 shadow-sm' : 'text-ink-500')}
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
        <Button type="submit" loading={save.isPending} icon={<Plus className="size-4" aria-hidden />} className="flex-1">Zapisz</Button>
        <Button variant="ghost" onClick={() => onDone()}>Anuluj</Button>
      </div>
    </form>
  )
}
