import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { balanceKeys, balancesApi, type Transfer } from '../balances/api'
import type { UserSummary } from '../expenses/api'
import { centsFromApi, formatAmount } from '../format/format'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { Section } from '../ui/Section'
import { TextField } from '../ui/TextField'
import { settlementKeys, settlementsApi, waitingForAnswer } from './api'
import { PaymentRow } from './PaymentRow'
import { PaymentStatus } from './PaymentStatus'
import { useSettlementActions } from './useSettlementActions'

const transferKey = (transfer: Transfer) => `${transfer.from.id}-${transfer.to.id}`

export function PaymentsPage() {
  const groupId = useParams().groupId!
  const queryClient = useQueryClient()
  const meId = useMe().data?.id
  const actions = useSettlementActions(groupId)

  // The transfer whose "add a note and confirm" form is open, and the note being typed.
  const [openTransfer, setOpenTransfer] = useState<string | null>(null)
  const [note, setNote] = useState('')

  const transfersQuery = useQuery({
    queryKey: balanceKeys.simplified(groupId),
    queryFn: () => balancesApi.simplified(groupId),
  })
  const settlementsQuery = useQuery({
    queryKey: settlementKeys.list(groupId),
    queryFn: () => settlementsApi.list(groupId),
  })

  const createMutation = useMutation({
    mutationFn: (transfer: Transfer) =>
      settlementsApi.create(groupId, transfer.from.id, transfer.to.id, transfer.amount, note.trim() || undefined),
    onSuccess: () => {
      setOpenTransfer(null)
      setNote('')
    },
    // Success or error: the screen is outdated either way, reload the group.
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['groups', groupId] }),
  })

  const busy = createMutation.isPending || actions.busy
  const settlements = settlementsQuery.data ?? []
  const toAnswer = waitingForAnswer(settlements, meId)
  const history = settlements.filter((settlement) => !toAnswer.includes(settlement))
  const transfers = transfersQuery.data?.transfers ?? []

  const name = (user: UserSummary) => (user.id === meId ? 'Tu' : user.displayName)
  const alreadySent = (transfer: Transfer) =>
    settlements.some(
      (s) => s.status === 'PENDING' && s.from.id === transfer.from.id && s.to.id === transfer.to.id,
    )

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-6">
      <PageHeader title="Plăți" backTo={`/groups/${groupId}`} />

      {(transfersQuery.isPending || settlementsQuery.isPending) && <p className="text-ink/60">Se încarcă…</p>}
      {transfersQuery.isError && <ErrorText message={errorMessage(transfersQuery.error)} />}
      {settlementsQuery.isError && <ErrorText message={errorMessage(settlementsQuery.error)} />}
      <ErrorText message={createMutation.isError ? errorMessage(createMutation.error) : actions.errorText} />

      {toAnswer.length > 0 && (
        <Section title="De confirmat">
          <Card padded={false}>
            <ul className="divide-y divide-border">
              {toAnswer.map((settlement) => (
                <PaymentRow
                  key={settlement.id}
                  groupId={groupId}
                  settlement={settlement}
                  meId={meId}
                  actions={
                    <PaymentStatus
                      status={settlement.status}
                      canAnswer
                      busy={busy}
                      onConfirm={() => actions.confirm(settlement.id)}
                      onReject={() => actions.reject(settlement.id)}
                    />
                  }
                />
              ))}
            </ul>
          </Card>
        </Section>
      )}

      {transfersQuery.data && (
        <Section title="Cum vă puneți la zi">
          {transfers.length === 0 ? (
            <Card>
              <p className="font-medium">Sunteți la zi.</p>
              <p className="text-ink/70">Nimeni nu datorează nimic nimănui.</p>
            </Card>
          ) : (
            <Card padded={false}>
              <ul className="divide-y divide-border">
                {transfers.map((transfer) => {
                  const key = transferKey(transfer)
                  const mine = transfer.from.id === meId || transfer.to.id === meId
                  const sent = alreadySent(transfer)
                  return (
                    <li key={key} className="flex flex-col gap-3 px-4 py-3">
                      <div className="flex items-baseline justify-between gap-3">
                        <span>
                          {name(transfer.from)} <span aria-hidden="true">→</span>
                          <span className="sr-only">îi trimite lui</span> {name(transfer.to)}
                        </span>
                        <span className="shrink-0 font-display font-bold">
                          {formatAmount(centsFromApi(transfer.amount))}
                        </span>
                      </div>

                      {sent && <PaymentStatus status="PENDING" canAnswer={false} />}

                      {mine && !sent && openTransfer !== key && (
                        <Button variant="secondary" size="sm" className="self-start" onClick={() => setOpenTransfer(key)}>
                          {transfer.from.id === meId ? 'Am trimis banii' : 'Am primit banii'}
                        </Button>
                      )}

                      {mine && !sent && openTransfer === key && (
                        <form
                          className="flex flex-col gap-3 rounded-2xl bg-page p-3"
                          onSubmit={(event) => {
                            event.preventDefault()
                            createMutation.mutate(transfer)
                          }}
                        >
                          <TextField
                            label="Notă (opțional)"
                            placeholder="ex. Revolut, chiria pe noiembrie"
                            maxLength={200}
                            value={note}
                            onChange={(event) => setNote(event.target.value)}
                          />
                          <div className="flex gap-2">
                            <Button type="submit" size="sm" disabled={busy}>
                              Salvează
                            </Button>
                            <Button size="sm" variant="secondary" onClick={() => setOpenTransfer(null)}>
                              Renunță
                            </Button>
                          </div>
                        </form>
                      )}
                    </li>
                  )
                })}
              </ul>
            </Card>
          )}
        </Section>
      )}

      {history.length > 0 && (
        <Section title="Istoric">
          <Card padded={false}>
            <ul className="divide-y divide-border">
              {history.map((settlement) => (
                <PaymentRow key={settlement.id} groupId={groupId} settlement={settlement} meId={meId} />
              ))}
            </ul>
          </Card>
        </Section>
      )}
    </main>
  )
}
