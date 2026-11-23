import { useQuery } from '@tanstack/react-query'
import { PageLoader } from '../../components/ui/Spinner'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { adminService } from '../../services/adminService'
import type { AdminEventView } from '../../types/api'

const LABELS: Record<string, string> = {
  EVENT_CREATED: 'Utworzono wydarzenie',
  EVENT_UPDATED: 'Zmieniono ustawienia',
  EVENT_STATUS_CHANGED: 'Zmieniono status',
  INVITATION_GENERATED: 'Wygenerowano zaproszenie',
  PARTICIPANT_REMOVED: 'Usunięto uczestnika',
  DRAW_COMPLETED: 'Przeprowadzono losowanie',
  NOTIFICATION_RETRY_REQUESTED: 'Ponowiono powiadomienia',
  PERSONAL_DATA_PURGED: 'Usunięto dane osobowe',
}

export function AuditPanel({ event }: { event: AdminEventView }) {
  const audit = useQuery({ queryKey: queryKeys.adminAudit(event.id), queryFn: () => adminService.audit(event.id) })
  if (audit.isPending) return <PageLoader />
  if (audit.isError || audit.data.length === 0) return <p className="text-sm text-ink-500">Brak wpisów.</p>
  return (
    <ol className="space-y-2 border-l-2 border-line-strong pl-4">
      {audit.data.map((entry, index) => (
        <li key={`${entry.createdAt}-${index}`} className="text-sm">
          <p className="font-semibold">{LABELS[entry.type] ?? entry.type}</p>
          <p className="text-xs text-ink-500">{formatDateTime(entry.createdAt, event.timezone)}</p>
        </li>
      ))}
    </ol>
  )
}
