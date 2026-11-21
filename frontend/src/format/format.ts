// The frontend works with money as integer cents, like the backend's SplitCalculator,
// so adding and splitting never suffers from floating-point errors (0.1 + 0.2 !== 0.3).

// ro-RO normally skips the thousands separator for 4-digit numbers ("1234,50");
// useGrouping 'always' gives the "1.234,50" the design asks for.
const amountFormatter = new Intl.NumberFormat('ro-RO', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: 'always',
})

/** 123450 -> "1.234,50 lei" */
export function formatAmount(cents: number): string {
  return `${amountFormatter.format(cents / 100)} lei`
}

/**
 * Balance with an explicit sign, so its meaning does not depend on color alone:
 * 1250 -> "+12,50 lei", -1250 -> "−12,50 lei" (U+2212, the real minus sign), 0 -> "0,00 lei".
 */
export function formatSignedAmount(cents: number): string {
  if (cents > 0) {
    return `+${formatAmount(cents)}`
  }
  if (cents < 0) {
    return `−${formatAmount(-cents)}`
  }
  return formatAmount(0)
}

/** 1250 -> "12,50", the format used to pre-fill inputs. */
export function centsToInput(cents: number): string {
  return (cents / 100).toFixed(2).replace('.', ',')
}

/**
 * Parses what a user types into cents. Accepts "12", "12,5", "12.50" and "1.234,50".
 * If there is a comma it is the decimal separator (dots are thousands separators),
 * otherwise a dot is the decimal separator. Returns null for anything else.
 */
export function parseAmount(input: string): number | null {
  const compact = input.replace(/\s/g, '')
  const normalized = compact.includes(',') ? compact.replace(/\./g, '').replace(',', '.') : compact
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(normalized)) {
    return null
  }
  const [whole, fraction = ''] = normalized.split('.')
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
}

/** The API sends amounts as JSON numbers (e.g. 33.34); round to the nearest cent to remove float noise. */
export function centsFromApi(amount: number): number {
  return Math.round(amount * 100)
}

/**
 * Integer cents / 100 always serializes to the exact decimal ("33.34"), because JSON.stringify
 * prints the shortest text that maps back to the same double. The backend reads it into a BigDecimal.
 */
export function centsToApi(cents: number): number {
  return cents / 100
}

const dateFormatter = new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' })

/** "2026-11-05" -> "5 nov. 2026". Built from parts so the date is not shifted by the time zone. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return dateFormatter.format(new Date(year, month - 1, day))
}

const dateTimeFormatter = new Intl.DateTimeFormat('ro-RO', {
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})

/** An instant from the API ("2026-11-05T18:30:00Z") in the user's time zone: "5 nov., 20:30". */
export function formatDateTime(instant: string): string {
  return dateTimeFormatter.format(new Date(instant))
}

/** A date in the user's time zone as "YYYY-MM-DD" (toISOString would use UTC and can shift the day). */
export function localDateKey(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function todayIso(): string {
  return localDateKey(new Date())
}

const dayFormatter = new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'long' })
const dayWithYearFormatter = new Intl.DateTimeFormat('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' })

/** "Azi", "Ieri", "12 noiembrie", or "12 noiembrie 2025" for another year. */
export function formatDayLabel(dayKey: string, now: Date = new Date()): string {
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (dayKey === localDateKey(now)) return 'Azi'
  if (dayKey === localDateKey(yesterday)) return 'Ieri'

  const [year, month, day] = dayKey.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return (year === now.getFullYear() ? dayFormatter : dayWithYearFormatter).format(date)
}

const timeFormatter = new Intl.DateTimeFormat('ro-RO', { hour: '2-digit', minute: '2-digit' })

/** "14:05" in the user's time zone. */
export function formatTime(instant: string): string {
  return timeFormatter.format(new Date(instant))
}
