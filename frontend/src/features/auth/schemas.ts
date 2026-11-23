import { z } from 'zod'

const email = z.string().trim().min(1, 'Podaj adres e-mail.').email('Podaj prawidłowy adres e-mail.').max(254)
const newPassword = z
  .string()
  .min(10, 'Hasło musi mieć co najmniej 10 znaków.')
  .max(128, 'Hasło może mieć maksymalnie 128 znaków.')

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Podaj hasło.'),
})

export const registerSchema = z.object({
  firstName: z.string().trim().min(1, 'Podaj imię.').max(100),
  lastName: z.string().trim().min(1, 'Podaj nazwisko.').max(100),
  email,
  phoneNumber: z
    .string()
    .trim()
    .min(1, 'Podaj numer telefonu.')
    .max(30)
    .regex(/^[+0-9 ()-]{7,30}$/, 'Podaj prawidłowy numer telefonu.'),
  password: newPassword,
  privacyNoticeAccepted: z.boolean().refine((value) => value, 'Zapoznaj się z informacją o przetwarzaniu danych.'),
})

export const emailOnlySchema = z.object({ email })

export const resetPasswordSchema = z
  .object({ password: newPassword, confirmPassword: z.string() })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Hasła nie są takie same.',
  })

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>
export type EmailOnlyValues = z.infer<typeof emailOnlySchema>
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>
