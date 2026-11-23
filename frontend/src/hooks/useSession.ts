import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../lib/queryKeys'
import { authService } from '../services/authService'

export function useSession() {
  const query = useQuery({
    queryKey: queryKeys.session,
    queryFn: authService.session,
    staleTime: 60_000,
  })
  const user = query.data?.authenticated ? query.data.user ?? null : null
  return {
    isLoading: query.isPending,
    user,
    isAdmin: user?.roles.includes('ADMIN') ?? false,
  }
}
