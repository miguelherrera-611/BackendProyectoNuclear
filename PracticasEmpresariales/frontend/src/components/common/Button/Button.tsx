import { ButtonHTMLAttributes } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md'
  loading?: boolean
}

const VARIANTS: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary:   'bg-cue-primary text-white hover:bg-cue-secondary disabled:opacity-50',
  secondary: 'bg-white text-cue-primary border border-cue-primary hover:bg-cue-light disabled:opacity-50',
  danger:    'bg-red-600 text-white hover:bg-red-700 disabled:opacity-50',
  ghost:     'text-gray-600 hover:bg-gray-100 disabled:opacity-50',
}

const SIZES: Record<NonNullable<ButtonProps['size']>, string> = {
  sm: 'px-3 py-1.5 text-xs rounded-lg',
  md: 'px-4 py-2 text-sm rounded-lg',
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  children,
  className = '',
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 font-medium transition-colors ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
    >
      {loading && (
        <span className="animate-spin rounded-full h-3.5 w-3.5 border-b-2 border-current" />
      )}
      {children}
    </button>
  )
}
