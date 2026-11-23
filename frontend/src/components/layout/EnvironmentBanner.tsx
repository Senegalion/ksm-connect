import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../../lib/queryKeys'
import { authService } from '../../services/authService'

export function EnvironmentBanner() {
  const info = useQuery({ queryKey: queryKeys.publicInfo, queryFn: authService.publicInfo, staleTime: Infinity })
  if (info.data?.environment !== 'test') {
    return null
  }
  return (
    <div role="note" className="bg-ksm-red-600 px-4 py-1.5 text-center text-[13px] font-bold tracking-wide text-white">
      ŚRODOWISKO TESTOWE · dane fikcyjne · maile trafiają do Mailpit
    </div>
  )
}
