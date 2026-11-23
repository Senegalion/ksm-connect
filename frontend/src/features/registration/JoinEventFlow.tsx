import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { TextField } from '../../components/forms/TextField'
import { Alert } from '../../components/ui/Alert'
import { Button, ButtonLink } from '../../components/ui/Button'
import { Card, CardBody } from '../../components/ui/Card'
import { Confetti } from '../../components/ui/Confetti'
import { useSession } from '../../hooks/useSession'
import { errorMessage } from '../../lib/errors'
import { clearPendingInvitation, invitationCodeFromHash, peekPendingInvitation, rememberPendingInvitation } from '../../lib/invitation'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { eventService } from '../../services/eventService'
import { EventMeta } from '../events/EventMeta'

export function JoinEventFlow() {
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, isLoading } = useSession()
  const [code, setCode] = useState(() => invitationCodeFromHash(location.hash) ?? peekPendingInvitation() ?? '')
  const autoPreviewed = useRef(false)

  const preview = useMutation({ mutationFn: eventService.previewInvitation })
  const accept = useMutation({
    mutationFn: eventService.acceptInvitation,
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.myEvents })
      queryClient.setQueryData(queryKeys.myEvent(result.event.id), result.event)
    },
  })

  useEffect(() => {
    if (location.hash) {
      window.history.replaceState(null, '', window.location.pathname)
    }
  }, [location.hash])

  useEffect(() => {
    if (user && code && !autoPreviewed.current) {
      autoPreviewed.current = true
      clearPendingInvitation()
      preview.mutate(code)
    }
  }, [user, code, preview])

  if (isLoading) {
    return null
  }

  if (!user) {
    return (
      <Card>
        <CardBody className="space-y-4">
          <h2 className="text-xl">Masz zaproszenie!</h2>
          <p className="text-sm leading-relaxed text-ink-500">Zaloguj się albo załóż konto – po zalogowaniu wrócisz tutaj, a kod zaproszenia zostanie wpisany automatycznie.</p>
          <div className="grid gap-2 sm:grid-cols-2">
            <Button size="lg" onClick={() => { if (code) rememberPendingInvitation(code); navigate('/login') }}>Zaloguj się</Button>
            <Button size="lg" variant="secondary" onClick={() => { if (code) rememberPendingInvitation(code); navigate('/register') }}>Załóż konto</Button>
          </div>
        </CardBody>
      </Card>
    )
  }

  if (accept.isSuccess) {
    const { event, newlyJoined } = accept.data
    return (
      <div className="relative">
        {newlyJoined && <Confetti />}
        <div className="relative animate-pop-in overflow-hidden rounded-3xl bg-ksm-yellow-400 p-6 text-ksm-blue-900 sm:p-8">
          <div className="pointer-events-none absolute -right-10 -bottom-14 size-40 rounded-full bg-ksm-red-600" aria-hidden />
          <div className="relative space-y-4">
            <h2 className="display-wide text-3xl text-ksm-blue-900">{newlyJoined ? 'Jesteś zapisany(-a)!' : 'Już jesteś na liście'}</h2>
            <p className="max-w-sm leading-relaxed font-medium">Dołączyłeś(-aś) do wydarzenia <strong>{event.name}</strong>. Teraz dodaj listę prezentów, żeby Twój Mikołaj wiedział, co sprawi Ci radość.</p>
            <ButtonLink to={`/events/${event.id}`} size="lg">Uzupełnij listę prezentów</ButtonLink>
          </div>
        </div>
      </div>
    )
  }

  const event = preview.data
  return (
    <div className="space-y-4">
      <Card>
        <CardBody>
          <form
            noValidate
            className="space-y-4"
            onSubmit={(formEvent) => {
              formEvent.preventDefault()
              if (code.trim()) {
                preview.mutate(code.trim())
              }
            }}
          >
            <TextField
              label="Kod zaproszenia"
              value={code}
              onChange={(changeEvent) => setCode(changeEvent.target.value)}
              placeholder="KSM-XXXXX-XXXXX"
              autoCapitalize="characters"
              autoComplete="off"
              spellCheck={false}
              className="font-mono tracking-wider uppercase"
              hint="Masz kod QR? Zeskanuj go aparatem w telefonie – otworzy się ta strona z wpisanym kodem."
            />
            {preview.isError && <Alert tone="error">{errorMessage(preview.error)}</Alert>}
            <Button type="submit" variant="secondary" fullWidth loading={preview.isPending} disabled={!code.trim()}>Sprawdź kod</Button>
          </form>
        </CardBody>
      </Card>

      {event && (
        <Card className="">
          <CardBody className="space-y-4">
            <div className="space-y-1">
              <p className="text-[13px] font-medium tracking-wide text-ink-500 uppercase">Zaproszenie do wydarzenia</p>
              <h2 className="text-2xl">{event.name}</h2>
            </div>
            {event.description && <p className="text-sm leading-relaxed whitespace-pre-line text-ink-700">{event.description}</p>}
            <EventMeta event={event} />
            {accept.isError && <Alert tone="error">{errorMessage(accept.error)}</Alert>}
            {event.participation ? (
              <ButtonLink to={`/events/${event.id}`} size="lg" fullWidth>Już jesteś zapisany(-a) – przejdź do wydarzenia</ButtonLink>
            ) : event.registrationState === 'OPEN' ? (
              <Button size="lg" fullWidth loading={accept.isPending} onClick={() => accept.mutate(code.trim())}>Dołączam!</Button>
            ) : (
              <Alert tone="info" title={event.registrationState === 'NOT_STARTED' ? 'Zapisy jeszcze nie ruszyły' : 'Zapisy są zamknięte'}>
                {event.registrationState === 'NOT_STARTED'
                  ? event.registrationOpensAt
                    ? `Zapiszesz się od ${formatDateTime(event.registrationOpensAt, event.timezone)}. Zachowaj ten link.`
                    : 'Organizator jeszcze nie otworzył zapisów. Spróbuj ponownie później.'
                  : 'Na to wydarzenie nie można się już zapisać. Jeśli to pomyłka, skontaktuj się z organizatorem.'}
              </Alert>
            )}
          </CardBody>
        </Card>
      )}
    </div>
  )
}
