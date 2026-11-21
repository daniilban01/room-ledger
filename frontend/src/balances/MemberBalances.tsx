import { centsFromApi } from '../format/format'
import { Avatar } from '../ui/Avatar'
import { Card } from '../ui/Card'
import { Section } from '../ui/Section'
import type { MemberBalance } from './api'
import { SignedAmount } from './SignedAmount'

type Props = { members: MemberBalance[]; meId: string | undefined }

export function MemberBalances({ members, meId }: Props) {
  return (
    <Section title="Colegi">
      <Card padded={false}>
        <ul className="divide-y divide-border">
          {members.map((member) => (
            <li key={member.userId} className="flex min-h-14 items-center gap-3 px-4 py-2">
              <Avatar name={member.displayName} id={member.userId} />
              <span className="min-w-0 flex-1 truncate">
                {member.displayName}
                {member.userId === meId && <span className="text-ink/50"> · tu</span>}
              </span>
              <SignedAmount cents={centsFromApi(member.balance)} />
            </li>
          ))}
        </ul>
      </Card>
    </Section>
  )
}
