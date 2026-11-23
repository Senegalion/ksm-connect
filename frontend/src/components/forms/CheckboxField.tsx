import { useId, type InputHTMLAttributes, type ReactNode, type Ref } from 'react'

interface CheckboxFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: ReactNode
  error?: string
  ref?: Ref<HTMLInputElement>
}

export function CheckboxField({ label, error, id, ref, ...rest }: CheckboxFieldProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  return (
    <div className="space-y-1">
      <label htmlFor={fieldId} className="flex cursor-pointer items-start gap-3 rounded-md p-1 text-sm leading-relaxed text-ink-700">
        <input
          ref={ref}
          id={fieldId}
          type="checkbox"
          aria-invalid={error ? true : undefined}
          className="mt-0.5 size-5 shrink-0 accent-ink-900"
          {...rest}
        />
        <span>{label}</span>
      </label>
      {error && <p className="pl-9 text-sm text-danger-700">{error}</p>}
    </div>
  )
}
