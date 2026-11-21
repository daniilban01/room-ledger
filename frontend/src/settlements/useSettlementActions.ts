import { useMutation, useQueryClient } from '@tanstack/react-query'
import { errorMessage } from '../api/errorMessage'
import { settlementsApi } from './api'

/** Confirm / reject a pending payment. Shared by the group page, the activity feed and the settle-up page. */
export function useSettlementActions(groupId: string) {
  const queryClient = useQueryClient()
  // Success or error (e.g. 409 because it was already resolved): either way the screen is outdated, reload the group.
  const refreshGroup = () => queryClient.invalidateQueries({ queryKey: ['groups', groupId] })

  const confirm = useMutation({
    mutationFn: (settlementId: string) => settlementsApi.confirm(groupId, settlementId),
    onSettled: refreshGroup,
  })
  const reject = useMutation({
    mutationFn: (settlementId: string) => settlementsApi.reject(groupId, settlementId),
    onSettled: refreshGroup,
  })

  const error = confirm.error ?? reject.error
  return {
    confirm: (settlementId: string) => confirm.mutate(settlementId),
    reject: (settlementId: string) => reject.mutate(settlementId),
    busy: confirm.isPending || reject.isPending,
    errorText: error ? errorMessage(error, { 409: 'Plata fusese deja confirmată sau respinsă.' }) : null,
  }
}
