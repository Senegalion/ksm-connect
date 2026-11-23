import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Mail, Phone, UserMinus, Users } from 'lucide-react'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
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
    return <EmptyState icon={<Users className="size-6" aria-hidden />} title="Nikt jeszcze nie dołączył" description="Udostępnij kod zaproszenia lub QR, aby uczestnicy mogli się zapisać." />
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-ink-500">Aktywni uczestnicy: <strong className="text-ink-900">{active.length}</strong>{withdrawn.length > 0 && <> · zrezygnowali: {withdrawn.length}</>}</p>
      {remove.isError && <Alert tone="error">{errorMessage(remove.error)}</Alert>}
      <ul className="grid gap-3 sm:grid-cols-2">
        {[...active, ...withdrawn].map((participant) => (
          <li key={participant.participantId} className="space-y-2 rounded-2xl border border-cream-200 bg-white p-4">
            <div className="flex items-start justify-between gap-2">
              <p className="font-semibold">{participant.firstName} {participant.lastName}</p>
              {participant.status === 'WITHDRAWN' && <Badge tone="neutral">Zrezygnował(-a)</Badge>}
            </div>
            <p className="flex items-center gap-2 text-sm text-ink-700 break-all"><Mail className="size-4 shrink-0 text-ink-300" aria-hidden />{participant.email}</p>
            {participant.phoneNumber && <p className="flex items-center gap-2 text-sm text-ink-700"><Phone className="size-4 shrink-0 text-ink-300" aria-hidden />{participant.phoneNumber}</p>}
            <p className="text-xs text-ink-500">Dołączył(-a): {formatDateTime(participant.joinedAt, event.timezone)}</p>
            {removable && participant.status === 'ACTIVE' && (
              <Button size="sm" variant="ghost" icon={<UserMinus className="size-4" aria-hidden />} onClick={() => setToRemove(participant)}>Usuń z wydarzenia</Button>
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
