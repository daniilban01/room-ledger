import { describe, expect, it } from 'vitest'
import { splitEqual, splitExact, splitItems, type SplitResult } from './split'

const A = '10000000-0000-0000-0000-000000000000'
const B = '20000000-0000-0000-0000-000000000000'
const C = '30000000-0000-0000-0000-000000000000'

function sharesOf(result: SplitResult): Record<string, number> {
  if (!result.ok) {
    throw new Error(`Expected a valid split, got: ${result.error}`)
  }
  return Object.fromEntries(result.shares)
}

describe('splitEqual', () => {
  it('gives the leftover cent to the first id, whatever the input order', () => {
    // Same expectation as SplitCalculatorTest on the backend: 100.00 / 3.
    expect(sharesOf(splitEqual(10000, [C, A, B]))).toEqual({ [A]: 3334, [B]: 3333, [C]: 3333 })
  })

  it('always sums exactly to the total', () => {
    for (let total = 1; total <= 1000; total += 7) {
      const shares = Object.values(sharesOf(splitEqual(total, [A, B, C])))
      expect(shares.reduce((a, b) => a + b, 0)).toBe(total)
    }
  })

  it('explains what is missing', () => {
    expect(splitEqual(null, [A])).toMatchObject({ ok: false })
    expect(splitEqual(1000, [])).toEqual({ ok: false, error: 'Alege cel puțin o persoană.' })
  })
})

describe('splitExact', () => {
  it('accepts amounts that add up and drops zero shares', () => {
    const amounts = new Map([[A, 7000], [B, 3000], [C, 0]])
    expect(sharesOf(splitExact(10000, amounts))).toEqual({ [A]: 7000, [B]: 3000 })
  })

  it('reports how far off the amounts are', () => {
    const result = splitExact(10000, new Map([[A, 5000], [B, 4000]]))
    expect(result.ok).toBe(false)
    expect(!result.ok && result.error.replace(/\s/g, ' ')).toBe('Sumele dau 90,00 lei din 100,00 lei.')
  })

  it('rejects invalid input', () => {
    expect(splitExact(10000, new Map([[A, null]]))).toMatchObject({ ok: false })
  })
})

describe('splitItems', () => {
  it('sums each person’s part of the items they had', () => {
    const result = splitItems([
      { amount: 1000, assignees: [A, B] },
      { amount: 450, assignees: [A] },
      { amount: 100, assignees: [A, B, C] },
    ])
    expect(sharesOf(result)).toEqual({ [A]: 984, [B]: 533, [C]: 33 })
  })

  it('requires a price and at least one person per item', () => {
    expect(splitItems([{ amount: null, assignees: [A] }])).toMatchObject({ ok: false })
    expect(splitItems([{ amount: 500, assignees: [] }])).toMatchObject({ ok: false })
    expect(splitItems([])).toMatchObject({ ok: false })
  })
})
