import { useEffect, useId, useRef, type ReactNode } from 'react'
import { Button } from './Button'

interface ConfirmDialogProps {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  cancelLabel?: string
  tone?: 'primary' | 'danger'
  loading?: boolean
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmDialog({ open, title, children, confirmLabel, cancelLabel = 'Anuluj', tone = 'primary', loading, onConfirm, onClose }: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) {
      return
    }
    if (open && !dialog.open) {
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onClose={onClose}
      onCancel={(event) => {
        if (loading) {
          event.preventDefault()
        }
      }}
      className="m-auto w-[min(100%-2rem,28rem)] rounded-3xl border border-cream-200 bg-white p-0 text-ink-900 shadow-2xl backdrop:bg-ink-900/40 backdrop:backdrop-blur-[2px]"
    >
      <div className="space-y-4 p-6">
        <h2 id={titleId} className="text-xl font-semibold">{title}</h2>
        <div className="space-y-2 text-sm leading-relaxed text-ink-700">{children}</div>
        <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" onClick={onClose} disabled={loading}>{cancelLabel}</Button>
          <Button variant={tone === 'danger' ? 'danger' : 'primary'} onClick={onConfirm} loading={loading}>{confirmLabel}</Button>
        </div>
      </div>
    </dialog>
  )
}
