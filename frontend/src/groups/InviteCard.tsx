import { useMutation, useQueryClient } from '@tanstack/react-query'
import { errorMessage } from '../api/errorMessage'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { Section } from '../ui/Section'
import { groupKeys, groupsApi, type GroupDetails } from './api'

export function InviteCard({ group }: { group: GroupDetails }) {
  const queryClient = useQueryClient()

  const regenerateMutation = useMutation({
    mutationFn: () => groupsApi.regenerateJoinCode(group.id),
    onSuccess: (updated) => queryClient.setQueryData(groupKeys.detail(group.id), updated),
  })

  return (
    <Section title="Invită colegi">
      <Card className="flex flex-col gap-3">
        <p className="text-ink/70">Trimite-le codul ăsta ca să intre în grup:</p>
        <p className="rounded-xl bg-page py-3 text-center font-display text-2xl font-bold tracking-[0.2em]">
          {group.joinCode}
        </p>
        {group.myRole === 'ADMIN' && (
          <Button
            variant="secondary"
            size="sm"
            className="self-start"
            disabled={regenerateMutation.isPending}
            onClick={() => regenerateMutation.mutate()}
          >
            Generează alt cod
          </Button>
        )}
        <ErrorText message={regenerateMutation.isError ? errorMessage(regenerateMutation.error) : null} />
      </Card>
    </Section>
  )
}
