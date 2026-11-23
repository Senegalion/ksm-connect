import { useMutation, useQueryClient } from '@tanstack/react-query'
import QRCode from 'qrcode'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Card, CardBody } from '../../components/ui/Card'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { adminService } from '../../services/adminService'
import type { AdminEventView, GeneratedInvitation } from '../../types/api'

const QR_OPTIONS = { width: 640, margin: 2, errorCorrectionLevel: 'M', color: { dark: '#2b2420', light: '#ffffff' } } as const

export function InvitationPanel({ event }: { event: AdminEventView }) {
  const queryClient = useQueryClient()
  const [generated, setGenerated] = useState<GeneratedInvitation | null>(null)
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [copied, setCopied] = useState<'code' | 'link' | null>(null)
  const canIssue = event.status !== 'DRAW_COMPLETED' && event.status !== 'CANCELLED'

  const regenerate = useMutation({
    mutationFn: async () => {
      const invitation = await adminService.regenerateInvitation(event.id)
      const qr = await QRCode.toDataURL(invitation.joinUrl, QR_OPTIONS).catch(() => null)
      return { invitation, qr }
    },
    onSuccess: ({ invitation, qr }) => {
      setGenerated(invitation)
      setQrDataUrl(qr)
      setConfirmOpen(false)
      queryClient.invalidateQueries({ queryKey: queryKeys.adminEvent(event.id) })
    },
  })

  const copy = async (value: string, kind: 'code' | 'link') => {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(kind)
      window.setTimeout(() => setCopied(null), 2000)
    } catch {
      setCopied(null)
    }
  }

  return (
    <Card>
      <CardBody className="space-y-4">
        <div className="flex items-center gap-2">
          
          <h2 className="text-xl font-semibold">Zaproszenie</h2>
        </div>

        {generated ? (
          <div className=" space-y-4">
            <Alert tone="info" title="Zapisz kod teraz">Ze względów bezpieczeństwa kod i QR są pokazywane tylko raz. Jeśli je zgubisz, wygeneruj nowe – stary kod przestanie działać.</Alert>
            <div className="grid items-center gap-4 sm:grid-cols-[auto_1fr]">
              {qrDataUrl && <img src={qrDataUrl} alt="Kod QR z linkiem do zapisów" className="mx-auto size-48 rounded-md border border-line bg-paper p-2 sm:size-44" />}
              <div className="space-y-3">
                <p className="font-mono text-2xl font-semibold tracking-wider text-ink-900 break-all" data-testid="invitation-code">{generated.code}</p>
                <div className="grid gap-2 sm:flex sm:flex-wrap">
                  <Button variant="secondary" size="sm" onClick={() => copy(generated.code, 'code')}>
                    {copied === 'code' ? 'Skopiowano' : 'Kopiuj kod'}
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => copy(generated.joinUrl, 'link')}>
                    {copied === 'link' ? 'Skopiowano' : 'Kopiuj link'}
                  </Button>
                  {qrDataUrl && (
                    <a href={qrDataUrl} download={`zaproszenie-${event.name.replace(/[^\p{L}\p{N}]+/gu, '-').toLowerCase()}.png`} className="inline-flex min-h-10 items-center justify-center gap-1.5 rounded-md border border-line-strong bg-paper px-3.5 text-sm font-semibold hover:bg-muted">
                      Pobierz QR
                    </a>
                  )}
                </div>
              </div>
            </div>
          </div>
        ) : event.invitation.exists ? (
          <p className="text-sm text-ink-700">
            Aktywne zaproszenie utworzono {formatDateTime(event.invitation.createdAt, event.timezone)}.
            {event.invitation.expiresAt && <> Wygasa {formatDateTime(event.invitation.expiresAt, event.timezone)}.</>}
            {!event.invitation.valid && <strong className="text-danger-700"> Zaproszenie wygasło.</strong>}
          </p>
        ) : (
          <p className="text-sm text-ink-500">Nie wygenerowano jeszcze kodu zaproszenia.</p>
        )}

        {regenerate.isError && <Alert tone="error">{errorMessage(regenerate.error)}</Alert>}
        {canIssue && (
          <Button
            variant={generated ? 'ghost' : 'primary'}
            loading={regenerate.isPending && !confirmOpen}
            onClick={() => (event.invitation.exists || generated ? setConfirmOpen(true) : regenerate.mutate())}
          >
            {event.invitation.exists || generated ? 'Wygeneruj nowy kod' : 'Wygeneruj kod i QR'}
          </Button>
        )}
      </CardBody>
      <ConfirmDialog
        open={confirmOpen}
        title="Wygenerować nowy kod?"
        confirmLabel="Tak, wygeneruj nowy"
        loading={regenerate.isPending}
        onConfirm={() => regenerate.mutate()}
        onClose={() => setConfirmOpen(false)}
      >
        <p>Dotychczasowy kod i QR natychmiast przestaną działać. Osoby, które już dołączyły, pozostaną na liście.</p>
      </ConfirmDialog>
    </Card>
  )
}
