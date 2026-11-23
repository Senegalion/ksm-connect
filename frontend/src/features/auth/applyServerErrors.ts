import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '../../lib/api'

const FIELD_ERROR_BY_CODE: Record<string, string> = {
  INVALID_PHONE_NUMBER: 'phoneNumber',
  WEAK_PASSWORD: 'password',
}

export function applyServerErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>, messages: Record<string, string>): boolean {
  if (!(error instanceof ApiError)) {
    return false
  }
  const field = FIELD_ERROR_BY_CODE[error.code]
  if (field && messages[error.code]) {
    setError(field as Path<T>, { message: messages[error.code] })
    return true
  }
  return false
}
