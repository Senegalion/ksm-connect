import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Card, CardBody } from '../../components/ui/Card'
import { Confetti } from '../../components/ui/Confetti'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { adminService } from '../../services/adminService'
import type { AdminEventView } from '../../types/api'

export function DrawPanel({ event }: { event: AdminEventView }) {
  const queryClient = useQueryClient()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const draw = useMutation({
    mutationFn: () => adminService.draw(event.id),
    onSettled: () => {
      setConfirmOpen(false)
      queryClient.invalidateQueries({ queryKey: queryKeys.adminEvent(event.id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.adminNotifications(event.id) })
    },
  })

  return (
    <Card className="relative">
      {draw.isSuccess && <Confetti spread={200} />}
      <CardBody className="space-y-4">
        <div className="flex items-center gap-2">
          
          <h2 className="text-xl font-semibold">Losowanie</h2>
        </div>

        {event.status === 'DRAW_COMPLETED' ? (
          <Alert tone="success" title="Losowanie zakończone">
            Pary wylosowano {formatDateTime(event.drawCompletedAt, event.timezone)}. Wynik jest ostateczny i nie można go zmienić – nikt, także organizator, nie widzi w aplikacji pełnej listy par.
          </Alert>
        ) : (
          <>
            {event.drawMode === 'SCHEDULED' && event.drawAt && (
              <p className="flex items-start gap-2 text-sm text-ink-700">
                
                Automatyczne losowanie: {formatDateTime(event.drawAt, event.timezone)}. Zapisy zamkną się wtedy same.
              </p>
            )}
            {event.drawOverdue && (
              <Alert tone="error" title="Losowanie automatyczne nie odbyło się">Sprawdź, czy zapisało się co najmniej dwóch uczestników. Możesz przeprowadzić losowanie ręcznie.</Alert>
            )}
            {event.status !== 'REGISTRATION_CLOSED' && event.status !== 'CANCELLED' && (
              <p className="text-sm text-ink-500">Losowanie będzie możliwe po zamknięciu zapisów.</p>
            )}
            {draw.isError && <Alert tone="error">{errorMessage(draw.error)}</Alert>}
            <Button
              size="lg"
              disabled={event.status !== 'REGISTRATION_CLOSED' || event.activeParticipants < 2}
              onClick={() => setConfirmOpen(true)}
            >
              Przeprowadź losowanie
            </Button>
          </>
        )}

        {draw.isSuccess && (
          <p className="rounded-xl bg-ksm-yellow-400 px-4 py-3 font-semibold text-ksm-blue-900">
            Wylosowano pary dla {draw.data.participantCount} osób. Powiadomienia są w drodze.
          </p>
        )}
      </CardBody>
      <ConfirmDialog
        open={confirmOpen}
        title="Przeprowadzić losowanie?"
        confirmLabel="Losuj pary"
        loading={draw.isPending}
        onConfirm={() => draw.mutate()}
        onClose={() => setConfirmOpen(false)}
      >
        <p>W losowaniu weźmie udział <strong>{event.activeParticipants}</strong> osób. Każdy dostanie jedną osobę do obdarowania i nikt nie wylosuje siebie.</p>
        <p>Wyniku <strong>nie da się cofnąć ani powtórzyć</strong>. Uczestnicy otrzymają e-mail i SMS.</p>
      </ConfirmDialog>
    </Card>
  )
}
