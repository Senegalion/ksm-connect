export interface FieldError {
  field: string
  code: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, fieldErrors: FieldError[] = []) {
    super(code)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

let csrfToken: { headerName: string; token: string } | null = null

async function loadCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, { credentials: 'include' })
  if (!response.ok) {
    throw new ApiError(response.status, 'CSRF_UNAVAILABLE')
  }
  csrfToken = (await response.json()) as { headerName: string; token: string }
  return csrfToken
}

export function resetCsrfToken() {
  csrfToken = null
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

async function send(method: Method, path: string, body: unknown, retryOnCsrf: boolean): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    const token = csrfToken ?? (await loadCsrfToken())
    headers[token.headerName] = token.token
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status === 403 && retryOnCsrf && method !== 'GET') {
    const problem = await response.clone().json().catch(() => null) as { code?: string } | null
    if (problem?.code === 'CSRF_INVALID') {
      resetCsrfToken()
      return send(method, path, body, false)
    }
  }
  return response
}

export async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  let response: Response
  try {
    response = await send(method, path, body, true)
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    throw new ApiError(0, 'NETWORK_ERROR')
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as { code?: string; fieldErrors?: FieldError[] }
    throw new ApiError(response.status, problem.code ?? 'UNKNOWN_ERROR', problem.fieldErrors ?? [])
  }
  if (response.status === 204 || response.status === 202 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {}),
  delete: <T>(path: string, body?: unknown) => request<T>('DELETE', path, body),
}
