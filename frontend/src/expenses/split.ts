import { formatAmount } from '../format/format'

/**
 * Live preview of how an expense is split. Mirrors the backend's SplitCalculator exactly
 * (integer cents, leftover cents to the first ids in string order), so the preview always
 * equals what the server will store.
 */
export type SplitResult = { ok: true; shares: Map<string, number> } | { ok: false; error: string }

export type ItemDraft = { amount: number | null; assignees: string[] }

/** Splits an amount equally; leftover cents go to the first people in sorted-id order. */
function distribute(amount: number, people: string[], into: Map<string, number>): void {
  // Default sort compares UTF-16 code units, the same as Java's String.compareTo.
  const ordered = [...new Set(people)].sort()
  const base = Math.floor(amount / ordered.length)
  const leftover = amount % ordered.length
  ordered.forEach((person, index) => {
    const part = base + (index < leftover ? 1 : 0)
    into.set(person, (into.get(person) ?? 0) + part)
  })
}

export function splitEqual(totalCents: number | null, participants: string[]): SplitResult {
  if (totalCents === null || totalCents <= 0) {
    return { ok: false, error: 'Introdu o sumă mai mare decât 0.' }
  }
  if (participants.length === 0) {
    return { ok: false, error: 'Alege cel puțin o persoană.' }
  }
  const shares = new Map<string, number>()
  distribute(totalCents, participants, shares)
  return { ok: true, shares }
}

/** `amounts` holds each person's cents, or null when what they typed is not a valid amount. */
export function splitExact(totalCents: number | null, amounts: Map<string, number | null>): SplitResult {
  if (totalCents === null || totalCents <= 0) {
    return { ok: false, error: 'Introdu o sumă mai mare decât 0.' }
  }
  const shares = new Map<string, number>()
  let sum = 0
  for (const [person, amount] of amounts) {
    if (amount === null) {
      return { ok: false, error: 'Una dintre sume nu este validă.' }
    }
    if (amount > 0) {
      shares.set(person, amount)
      sum += amount
    }
  }
  if (sum !== totalCents) {
    return { ok: false, error: `Sumele dau ${formatAmount(sum)} din ${formatAmount(totalCents)}.` }
  }
  return { ok: true, shares }
}

export function splitItems(items: ItemDraft[]): SplitResult {
  if (items.length === 0) {
    return { ok: false, error: 'Adaugă cel puțin un produs.' }
  }
  const shares = new Map<string, number>()
  for (const item of items) {
    if (item.amount === null || item.amount <= 0) {
      return { ok: false, error: 'Fiecare produs trebuie să aibă un preț valid.' }
    }
    if (item.assignees.length === 0) {
      return { ok: false, error: 'Alege cine a consumat fiecare produs.' }
    }
    distribute(item.amount, item.assignees, shares)
  }
  return { ok: true, shares }
}

export function itemsTotal(items: ItemDraft[]): number {
  return items.reduce((sum, item) => sum + (item.amount ?? 0), 0)
}
