import { useMutation, useQueryClient } from '@tanstack/react-query'
import { errorMessage } from '../api/errorMessage'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { groupKeys, groupsApi, type GroupDetails } from './api'

export function InviteCard({ group }: { group: GroupDetails }) {
  const queryClient = useQueryClient()

  const regenerateMutation = useMutation({
    mutationFn: () => groupsApi.regenerateJoinCode(group.id),
    onSuccess: (updated) => queryClient.setQueryData(groupKeys.detail(group.id), updated),
  })

  return (
    <Card className="flex flex-col gap-3">
      <h2 className="font-display text-xl font-bold">Invită colegi</h2>
      <p className="text-ink/70">Trimite codul colegilor ca să intre în grup.</p>
      <p className="font-display text-2xl font-bold tracking-[0.2em]">{group.joinCode}</p>
      {group.myRole === 'ADMIN' && (
        <Button
          variant="secondary"
          className="self-start"
          disabled={regenerateMutation.isPending}
          onClick={() => regenerateMutation.mutate()}
        >
          Generează cod nou
        </Button>
      )}
      <ErrorText message={regenerateMutation.isError ? errorMessage(regenerateMutation.error) : null} />
    </Card>
  )
}
