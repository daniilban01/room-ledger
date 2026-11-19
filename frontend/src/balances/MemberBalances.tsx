import { centsFromApi } from '../format/format'
import { Card } from '../ui/Card'
import type { MemberBalance } from './api'
import { SignedAmount } from './SignedAmount'

type Props = { members: MemberBalance[]; meId: string | undefined }

export function MemberBalances({ members, meId }: Props) {
  return (
    <Card>
      <h2 className="mb-3 font-display text-xl font-bold">Solduri</h2>
      <ul className="flex flex-col divide-y divide-border">
        {members.map((member) => (
          <li key={member.userId} className="flex min-h-11 items-center justify-between gap-4 py-2">
            <span>
              {member.displayName}
              {member.userId === meId && <span className="text-ink/60"> (tu)</span>}
            </span>
            <SignedAmount cents={centsFromApi(member.balance)} />
          </li>
        ))}
      </ul>
    </Card>
  )
}
