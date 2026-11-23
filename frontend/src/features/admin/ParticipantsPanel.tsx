import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageLoader } from '../../components/ui/Spinner'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { adminService } from '../../services/adminService'
import type { AdminEventView, ParticipantSummary } from '../../types/api'

export function ParticipantsPanel({ event }: { event: AdminEventView }) {
  const queryClient = useQueryClient()
  const [toRemove, setToRemove] = useState<ParticipantSummary | null>(null)
  const participants = useQuery({ queryKey: queryKeys.adminParticipants(event.id), queryFn: () => adminService.participants(event.id) })
  const remove = useMutation({
    mutationFn: (participantId: string) => adminService.removeParticipant(event.id, participantId),
    onSuccess: () => {
      setToRemove(null)
      queryClient.invalidateQueries({ queryKey: queryKeys.adminParticipants(event.id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.adminEvent(event.id) })
    },
  })
  const removable = event.status === 'REGISTRATION_OPEN' || event.status === 'REGISTRATION_CLOSED'

  if (participants.isPending) return <PageLoader />
  if (participants.isError) return <Alert tone="error">{errorMessage(participants.error)}</Alert>

  const active = participants.data.filter((participant) => participant.status === 'ACTIVE')
  const withdrawn = participants.data.filter((participant) => participant.status === 'WITHDRAWN')

  if (participants.data.length === 0) {
    return <EmptyState title="Nikt jeszcze nie dołączył" description="Udostępnij kod zaproszenia lub QR, aby uczestnicy mogli się zapisać." />
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-ink-500">Aktywni uczestnicy: <strong className="text-ink-900">{active.length}</strong>{withdrawn.length > 0 && <> · zrezygnowali: {withdrawn.length}</>}</p>
      {remove.isError && <Alert tone="error">{errorMessage(remove.error)}</Alert>}
      <ul className="divide-y divide-line border-y border-line">
        {[...active, ...withdrawn].map((participant) => (
          <li key={participant.participantId} className="grid gap-1 py-3 sm:grid-cols-[1fr_auto] sm:items-center sm:gap-4">
            <div className="min-w-0">
              <p className={participant.status === 'WITHDRAWN' ? 'text-ink-500 line-through' : 'font-medium'}>
                {participant.firstName} {participant.lastName}
                {participant.status === 'WITHDRAWN' && <span className="ml-2 text-[13px] text-ink-500 no-underline">zrezygnował(-a)</span>}
              </p>
              <p className="text-sm break-all text-ink-700">{participant.email}{participant.phoneNumber && <> · {participant.phoneNumber}</>}</p>
              <p className="text-[13px] text-ink-500">Dołączył(-a) {formatDateTime(participant.joinedAt, event.timezone)}</p>
            </div>
            {removable && participant.status === 'ACTIVE' && (
              <button type="button" className="min-h-10 justify-self-start text-sm text-danger-700 hover:underline" onClick={() => setToRemove(participant)}>Usuń z wydarzenia</button>
            )}
          </li>
        ))}
      </ul>
      <ConfirmDialog
        open={toRemove !== null}
        title="Usunąć uczestnika?"
        tone="danger"
        confirmLabel="Usuń"
        loading={remove.isPending}
        onConfirm={() => toRemove && remove.mutate(toRemove.participantId)}
        onClose={() => setToRemove(null)}
      >
        <p>{toRemove?.firstName} {toRemove?.lastName} nie weźmie udziału w losowaniu. Osoba może dołączyć ponownie kodem, dopóki zapisy są otwarte.</p>
      </ConfirmDialog>
    </div>
  )
}
