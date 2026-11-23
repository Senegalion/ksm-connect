import { useMutation } from '@tanstack/react-query'
import { CircleCheck, CircleX } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router'
import { ButtonLink } from '../../components/ui/Button'
import { PageLoader } from '../../components/ui/Spinner'
import { errorMessage } from '../../lib/errors'
import { authService } from '../../services/authService'
import { tokenFromHash } from './TokenFromHash'

export function EmailVerification() {
  const location = useLocation()
  const [token] = useState(() => tokenFromHash(location.hash))
  const started = useRef(false)
  const verify = useMutation({ mutationFn: authService.verifyEmail })

  useEffect(() => {
    if (token && !started.current) {
      started.current = true
      window.history.replaceState(null, '', window.location.pathname)
      verify.mutate(token)
    }
  }, [token, verify])

  if (!token || verify.isError) {
    return (
      <div className="space-y-4 text-center">
        <CircleX className="mx-auto size-12 text-danger-700" aria-hidden />
        <p className="text-ink-700">{token ? errorMessage(verify.error) : 'Link jest niekompletny. Otwórz go ponownie z wiadomości e-mail.'}</p>
        <ButtonLink to="/login" variant="secondary" fullWidth>Przejdź do logowania</ButtonLink>
      </div>
    )
  }
  if (!verify.isSuccess) {
    return <PageLoader label="Potwierdzamy adres e-mail…" />
  }
  return (
    <div className="space-y-4 text-center">
      <CircleCheck className="mx-auto size-12 text-pine-700" aria-hidden />
      <p className="text-lg font-semibold">Adres e-mail potwierdzony!</p>
      <p className="text-sm text-ink-500">Możesz się teraz zalogować i dołączyć do wydarzenia.</p>
      <ButtonLink to="/login" fullWidth size="lg">Zaloguj się</ButtonLink>
    </div>
  )
}
