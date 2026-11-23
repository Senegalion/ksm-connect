import { JoinEventFlow } from '../features/registration/JoinEventFlow'

export function JoinPage() {
  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div className="space-y-1">
        <h1 className="display-wide text-4xl leading-[1]">Dołącz do wydarzenia</h1>
        <p className="text-ink-500">Wpisz kod zaproszenia od organizatora.</p>
      </div>
      <JoinEventFlow />
    </div>
  )
}
