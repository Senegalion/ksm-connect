import { api, resetCsrfToken } from '../lib/api'
import type { PublicInfo, SessionView } from '../types/api'

export interface RegisterInput {
  email: string
  password: string
  firstName: string
  lastName: string
  phoneNumber: string
  privacyNoticeAccepted: boolean
}

export const authService = {
  session: () => api.get<SessionView>('/api/auth/session'),
  publicInfo: () => api.get<PublicInfo>('/api/public/info'),
  register: (input: RegisterInput) => api.post<undefined>('/api/auth/register', input),
  verifyEmail: (token: string) => api.post<undefined>('/api/auth/verify-email', { token }),
  resendVerification: (email: string) => api.post<undefined>('/api/auth/resend-verification', { email }),
  requestPasswordReset: (email: string) => api.post<undefined>('/api/auth/password-reset/request', { email }),
  confirmPasswordReset: (token: string, newPassword: string) =>
    api.post<undefined>('/api/auth/password-reset/confirm', { token, newPassword }),
  async login(email: string, password: string) {
    const session = await api.post<SessionView>('/api/auth/login', { email, password })
    resetCsrfToken()
    return session
  },
  async logout() {
    await api.post<undefined>('/api/auth/logout')
    resetCsrfToken()
  },
  async changePassword(currentPassword: string, newPassword: string) {
    await api.post<undefined>('/api/auth/change-password', { currentPassword, newPassword })
    resetCsrfToken()
  },
}
