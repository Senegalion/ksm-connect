import { api, resetCsrfToken } from '../lib/api'
import type { SessionUser } from '../types/api'

export interface ProfileInput {
  firstName: string
  lastName: string
  phoneNumber: string
}

export const accountService = {
  profile: () => api.get<SessionUser>('/api/me'),
  update: (input: ProfileInput) => api.put<SessionUser>('/api/me', input),
  async delete(password: string) {
    await api.delete<undefined>('/api/me', { password })
    resetCsrfToken()
  },
}
