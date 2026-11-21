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

const gcd = (a: number, b: number): number => (b === 0 ? a : gcd(b, a % b))

/**
 * Same algorithm as SplitCalculator.items on the backend: exact shares for the whole receipt
 * (scaled by the least common multiple of group sizes so they stay integers), rounded ONCE,
 * leftover cents to the largest remainders, ties by sorted id.
 */
export function splitItems(items: ItemDraft[]): SplitResult {
  if (items.length === 0) {
    return { ok: false, error: 'Adaugă cel puțin un produs.' }
  }
  let scale = 1
  let total = 0
  for (const item of items) {
    if (item.amount === null || item.amount <= 0) {
      return { ok: false, error: 'Fiecare produs trebuie să aibă un preț valid.' }
    }
    if (item.assignees.length === 0) {
      return { ok: false, error: 'Alege cine a consumat fiecare produs.' }
    }
    const people = new Set(item.assignees).size
    scale = (scale / gcd(scale, people)) * people
    total += item.amount
  }

  // Each person's exact share in units of 1/scale cent.
  const exact = new Map<string, number>()
  for (const item of items) {
    const people = [...new Set(item.assignees)]
    const perPerson = item.amount! * (scale / people.length)
    people.forEach((person) => exact.set(person, (exact.get(person) ?? 0) + perPerson))
  }

  const ids = [...exact.keys()].sort()
  const shares = new Map<string, number>()
  let assigned = 0
  for (const id of ids) {
    const whole = Math.floor(exact.get(id)! / scale)
    shares.set(id, whole)
    assigned += whole
  }
  const byRemainder = [...ids].sort((a, b) => (exact.get(b)! % scale) - (exact.get(a)! % scale) || (a < b ? -1 : 1))
  for (let i = 0; i < total - assigned; i++) {
    shares.set(byRemainder[i], shares.get(byRemainder[i])! + 1)
  }
  return { ok: true, shares }
}

export function itemsTotal(items: ItemDraft[]): number {
  return items.reduce((sum, item) => sum + (item.amount ?? 0), 0)
}
