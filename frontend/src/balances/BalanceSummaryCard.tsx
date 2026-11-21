import { Link } from 'react-router'
import { centsFromApi } from '../format/format'
import { SignedAmount } from './SignedAmount'

type Props = { groupId: string; myBalance: number | undefined }

export function BalanceSummaryCard({ groupId, myBalance }: Props) {
  const cents = myBalance === undefined ? null : centsFromApi(myBalance)

  let explanation = 'Se calculează…'
  if (cents !== null) {
    if (cents > 0) explanation = 'Ți se datorează'
    else if (cents < 0) explanation = 'Datorezi'
    else explanation = 'Ești la zi'
  }

  return (
    <section aria-labelledby="my-balance-title" className="flex flex-col gap-4 rounded-[22px] bg-ink p-5 text-white">
      <div className="flex flex-col gap-1">
        <h2 id="my-balance-title" className="text-sm text-white/70">
          Soldul tău
        </h2>
        {cents === null ? (
          <p className="font-display text-4xl font-bold">…</p>
        ) : (
          <SignedAmount cents={cents} onDark className="text-4xl" />
        )}
        <p className="text-sm text-white/70">{explanation}</p>
      </div>
      <Link
        to={`/groups/${groupId}/payments`}
        className="flex min-h-11 items-center justify-center rounded-xl bg-white px-4 font-medium text-ink hover:bg-white/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
      >
        Decontează
      </Link>
    </section>
  )
}
