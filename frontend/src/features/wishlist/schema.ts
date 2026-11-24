import { z } from 'zod'

export const suggestionSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('URL'),
    url: z
      .string()
      .trim()
      .min(1, 'Wklej link do produktu.')
      .max(8192, 'Link jest za długi.')
      .refine((value) => {
        try {
          const url = new URL(value)
          return (url.protocol === 'https:' || url.protocol === 'http:') && url.hostname.length > 0 && !url.username && !url.password
        } catch {
          return false
        }
      }, 'Podaj pełny adres zaczynający się od https://'),
  }),
  z.object({
    type: z.literal('DESCRIPTION'),
    description: z.string().trim().min(1, 'Opisz swój pomysł na prezent.').max(500, 'Opis może mieć maksymalnie 500 znaków.'),
  }),
])

export type SuggestionValues = z.infer<typeof suggestionSchema>
