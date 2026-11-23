import { useId, type InputHTMLAttributes, type ReactNode, type Ref, type TextareaHTMLAttributes } from 'react'
import { cn } from '../ui/cn'

const CONTROL = cn(
  'w-full rounded-xl border bg-white px-4 text-[16px] text-ink-900 placeholder:text-ink-300',
  'transition-colors focus:border-wine-600 focus:outline-none focus:ring-4 focus:ring-wine-100',
  'disabled:bg-cream-50 disabled:text-ink-500',
)

interface FieldShellProps {
  label: string
  error?: string
  hint?: ReactNode
  id: string
  children: ReactNode
}

function FieldShell({ label, error, hint, id, children }: FieldShellProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-semibold text-ink-700">{label}</label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="text-sm text-danger-700">{error}</p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-xs leading-relaxed text-ink-500">{hint}</p>
      ) : null}
    </div>
  )
}

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: ReactNode
  ref?: Ref<HTMLInputElement>
}

export function TextField({ label, error, hint, id, className, ref, ...rest }: TextFieldProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  return (
    <FieldShell label={label} error={error} hint={hint} id={fieldId}>
      <input
        ref={ref}
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fieldId}-error` : hint ? `${fieldId}-hint` : undefined}
        className={cn(CONTROL, 'min-h-12', error ? 'border-danger-700' : 'border-cream-300', className)}
        {...rest}
      />
    </FieldShell>
  )
}

interface TextAreaFieldProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
  hint?: ReactNode
  ref?: Ref<HTMLTextAreaElement>
}

export function TextAreaField({ label, error, hint, id, className, ref, ...rest }: TextAreaFieldProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  return (
    <FieldShell label={label} error={error} hint={hint} id={fieldId}>
      <textarea
        ref={ref}
        id={fieldId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${fieldId}-error` : hint ? `${fieldId}-hint` : undefined}
        className={cn(CONTROL, 'min-h-24 py-3 leading-relaxed', error ? 'border-danger-700' : 'border-cream-300', className)}
        {...rest}
      />
    </FieldShell>
  )
}
