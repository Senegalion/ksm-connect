import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Link, type LinkProps } from 'react-router'
import { buttonClasses, type Size, type Variant } from './buttonStyles'
import { cn } from './cn'
import { Spinner } from './Spinner'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  fullWidth?: boolean
  loading?: boolean
  icon?: ReactNode
}

export function Button({ variant, size, fullWidth, loading, icon, children, className, disabled, type, ...rest }: ButtonProps) {
  return (
    <button
      type={type ?? 'button'}
      className={cn(buttonClasses(variant, size, fullWidth), className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? <Spinner className="size-4" /> : icon}
      {children}
    </button>
  )
}

interface ButtonLinkProps extends LinkProps {
  variant?: Variant
  size?: Size
  fullWidth?: boolean
  icon?: ReactNode
}

export function ButtonLink({ variant, size, fullWidth, icon, children, className, ...rest }: ButtonLinkProps) {
  return (
    <Link className={cn(buttonClasses(variant, size, fullWidth), className as string)} {...rest}>
      {icon}
      {children}
    </Link>
  )
}
