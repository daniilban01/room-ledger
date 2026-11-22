import type { ButtonHTMLAttributes } from 'react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'md' | 'sm'
}

const variants = {
  primary: 'bg-accent text-white hover:bg-accent/90',
  secondary: 'border border-border bg-surface text-ink hover:bg-page',
  danger: 'bg-negative text-white hover:bg-negative/90',
}

// Both sizes keep min-h-11 (44px): "sm" is only visually lighter, never a smaller touch target.
const sizes = {
  md: 'rounded-xl px-4 font-medium',
  sm: 'rounded-full px-4 text-sm font-medium',
}

export function Button({ variant = 'primary', size = 'md', type = 'button', className = '', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={`min-h-11 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    />
  )
}
