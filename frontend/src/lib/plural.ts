export function pluralParticipants(count: number): string {
  if (count === 1) {
    return 'uczestnik'
  }
  const lastDigit = count % 10
  const lastTwo = count % 100
  const isFew = lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)
  return isFew ? 'uczestnicy' : 'uczestników'
}
