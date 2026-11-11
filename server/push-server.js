import { resolve } from 'node:path'
import { pathToFileURL } from 'node:url'
import express from 'express'
import webPush from 'web-push'
import { authenticate } from './push-server/auth.js'
import { deliverDueAlarms } from './push-server/deliverDueAlarms.js'
import { registerPushRoutes } from './push-server/routes.js'
const port = Number(process.env.PUSH_PORT || 3001)
const allowedOrigins = new Set((process.env.CORS_ALLOWED_ORIGINS || 'http://localhost:5173,http://127.0.0.1:5173').split(',').map((origin) => origin.trim()).filter(Boolean))

if (!process.env.VAPID_PUBLIC_KEY || !process.env.VAPID_PRIVATE_KEY || !process.env.VAPID_SUBJECT) {
  throw new Error('VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY and VAPID_SUBJECT are required.')
}
if (!allowedOrigins.size) throw new Error('CORS_ALLOWED_ORIGINS must include the frontend origin.')
webPush.setVapidDetails(process.env.VAPID_SUBJECT, process.env.VAPID_PUBLIC_KEY, process.env.VAPID_PRIVATE_KEY)

const app = express()
app.disable('x-powered-by')
app.use((req, res, next) => {
  const origin = req.get('origin')
  if (origin && !allowedOrigins.has(origin)) return res.status(403).json({ message: 'Origem não permitida.' })
  if (origin) {
    res.set('Access-Control-Allow-Origin', origin)
    res.set('Vary', 'Origin')
    res.set('Access-Control-Allow-Headers', 'Authorization, Content-Type, X-Profile-Id')
    res.set('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS')
  }
  if (req.method === 'OPTIONS') return res.sendStatus(204)
  return next()
})
app.use(express.json({ limit: '16kb' }))

registerPushRoutes(app, authenticate)

app.use((error, req, res, _next) => {
  console.error('Push service request failed:', error.message)
  return res.status(400).json({ message: error.message || 'Não foi possível processar a solicitação.' })
})

export function startPushServer(listenPort = port) {
  const server = app.listen(listenPort, () => console.log(`Push alarm service listening on port ${listenPort}`))
  const scheduler = setInterval(() => {
    deliverDueAlarms().catch((error) => console.error('Push scheduler failed:', error.message))
  }, 15_000)
  server.on('close', () => clearInterval(scheduler))
  return server
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) startPushServer()
