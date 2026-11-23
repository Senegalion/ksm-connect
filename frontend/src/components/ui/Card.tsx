import type { HTMLAttributes } from 'react'
import { cn } from './cn'

export function Card({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('rounded-[var(--radius-card)] border border-cream-200 bg-white shadow-[var(--shadow-card)]', className)}
      {...rest}
    />
  )
}

export function CardBody({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('p-5 sm:p-6', className)} {...rest} />
}
