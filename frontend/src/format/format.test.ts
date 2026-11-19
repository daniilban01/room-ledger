import { describe, expect, it } from 'vitest'
import { centsFromApi, centsToApi, centsToInput, formatAmount, formatDate, parseAmount } from './format'

// Intl may use a non-breaking space; normalize so the assertions stay readable.
const plain = (text: string) => text.replace(/\s/g, ' ')

describe('formatAmount', () => {
  it('uses Romanian separators and the "lei" suffix', () => {
    expect(plain(formatAmount(123450))).toBe('1.234,50 lei')
  })

  it('groups thousands even for 4-digit amounts', () => {
    expect(plain(formatAmount(100000))).toBe('1.000,00 lei')
  })

  it('always shows two decimals', () => {
    expect(plain(formatAmount(5))).toBe('0,05 lei')
    expect(plain(formatAmount(1200))).toBe('12,00 lei')
  })
})

describe('parseAmount', () => {
  it.each([
    ['12', 1200],
    ['12,5', 1250],
    ['12,50', 1250],
    ['12.50', 1250],
    ['1.234,50', 123450],
    [' 7 ', 700],
    ['0,01', 1],
  ])('parses "%s" as %i cents', (input, cents) => {
    expect(parseAmount(input)).toBe(cents)
  })

  it.each(['', 'abc', '12,345', '-5', '1,2,3', '12.'])('rejects "%s"', (input) => {
    expect(parseAmount(input)).toBeNull()
  })
})

describe('conversions', () => {
  it('round-trips through the API without float errors', () => {
    expect(centsFromApi(33.34)).toBe(3334)
    expect(centsFromApi(0.1 + 0.2)).toBe(30)
    expect(JSON.stringify(centsToApi(3334))).toBe('33.34')
  })

  it('formats cents for inputs', () => {
    expect(centsToInput(1250)).toBe('12,50')
  })
})

describe('formatDate', () => {
  it('formats ISO dates in Romanian without shifting the day', () => {
    expect(plain(formatDate('2026-11-05'))).toBe('5 nov. 2026')
  })
})
