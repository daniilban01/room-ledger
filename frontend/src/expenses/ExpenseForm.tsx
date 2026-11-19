import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import type { Me } from '../auth/useMe'
import { centsFromApi, centsToApi, centsToInput, formatAmount, parseAmount, todayIso } from '../format/format'
import type { GroupDetails } from '../groups/api'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { TextField } from '../ui/TextField'
import { expensesApi, type Expense, type ExpenseRequest, type SplitType } from './api'
import { itemsTotal, splitEqual, splitExact, splitItems, type SplitResult } from './split'

type ItemRow = { key: string; name: string; amountInput: string; assignees: string[] }

type Props = { me: Me; group: GroupDetails; expense?: Expense }

const splitTypeLabels: Record<SplitType, string> = {
  EQUAL: 'Egal',
  EXACT: 'Sume exacte',
  ITEMS: 'Pe produse',
}

export function ExpenseForm({ me, group, expense }: Props) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const members = group.members
  const allMemberIds = members.map((member) => member.userId)

  // Initial state comes from the expense when editing, from sensible defaults when creating.
  const [description, setDescription] = useState(expense?.description ?? '')
  const [amountInput, setAmountInput] = useState(expense ? centsToInput(centsFromApi(expense.totalAmount)) : '')
  const [paidBy, setPaidBy] = useState(expense?.paidBy.id ?? me.id)
  const [expenseDate, setExpenseDate] = useState(() => expense?.expenseDate ?? todayIso())
  const [splitType, setSplitType] = useState<SplitType>(expense?.splitType ?? 'EQUAL')
  const [participants, setParticipants] = useState<string[]>(
    expense?.splitType === 'EQUAL' ? expense.shares.map((share) => share.userId) : allMemberIds,
  )
  const [exactInputs, setExactInputs] = useState<Record<string, string>>(() =>
    expense?.splitType === 'EXACT'
      ? Object.fromEntries(expense.shares.map((share) => [share.userId, centsToInput(centsFromApi(share.amount))]))
      : {},
  )
  const [items, setItems] = useState<ItemRow[]>(() =>
    expense?.splitType === 'ITEMS'
      ? expense.items.map((item) => ({
          key: item.id,
          name: item.name,
          amountInput: centsToInput(centsFromApi(item.amount)),
          assignees: item.assignedTo,
        }))
      : [],
  )
  const nextItemKey = useRef(0)

  const canEdit = !expense || expense.createdBy.id === me.id || group.myRole === 'ADMIN'

  // Derived on every render instead of stored: the preview can never be out of sync with the inputs.
  const itemDrafts = items.map((item) => ({ amount: parseAmount(item.amountInput), assignees: item.assignees }))
  const totalCents = splitType === 'ITEMS' ? itemsTotal(itemDrafts) : parseAmount(amountInput)
  const split = calculateSplit()

  function calculateSplit(): SplitResult {
    if (splitType === 'EQUAL') {
      return splitEqual(totalCents, participants)
    }
    if (splitType === 'EXACT') {
      // An empty input means "owes nothing".
      const amounts = new Map(allMemberIds.map((id) => [id, parseAmount(exactInputs[id] || '0')]))
      return splitExact(totalCents, amounts)
    }
    return splitItems(itemDrafts)
  }

  const saveMutation = useMutation({
    mutationFn: (request: ExpenseRequest) =>
      expense ? expensesApi.update(group.id, expense.id, request) : expensesApi.create(group.id, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['groups', group.id] })
      navigate(`/groups/${group.id}`)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => expensesApi.remove(group.id, expense!.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['groups', group.id] })
      navigate(`/groups/${group.id}`)
    },
  })

  function buildRequest(): ExpenseRequest | null {
    if (!split.ok || totalCents === null) {
      return null
    }
    const request: ExpenseRequest = {
      description: description.trim(),
      totalAmount: centsToApi(totalCents),
      paidBy,
      expenseDate,
      splitType,
      version: expense?.version,
    }
    if (splitType === 'EQUAL') {
      request.participants = participants
    } else if (splitType === 'EXACT') {
      request.shares = [...split.shares].map(([userId, cents]) => ({ userId, amount: centsToApi(cents) }))
    } else {
      request.items = items.map((item) => ({
        name: item.name.trim(),
        amount: centsToApi(parseAmount(item.amountInput) ?? 0),
        assignedTo: item.assignees,
      }))
    }
    return request
  }

  function changeSplitType(type: SplitType) {
    setSplitType(type)
    if (type === 'ITEMS' && items.length === 0) {
      addItem()
    }
  }

  function toggleParticipant(userId: string) {
    setParticipants((current) =>
      current.includes(userId) ? current.filter((id) => id !== userId) : [...current, userId],
    )
  }

  function addItem() {
    nextItemKey.current += 1
    setItems((current) => [
      ...current,
      { key: `new-${nextItemKey.current}`, name: '', amountInput: '', assignees: allMemberIds },
    ])
  }

  function updateItem(key: string, change: Partial<ItemRow>) {
    setItems((current) => current.map((item) => (item.key === key ? { ...item, ...change } : item)))
  }

  function toggleItemAssignee(item: ItemRow, userId: string) {
    const assignees = item.assignees.includes(userId)
      ? item.assignees.filter((id) => id !== userId)
      : [...item.assignees, userId]
    updateItem(item.key, { assignees })
  }

  const saveError = saveMutation.isError
    ? errorMessage(saveMutation.error, {
        409: 'Cheltuiala a fost modificată între timp de altcineva. Reîncarcă pagina și încearcă din nou.',
        403: 'Nu ai voie să modifici această cheltuială.',
      })
    : null

  return (
    <form
      className="flex flex-col gap-6"
      onSubmit={(event) => {
        event.preventDefault()
        const request = buildRequest()
        if (request) {
          saveMutation.mutate(request)
        }
      }}
    >
      {!canEdit && (
        <p className="rounded-2xl border border-border bg-surface p-4">
          Doar cine a adăugat cheltuiala sau un admin al grupului o poate modifica.
        </p>
      )}

      {/* A disabled fieldset disables every input inside it in one place. */}
      <fieldset disabled={!canEdit} className="flex flex-col gap-6">
        <Card className="flex flex-col gap-4">
          <TextField
            label="Descriere"
            placeholder="ex. Cumpărături Kaufland"
            required
            maxLength={200}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          {splitType !== 'ITEMS' && (
            <TextField
              label="Sumă (lei)"
              inputMode="decimal"
              placeholder="0,00"
              required
              value={amountInput}
              onChange={(event) => setAmountInput(event.target.value)}
            />
          )}
          <div className="flex flex-col gap-1.5">
            <label htmlFor="paid-by" className="text-sm font-medium">
              Cine a plătit
            </label>
            <select
              id="paid-by"
              value={paidBy}
              onChange={(event) => setPaidBy(event.target.value)}
              className="min-h-11 rounded-xl border border-border bg-surface px-3 text-base focus-visible:border-accent focus-visible:ring-2 focus-visible:ring-accent/30 focus-visible:outline-none"
            >
              {members.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.userId === me.id ? `${member.displayName} (eu)` : member.displayName}
                </option>
              ))}
            </select>
          </div>
          <TextField
            label="Data"
            type="date"
            required
            value={expenseDate}
            onChange={(event) => setExpenseDate(event.target.value)}
          />
        </Card>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm font-medium">Cum se împarte</legend>
          <div className="grid grid-cols-3 gap-2">
            {(Object.keys(splitTypeLabels) as SplitType[]).map((type) => (
              <label
                key={type}
                className="flex min-h-11 cursor-pointer items-center justify-center rounded-xl border border-border bg-surface px-2 text-center text-sm font-medium has-checked:border-ink has-checked:bg-ink has-checked:text-white has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-accent"
              >
                <input
                  type="radio"
                  name="split-type"
                  value={type}
                  checked={splitType === type}
                  onChange={() => changeSplitType(type)}
                  className="sr-only"
                />
                {splitTypeLabels[type]}
              </label>
            ))}
          </div>
        </fieldset>

        {splitType === 'EQUAL' && (
          <Card>
            <fieldset className="flex flex-col">
              <legend className="mb-2 font-display text-lg font-bold">Între cine</legend>
              {members.map((member) => (
                <label key={member.userId} className="flex min-h-11 cursor-pointer items-center gap-3">
                  <input
                    type="checkbox"
                    checked={participants.includes(member.userId)}
                    onChange={() => toggleParticipant(member.userId)}
                    className="size-5 accent-accent"
                  />
                  {member.displayName}
                </label>
              ))}
            </fieldset>
          </Card>
        )}

        {splitType === 'EXACT' && (
          <Card className="flex flex-col gap-3">
            <h2 className="font-display text-lg font-bold">Cât datorează fiecare</h2>
            {members.map((member) => (
              <TextField
                key={member.userId}
                label={member.displayName}
                inputMode="decimal"
                placeholder="0,00"
                value={exactInputs[member.userId] ?? ''}
                onChange={(event) => setExactInputs((current) => ({ ...current, [member.userId]: event.target.value }))}
              />
            ))}
          </Card>
        )}

        {splitType === 'ITEMS' && (
          <div className="flex flex-col gap-3">
            {items.map((item, index) => (
              <Card key={item.key} className="flex flex-col gap-3">
                <div className="grid grid-cols-[1fr_7rem] gap-2">
                  <TextField
                    label={`Produs ${index + 1}`}
                    required
                    maxLength={200}
                    value={item.name}
                    onChange={(event) => updateItem(item.key, { name: event.target.value })}
                  />
                  <TextField
                    label="Preț"
                    inputMode="decimal"
                    placeholder="0,00"
                    required
                    value={item.amountInput}
                    onChange={(event) => updateItem(item.key, { amountInput: event.target.value })}
                  />
                </div>
                <div className="flex flex-wrap gap-2" role="group" aria-label={`Cine a consumat produsul ${index + 1}`}>
                  {members.map((member) => {
                    const selected = item.assignees.includes(member.userId)
                    return (
                      <button
                        key={member.userId}
                        type="button"
                        aria-pressed={selected}
                        onClick={() => toggleItemAssignee(item, member.userId)}
                        className={`min-h-11 rounded-full border px-4 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent ${
                          selected ? 'border-accent bg-accent text-white' : 'border-border bg-surface'
                        }`}
                      >
                        {member.displayName}
                      </button>
                    )
                  })}
                </div>
                <Button
                  variant="secondary"
                  className="self-start"
                  onClick={() => setItems((current) => current.filter((row) => row.key !== item.key))}
                >
                  Elimină produsul
                </Button>
              </Card>
            ))}
            <Button variant="secondary" onClick={addItem}>
              + Adaugă produs
            </Button>
          </div>
        )}
      </fieldset>

      <section aria-labelledby="preview-title" className="flex flex-col gap-3 rounded-[20px] bg-ink p-5 text-white">
        <div className="flex items-baseline justify-between gap-4">
          <h2 id="preview-title" className="font-display text-lg font-bold">
            Cine cât datorează
          </h2>
          {totalCents !== null && totalCents > 0 && (
            <span className="font-display text-xl font-bold">{formatAmount(totalCents)}</span>
          )}
        </div>
        {/* aria-live: screen readers announce the new split as the user types. */}
        <div aria-live="polite">
          {split.ok ? (
            <ul className="flex flex-col divide-y divide-white/15">
              {members
                .filter((member) => split.shares.has(member.userId))
                .map((member) => (
                  <li key={member.userId} className="flex min-h-10 items-center justify-between">
                    <span>{member.displayName}</span>
                    <span className="font-display font-bold">{formatAmount(split.shares.get(member.userId)!)}</span>
                  </li>
                ))}
            </ul>
          ) : (
            <p className="text-white/80">{split.error}</p>
          )}
        </div>
      </section>

      <ErrorText message={saveError} />

      {canEdit && (
        <div className="flex flex-col gap-3">
          <Button type="submit" disabled={!split.ok || saveMutation.isPending}>
            {saveMutation.isPending ? 'Se salvează…' : 'Salvează cheltuiala'}
          </Button>
          {expense && (
            <>
              <Button
                variant="secondary"
                disabled={deleteMutation.isPending}
                onClick={() => {
                  if (window.confirm('Ștergi această cheltuială?')) {
                    deleteMutation.mutate()
                  }
                }}
              >
                Șterge cheltuiala
              </Button>
              <ErrorText message={deleteMutation.isError ? errorMessage(deleteMutation.error) : null} />
            </>
          )}
        </div>
      )}
    </form>
  )
}
