import type { CSSProperties } from 'react'

const COLORS = ['#fdd20b', '#d6232b', '#1b3f94', '#ffffff', '#fdd20b', '#2453b8']
const SHAPES = ['rounded-full', 'rounded-none', 'rounded-sm rotate-45']

function pseudoRandom(seed: number): number {
  const value = Math.sin(seed * 9301 + 49297) * 233280
  return value - Math.floor(value)
}

export function Confetti({ pieces = 36, spread = 260 }: { pieces?: number; spread?: number }) {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-visible" aria-hidden>
      {Array.from({ length: pieces }, (_, index) => {
        const angle = (index / pieces) * Math.PI * 2 + pseudoRandom(index) * 0.6
        const distance = spread * (0.45 + pseudoRandom(index + 100) * 0.55)
        const size = 6 + Math.round(pseudoRandom(index + 200) * 8)
        const style = {
          width: size,
          height: index % 3 === 1 ? size * 0.45 : size,
          background: COLORS[index % COLORS.length],
          left: '50%',
          top: '50%',
          '--dx': `${Math.cos(angle) * distance}px`,
          '--dy': `${Math.sin(angle) * distance}px`,
          '--rot': `${Math.round(pseudoRandom(index + 300) * 720 - 360)}deg`,
          animation: `confetti-burst ${900 + Math.round(pseudoRandom(index + 400) * 700)}ms cubic-bezier(0.15, 0.7, 0.3, 1) both`,
          boxShadow: '0 0 0 1px rgb(15 27 51 / 0.08)',
        } as CSSProperties
        return <span key={index} className={`absolute ${SHAPES[index % SHAPES.length]}`} style={style} />
      })}
    </div>
  )
}
