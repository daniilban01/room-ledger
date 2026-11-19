import { useId, type InputHTMLAttributes } from 'react'

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
}

/** Input with a real <label> linked by id, so clicking the label focuses the field and screen readers announce it. */
export function TextField({ label, id, className = '', ...inputProps }: TextFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm font-medium">
        {label}
      </label>
      <input
        id={inputId}
        className={`min-h-11 rounded-xl border border-border bg-surface px-3 text-base outline-none focus-visible:border-accent focus-visible:ring-2 focus-visible:ring-accent/30 ${className}`}
        {...inputProps}
      />
    </div>
  )
}
