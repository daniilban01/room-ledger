import { useMutation, useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router'
import { errorMessage } from '../api/errorMessage'
import { useMe } from '../auth/useMe'
import { ExpenseForm } from '../expenses/ExpenseForm'
import { centsFromApi, formatAmount } from '../format/format'
import { groupKeys, groupsApi } from '../groups/api'
import { Card } from '../ui/Card'
import { ErrorText } from '../ui/ErrorText'
import { PageHeader } from '../ui/PageHeader'
import { receiptsApi, type ReceiptDraft } from './api'
import { resizeImage } from './resizeImage'

function warningText(code: ReceiptDraft['warnings'][number]['code'], draft: ReceiptDraft): string {
  switch (code) {
    case 'TOTAL_MISMATCH':
      return `Produsele adună ${formatAmount(centsFromApi(draft.itemsTotal))}, dar pe bon totalul e ${formatAmount(
        centsFromApi(draft.total ?? 0),
      )}. Verifică dacă lipsește sau e greșit vreun produs.`
    case 'TOTAL_MISSING':
      return 'N-am găsit totalul pe bon, așa că nu l-am putut verifica. Uită-te peste produse.'
    case 'ITEMS_DROPPED':
      return 'Câteva rânduri n-au putut fi citite și au fost lăsate deoparte. Adaugă-le de mână dacă lipsesc.'
  }
}

/** 1) pick or take a photo, 2) the server reads it, 3) the user reviews it in the normal expense form. */
export function ReceiptScanPage() {
  const groupId = useParams().groupId!
  const meQuery = useMe()
  const groupQuery = useQuery({ queryKey: groupKeys.detail(groupId), queryFn: () => groupsApi.get(groupId) })

  const parseMutation = useMutation({
    mutationFn: async (file: File) => receiptsApi.parse(groupId, await resizeImage(file)),
  })
  const draft = parseMutation.data

  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 px-4 py-6">
      <PageHeader title={draft ? 'Verifică bonul' : 'Scanează bon'} backTo={`/groups/${groupId}`} />

      {!draft && (
        <Card className="flex flex-col items-center gap-4 py-10 text-center">
          {parseMutation.isPending ? (
            <>
              <span
                aria-hidden="true"
                className="size-10 animate-spin rounded-full border-4 border-border border-t-accent"
              />
              <p role="status">
                Citim bonul…
                <br />
                <span className="text-sm text-ink/60">De obicei durează câteva secunde, uneori până la un minut.</span>
              </p>
            </>
          ) : (
            <>
              <p className="text-ink/70">
                Fă o poză clară, cu tot bonul în cadru. Produsele apar apoi aici și le poți corecta înainte de salvare.
              </p>
              {/* A real <input type="file"> styled as a button: on phones, capture="environment" opens
                  the back camera directly, and the user can still choose a photo from the gallery. */}
              <label className="flex min-h-12 cursor-pointer items-center rounded-xl bg-accent px-6 font-medium text-white hover:bg-accent/90 has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-accent">
                Fă o poză bonului
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/*"
                  capture="environment"
                  className="sr-only"
                  onChange={(event) => {
                    const file = event.target.files?.[0]
                    if (file) {
                      parseMutation.mutate(file)
                    }
                    event.target.value = '' // allow picking the same file again after an error
                  }}
                />
              </label>
              <ErrorText
                message={
                  parseMutation.isError
                    ? errorMessage(parseMutation.error, {
                        400: 'Fișierul nu e o imagine acceptată. Folosește o poză JPEG, PNG sau WebP.',
                        413: 'Poza e prea mare. Încearcă alta.',
                        422: 'N-am reușit să citim bonul. Încearcă o poză mai clară sau adaugă cheltuiala manual.',
                        429: 'Ai scanat multe bonuri în ultima oră. Mai încearcă puțin mai târziu.',
                        502: 'Serviciul care citește bonurile nu răspunde acum. Încearcă din nou în câteva minute.',
                      })
                    : null
                }
              />
            </>
          )}
        </Card>
      )}

      {draft && (
        <>
          {draft.warnings.length > 0 && (
            <div role="alert" className="flex flex-col gap-2 rounded-[20px] border border-negative/30 bg-negative/5 p-4">
              {draft.warnings.map((warning) => (
                <p key={warning.code}>{warningText(warning.code, draft)}</p>
              ))}
            </div>
          )}
          {draft.total !== null && (
            <p className="px-1 text-sm text-ink/60">Total pe bon: {formatAmount(centsFromApi(draft.total))}</p>
          )}
          {meQuery.data && groupQuery.data ? (
            <ExpenseForm
              me={meQuery.data}
              group={groupQuery.data}
              draft={{
                description: draft.storeName ?? 'Cumpărături',
                expenseDate: draft.date,
                items: draft.items.map((item) => ({ name: item.name, amountCents: centsFromApi(item.amount) })),
              }}
            />
          ) : (
            <p className="text-ink/60">Se încarcă…</p>
          )}
        </>
      )}
    </main>
  )
}
