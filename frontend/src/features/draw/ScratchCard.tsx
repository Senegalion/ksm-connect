import { useCallback, useEffect, useRef, type ReactNode } from 'react'

const REVEAL_THRESHOLD = 0.45
const BRUSH_RADIUS = 26

interface ScratchCardProps {
  revealed: boolean
  onReveal: () => void
  children: ReactNode
}

function paintCover(canvas: HTMLCanvasElement) {
  const context = canvas.getContext('2d')
  if (!context) {
    return
  }
  const { width, height } = canvas
  const scale = window.devicePixelRatio || 1
  context.globalCompositeOperation = 'source-over'
  context.fillStyle = '#fdd20b'
  context.fillRect(0, 0, width, height)
  context.save()
  context.globalAlpha = 0.16
  context.strokeStyle = '#1b3f94'
  context.lineWidth = 10 * scale
  for (let x = -height; x < width; x += 34 * scale) {
    context.beginPath()
    context.moveTo(x, height)
    context.lineTo(x + height, 0)
    context.stroke()
  }
  context.restore()
  context.fillStyle = '#0c1d4a'
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.font = `900 ${26 * scale}px "Archivo Variable", sans-serif`
  context.fillText('ZDRAP LOS', width / 2, height / 2 - 12 * scale)
  context.font = `600 ${14 * scale}px "IBM Plex Sans", sans-serif`
  context.fillText('przeciągnij palcem lub myszką', width / 2, height / 2 + 20 * scale)
}

function clearedRatio(canvas: HTMLCanvasElement): number {
  const context = canvas.getContext('2d')
  if (!context || canvas.width === 0 || canvas.height === 0) {
    return 0
  }
  const { data } = context.getImageData(0, 0, canvas.width, canvas.height)
  let cleared = 0
  let sampled = 0
  for (let index = 3; index < data.length; index += 4 * 24) {
    sampled += 1
    if (data[index] === 0) {
      cleared += 1
    }
  }
  return sampled === 0 ? 0 : cleared / sampled
}

export function ScratchCard({ revealed, onReveal, children }: ScratchCardProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const drawing = useRef(false)
  const lastPoint = useRef<{ x: number; y: number } | null>(null)
  const strokes = useRef(0)

  useEffect(() => {
    const container = containerRef.current
    const canvas = canvasRef.current
    if (!container || !canvas || revealed) {
      return
    }
    const resize = () => {
      const scale = window.devicePixelRatio || 1
      canvas.width = Math.round(container.clientWidth * scale)
      canvas.height = Math.round(container.clientHeight * scale)
      paintCover(canvas)
    }
    resize()
    const observer = new ResizeObserver(resize)
    observer.observe(container)
    document.fonts?.ready.then(() => {
      if (strokes.current === 0) {
        paintCover(canvas)
      }
    })
    return () => observer.disconnect()
  }, [revealed])

  const scratch = useCallback((clientX: number, clientY: number) => {
    const canvas = canvasRef.current
    const context = canvas?.getContext('2d')
    if (!canvas || !context) {
      return
    }
    const bounds = canvas.getBoundingClientRect()
    const scale = canvas.width / bounds.width
    const point = { x: (clientX - bounds.left) * scale, y: (clientY - bounds.top) * scale }
    context.globalCompositeOperation = 'destination-out'
    context.lineCap = 'round'
    context.lineJoin = 'round'
    context.lineWidth = BRUSH_RADIUS * 2 * scale
    context.beginPath()
    context.moveTo(lastPoint.current?.x ?? point.x, lastPoint.current?.y ?? point.y)
    context.lineTo(point.x, point.y)
    context.stroke()
    lastPoint.current = point
    strokes.current += 1
    if (strokes.current % 8 === 0 && clearedRatio(canvas) >= REVEAL_THRESHOLD) {
      drawing.current = false
      onReveal()
    }
  }, [onReveal])

  return (
    <div ref={containerRef} className="relative">
      {children}
      {!revealed && (
        <canvas
          ref={canvasRef}
          aria-hidden
          className="absolute inset-0 h-full w-full cursor-grab touch-none rounded-2xl active:cursor-grabbing"
          onPointerDown={(event) => {
            event.currentTarget.setPointerCapture(event.pointerId)
            drawing.current = true
            lastPoint.current = null
            scratch(event.clientX, event.clientY)
          }}
          onPointerMove={(event) => {
            if (drawing.current) {
              scratch(event.clientX, event.clientY)
            }
          }}
          onPointerUp={() => {
            drawing.current = false
            lastPoint.current = null
          }}
          onPointerCancel={() => {
            drawing.current = false
            lastPoint.current = null
          }}
        />
      )}
    </div>
  )
}
