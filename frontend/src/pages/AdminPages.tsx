import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { ButtonLink } from '../components/ui/Button'
import { Card, CardBody } from '../components/ui/Card'
import { cn } from '../components/ui/cn'
import { EmptyState } from '../components/ui/EmptyState'
import { PageLoader } from '../components/ui/Spinner'
import { AdminEventCard } from '../features/admin/AdminEventCard'
import { AuditPanel } from '../features/admin/AuditPanel'
import { DrawPanel } from '../features/admin/DrawPanel'
import { EventForm } from '../features/admin/EventForm'
import { emptyEventForm, toFormValues, toRequest } from '../features/admin/eventFormSchema'
import { InvitationPanel } from '../features/admin/InvitationPanel'
import { LifecyclePanel } from '../features/admin/LifecyclePanel'
import { NotificationsPanel } from '../features/admin/NotificationsPanel'
import { ParticipantsPanel } from '../features/admin/ParticipantsPanel'
import { errorMessage } from '../lib/errors'
import { queryKeys } from '../lib/queryKeys'
import { adminService } from '../services/adminService'

export function AdminEventsPage() {
  const events = useQuery({ queryKey: queryKeys.adminEvents, queryFn: adminService.events })
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-[15px] font-semibold text-ksm-red-600">Panel organizatora</p>
          <h1 className="display-wide text-4xl leading-[1]">Wydarzenia</h1>
        </div>
        <ButtonLink to="/admin/events/new">Nowe wydarzenie</ButtonLink>
      </div>
      {events.isPending ? <PageLoader /> : events.isError ? <Alert tone="error">{errorMessage(events.error)}</Alert> : events.data.length === 0 ? (
        <EmptyState title="Brak wydarzeń" description="Utwórz pierwsze wydarzenie, np. Wigilię KSM." action={<ButtonLink to="/admin/events/new">Utwórz wydarzenie</ButtonLink>} />
      ) : (
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-3">
          {events.data.map((event) => <AdminEventCard key={event.id} event={event} />)}
        </div>
      )}
    </div>
  )
}

export function AdminCreateEventPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const create = useMutation({
    mutationFn: adminService.create,
    onSuccess: (event) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.adminEvents })
      navigate(`/admin/events/${event.id}`, { replace: true })
    },
  })
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <BackLink to="/admin" label="Wydarzenia" />
      <h1 className="display-wide text-4xl leading-[1]">Nowe wydarzenie</h1>
      <Card>
        <CardBody>
          <EventForm initialValues={emptyEventForm} submitLabel="Utwórz wydarzenie" submitting={create.isPending} error={create.error} onSubmit={(values) => create.mutate(toRequest(values))} />
        </CardBody>
      </Card>
    </div>
  )
}

type Tab = 'overview' | 'participants' | 'notifications' | 'settings'

const TABS: Array<{ id: Tab; label: string }> = [
  { id: 'overview', label: 'Przegląd' },
  { id: 'participants', label: 'Uczestnicy' },
  { id: 'notifications', label: 'Powiadomienia' },
  { id: 'settings', label: 'Ustawienia' },
]

export function AdminEventPage() {
  const { eventId = '' } = useParams()
  const [tab, setTab] = useState<Tab>('overview')
  const queryClient = useQueryClient()
  const event = useQuery({ queryKey: queryKeys.adminEvent(eventId), queryFn: () => adminService.event(eventId) })
  const update = useMutation({
    mutationFn: (values: Parameters<typeof toRequest>[0]) => adminService.update(eventId, toRequest(values, event.data?.timezone)),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.adminEvent(eventId), updated)
      queryClient.invalidateQueries({ queryKey: queryKeys.adminEvents })
    },
  })

  if (event.isPending) return <PageLoader />
  if (event.isError) return <Alert tone="error">{errorMessage(event.error)}</Alert>
  const data = event.data
  const configurable = data.status === 'DRAFT' || data.status === 'REGISTRATION_OPEN' || data.status === 'REGISTRATION_CLOSED'

  return (
    <div className="space-y-6">
      <BackLink to="/admin" label="Wydarzenia" />
      <h1 className="display-wide text-4xl leading-[1]">{data.name}</h1>
      <div role="tablist" aria-label="Sekcje wydarzenia" className="-mx-4 flex gap-6 overflow-x-auto border-b border-line px-4 md:mx-0 md:px-0">
        {TABS.map(({ id, label }) => (
          <button
            key={id}
            role="tab"
            type="button"
            aria-selected={tab === id}
            onClick={() => setTab(id)}
            className={cn('min-h-11 shrink-0 border-b-[3px] text-[15px] transition-colors', tab === id ? 'border-ksm-yellow-400 font-semibold text-ksm-blue-800' : 'border-transparent text-ink-500 hover:text-ksm-blue-800')}
          >
            {label}
          </button>
        ))}
      </div>

      <div role="tabpanel">
        {tab === 'overview' && (
          <div className="grid gap-4 lg:grid-cols-2">
            <LifecyclePanel event={data} />
            <DrawPanel event={data} />
            <InvitationPanel event={data} />
            <Card>
              <CardBody className="space-y-3">
                <h2 className="text-xl font-semibold">Historia</h2>
                <AuditPanel event={data} />
              </CardBody>
            </Card>
          </div>
        )}
        {tab === 'participants' && <ParticipantsPanel event={data} />}
        {tab === 'notifications' && <NotificationsPanel event={data} />}
        {tab === 'settings' && (
          <Card>
            <CardBody className="space-y-4">
              {!configurable && <Alert tone="info">Ustawień nie można już zmieniać.</Alert>}
              {update.isSuccess && <Alert tone="success">Zapisano zmiany.</Alert>}
              {configurable && (
                <EventForm key={data.updatedAt} initialValues={toFormValues(data)} submitLabel="Zapisz zmiany" submitting={update.isPending} error={update.error} onSubmit={(values) => update.mutate(values)} />
              )}
            </CardBody>
          </Card>
        )}
      </div>
    </div>
  )
}

function BackLink({ to, label }: { to: string; label: string }) {
  return (
    <Link to={to} className="inline-flex min-h-10 items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900">
      ← {label}
    </Link>
  )
}
