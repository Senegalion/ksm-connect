import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert } from '../../components/ui/Alert'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageLoader } from '../../components/ui/Spinner'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { NOTIFICATION_STATUS_LABELS, NOTIFICATION_STATUS_TONES } from '../../lib/statusLabels'
import { formatDateTime } from '../../lib/time'
import { adminService } from '../../services/adminService'
import type { AdminEventView } from '../../types/api'

export function NotificationsPanel({ event }: { event: AdminEventView }) {
  const queryClient = useQueryClient()
  const notifications = useQuery({
    queryKey: queryKeys.adminNotifications(event.id),
    queryFn: () => adminService.notifications(event.id),
    refetchInterval: (query) => (query.state.data?.some((entry) => entry.status !== 'SENT' && entry.status !== 'FAILED') ? 5000 : false),
  })
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.adminNotifications(event.id) })
    queryClient.invalidateQueries({ queryKey: queryKeys.adminEvent(event.id) })
  }
  const retryOne = useMutation({ mutationFn: (id: string) => adminService.retryNotification(event.id, id), onSuccess: refresh })
  const retryAll = useMutation({ mutationFn: () => adminService.retryFailed(event.id), onSuccess: refresh })

  if (notifications.isPending) return <PageLoader />
  if (notifications.isError) return <Alert tone="error">{errorMessage(notifications.error)}</Alert>
  if (notifications.data.length === 0) {
    return <EmptyState title="Brak powiadomień" description="Powiadomienia pojawią się tutaj po losowaniu." />
  }
  const failed = notifications.data.filter((entry) => entry.status === 'FAILED').length

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-ink-500">Wiadomości nie zawierają wyników innych osób. Ponowienie wysyłki nie zmienia losowania.</p>
        {failed > 0 && (
          <Button variant="secondary" size="sm" loading={retryAll.isPending} onClick={() => retryAll.mutate()}>
            Ponów nieudane ({failed})
          </Button>
        )}
      </div>
      {(retryOne.isError || retryAll.isError) && <Alert tone="error">{errorMessage(retryOne.error ?? retryAll.error)}</Alert>}
      <ul className="divide-y divide-line border-y border-line">
        {notifications.data.map((entry) => (
          <li key={entry.id} className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div>
                <p className="font-medium">
                  {entry.participantName ?? 'Uczestnik'}
                  <span className="ml-2 font-mono text-xs text-ink-500">{entry.channel === 'EMAIL' ? 'E-MAIL' : 'SMS'}</span>
                </p>
                <p className="text-[13px] text-ink-500">
                  Próby: {entry.attemptCount}/{entry.maxAttempts}
                  {entry.sentAt && <> · wysłano {formatDateTime(entry.sentAt, event.timezone)}</>}
                  {entry.status === 'RETRY_PENDING' && <> · kolejna próba {formatDateTime(entry.nextAttemptAt, event.timezone)}</>}
                  {entry.lastError && <> · błąd: <code>{entry.lastError}</code></>}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 self-start sm:self-center">
              <Badge tone={NOTIFICATION_STATUS_TONES[entry.status]}>{NOTIFICATION_STATUS_LABELS[entry.status]}</Badge>
              {(entry.status === 'FAILED' || entry.status === 'RETRY_PENDING') && (
                <Button size="sm" variant="ghost" loading={retryOne.isPending && retryOne.variables === entry.id} onClick={() => retryOne.mutate(entry.id)}>Ponów</Button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
