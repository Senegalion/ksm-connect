import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card, CardBody } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import { adminService, type LifecycleAction } from '../../services/adminService'
import type { AdminEventView } from '../../types/api'

const DESCRIPTIONS: Record<AdminEventView['status'], string> = {
  DRAFT: 'Wydarzenie jest szkicem – uczestnicy nie mogą jeszcze dołączać.',
  REGISTRATION_OPEN: 'Uczestnicy mogą dołączać przy użyciu kodu zaproszenia.',
  REGISTRATION_CLOSED: 'Zapisy są zamknięte. Możesz przeprowadzić losowanie albo otworzyć zapisy ponownie.',
  DRAW_COMPLETED: 'Losowanie zakończone. Uczestnicy widzą swoje osoby i listy prezentów.',
  CANCELLED: 'Wydarzenie zostało odwołane.',
}

export function LifecyclePanel({ event }: { event: AdminEventView }) {
  const queryClient = useQueryClient()
  const [confirmCancel, setConfirmCancel] = useState(false)
  const change = useMutation({
    mutationFn: (action: LifecycleAction) => adminService.changeLifecycle(event.id, action),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.adminEvent(event.id), updated)
      queryClient.invalidateQueries({ queryKey: queryKeys.adminEvents })
      setConfirmCancel(false)
    },
  })

  const actions: Array<{ action: LifecycleAction; label: string; variant: 'primary' | 'secondary' | 'pine' }> = []
  if (event.status === 'DRAFT') actions.push({ action: 'open-registration', label: 'Otwórz zapisy', variant: 'pine' })
  if (event.status === 'REGISTRATION_OPEN') actions.push({ action: 'close-registration', label: 'Zamknij zapisy', variant: 'primary' })
  if (event.status === 'REGISTRATION_CLOSED') actions.push({ action: 'open-registration', label: 'Otwórz zapisy ponownie', variant: 'secondary' })
  const cancellable = event.status === 'DRAFT' || event.status === 'REGISTRATION_OPEN' || event.status === 'REGISTRATION_CLOSED'

  return (
    <Card>
      <CardBody className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-semibold">Status</h2>
          <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
        </div>
        <p className="text-sm leading-relaxed text-ink-700">{DESCRIPTIONS[event.status]}</p>
        <dl className="grid grid-cols-2 gap-3">
          <Stat label="Uczestnicy" value={event.participantLimit ? `${event.activeParticipants}/${event.participantLimit}` : String(event.activeParticipants)} />
          <Stat label="Wysłane powiadomienia" value={`${event.notifications.SENT}`} />
        </dl>
        {change.isError && <Alert tone="error">{errorMessage(change.error)}</Alert>}
        <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
          {actions.map(({ action, label, variant }) => (
            <Button key={action} variant={variant} loading={change.isPending && change.variables === action} onClick={() => change.mutate(action)}>{label}</Button>
          ))}
          {cancellable && <Button variant="ghost" onClick={() => setConfirmCancel(true)}>Odwołaj wydarzenie</Button>}
        </div>
      </CardBody>
      <ConfirmDialog
        open={confirmCancel}
        title="Odwołać wydarzenie?"
        tone="danger"
        confirmLabel="Odwołaj"
        loading={change.isPending}
        onConfirm={() => change.mutate('cancel')}
        onClose={() => setConfirmCancel(false)}
      >
        <p>Odwołania nie można cofnąć. Zaproszenia przestaną działać, a losowanie nie będzie możliwe.</p>
      </ConfirmDialog>
    </Card>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-muted px-4 py-3">
      <dt className="text-[13px] text-ink-500">{label}</dt>
      <dd className="display-wide text-3xl text-ksm-blue-800">{value}</dd>
    </div>
  )
}
