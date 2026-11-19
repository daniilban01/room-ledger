// role="alert" makes screen readers announce the error as soon as it appears.
export function ErrorText({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return (
    <p role="alert" className="text-sm text-negative">
      {message}
    </p>
  )
}
