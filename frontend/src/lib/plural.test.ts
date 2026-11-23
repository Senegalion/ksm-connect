import { describe, expect, it } from 'vitest'
import { pluralParticipants } from './plural'

describe('pluralParticipants', () => {
  it.each([
    [1, 'uczestnik'],
    [2, 'uczestnicy'],
    [4, 'uczestnicy'],
    [5, 'uczestników'],
    [12, 'uczestników'],
    [22, 'uczestnicy'],
    [30, 'uczestników'],
  ])('%i -> %s', (count, expected) => {
    expect(pluralParticipants(count)).toBe(expected)
  })
})
