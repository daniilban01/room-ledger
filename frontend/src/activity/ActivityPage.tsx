import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { formatDayLabel, formatTime } from '../format/format'
import { settlementKeys, settlementsApi } from '../settlements/api'
import { PaymentStatus } from '../settlements/PaymentStatus'
import { useSettlementActions } from '../settlements/useSettlementActions'
import { Avatar } from '../ui/Avatar'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { Section } from '../ui/Section'
import { activityApi, activityKeys } from './api'
import { describeActivity } from './describeActivity'
import { buildFeed, type FeedItem } from './feed'

export function ActivityPage() {
  const groupId = useParams().groupId!
  const meId = useMe().data?.id
  const actions = useSettlementActions(groupId)

  // Infinite query: each "load more" fetches the next page and appends it to the list.
  const activityQuery = useInfiniteQuery({
    queryKey: activityKeys.list(groupId),
    queryFn: ({ pageParam }) => activityApi.list(groupId, pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.page + 1 < lastPage.totalPages ? lastPage.page + 1 : undefined),
  })
  const settlementsQuery = useQuery({
    queryKey: settlementKeys.list(groupId),
    queryFn: () => settlementsApi.list(groupId),
  })

  const events = activityQuery.data?.pages.flatMap((page) => page.content) ?? []
  const days = buildFeed(events, settlementsQuery.data ?? [])

  function renderItem(item: FeedItem) {
    const { event } = item
    // For a payment the avatar is the person who sent the money, not whoever clicked.
    const person =
      item.kind === 'payment'
        ? { id: event.payload.fromUserId, name: event.payload.fromName }
        : { id: event.actor.id, name: event.actor.displayName }

    return (
      <li key={event.id} className="flex gap-3 px-4 py-3">
        <Avatar name={person.name} id={person.id} />
        <div className="flex min-w-0 flex-1 flex-col gap-1.5">
          <div className="flex items-baseline justify-between gap-3">
            {item.kind === 'payment' ? (
              <Link
                to={`/groups/${groupId}/payments/${item.settlementId}`}
                className="underline-offset-2 hover:underline focus-visible:outline-2 focus-visible:outline-accent"
              >
                {describeActivity(event)}
              </Link>
            ) : (
              <p>{describeActivity(event)}</p>
            )}
            <time dateTime={event.createdAt} className="shrink-0 text-xs text-ink/50">
              {formatTime(event.createdAt)}
            </time>
          </div>
          {item.kind === 'payment' && (
            <PaymentStatus
              status={item.status}
              canAnswer={item.toUserId === meId}
              busy={actions.busy}
              onConfirm={() => actions.confirm(item.settlementId)}
              onReject={() => actions.reject(item.settlementId)}
            />
          )}
        </div>
      </li>
    )
  }

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-6">
      <PageHeader title="Activitate" backTo={`/groups/${groupId}`} />

      {activityQuery.isPending && <p className="text-ink/60">Se încarcă…</p>}
      {activityQuery.isError && <ErrorText message={errorMessage(activityQuery.error)} />}
      <ErrorText message={actions.errorText} />

      {days.map((day) => (
        <Section key={day.dayKey} title={formatDayLabel(day.dayKey)}>
          <Card padded={false}>
            <ol className="divide-y divide-border">{day.items.map(renderItem)}</ol>
          </Card>
        </Section>
      ))}

      {activityQuery.hasNextPage && (
        <Button variant="secondary" disabled={activityQuery.isFetchingNextPage} onClick={() => activityQuery.fetchNextPage()}>
          {activityQuery.isFetchingNextPage ? 'Se încarcă…' : 'Mai vechi'}
        </Button>
      )}
    </main>
  )
}
