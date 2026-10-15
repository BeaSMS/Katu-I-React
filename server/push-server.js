import { randomUUID } from 'node:crypto'
import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import express from 'express'
import webPush from 'web-push'
import { advanceSchedule, createSchedule } from './alarm-schedule.js'

const directory = dirname(fileURLToPath(import.meta.url))
const storePath = resolve(process.env.PUSH_STORE_PATH || `${directory}/data/push-store.json`)
const port = Number(process.env.PUSH_PORT || 3001)
const authApiUrl = process.env.AUTH_API_URL
const allowedOrigins = new Set((process.env.CORS_ALLOWED_ORIGINS || 'http://localhost:5173,http://127.0.0.1:5173').split(',').map((origin) => origin.trim()).filter(Boolean))
const pushEndpointHosts = ['fcm.googleapis.com', 'updates.push.services.mozilla.com', 'web.push.apple.com', 'notify.windows.com']

if (!authApiUrl) throw new Error('AUTH_API_URL must point to the authenticated API /me endpoint base.')
if (!process.env.VAPID_PUBLIC_KEY || !process.env.VAPID_PRIVATE_KEY || !process.env.VAPID_SUBJECT) {
  throw new Error('VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY and VAPID_SUBJECT are required.')
}
if (!allowedOrigins.size) throw new Error('CORS_ALLOWED_ORIGINS must include the frontend origin.')
webPush.setVapidDetails(process.env.VAPID_SUBJECT, process.env.VAPID_PUBLIC_KEY, process.env.VAPID_PRIVATE_KEY)

const emptyStore = () => ({ subscriptions: [], alarms: [] })
let storeMutation = Promise.resolve()
const activeDispatches = new Set()

async function readStore() {
  try {
    const data = JSON.parse(await readFile(storePath, 'utf8'))
    if (!Array.isArray(data.subscriptions) || !Array.isArray(data.alarms)) throw new Error('Invalid push store shape.')
    return data
  } catch (error) {
    if (error.code === 'ENOENT') return emptyStore()
    throw error
  }
}

async function mutateStore(mutator) {
  const operation = storeMutation.then(async () => {
    const store = await readStore()
    const result = await mutator(store)
    await mkdir(dirname(storePath), { recursive: true })
    const temporaryPath = `${storePath}.${process.pid}.tmp`
    await writeFile(temporaryPath, JSON.stringify(store), { mode: 0o600 })
    await rename(temporaryPath, storePath)
    return result
  })
  storeMutation = operation.catch(() => {})
  return operation
}

function isAllowedPushEndpoint(value) {
  let url
  try { url = new URL(value) } catch { return false }
  if (url.protocol !== 'https:') return false
  return pushEndpointHosts.some((host) => url.hostname === host || url.hostname.endsWith(`.${host}`))
}

function validateSubscription(subscription) {
  return Boolean(
    subscription &&
    isAllowedPushEndpoint(subscription.endpoint) &&
    typeof subscription.keys?.p256dh === 'string' &&
    typeof subscription.keys?.auth === 'string',
  )
}

async function authenticate(req, res, next) {
  const authorization = req.get('authorization') || ''
  if (!authorization.startsWith('Bearer ')) return res.status(401).json({ message: 'Entre na sua conta para gerenciar alarmes em segundo plano.' })
  const profileId = req.get('x-profile-id')
  if (!profileId) return res.status(400).json({ message: 'Selecione um perfil de cuidado para gerenciar os alarmes.' })

  try {
    const response = await fetch(`${authApiUrl.replace(/\/$/, '')}/me`, {
      headers: { Authorization: authorization, 'X-Profile-Id': profileId },
      signal: AbortSignal.timeout(5000),
    })
    if (!response.ok) return res.status(response.status === 401 ? 401 : 502).json({ message: 'Não foi possível validar sua sessão para notificações.' })
    const account = await response.json()
    const profiles = Array.isArray(account.profiles) ? account.profiles : []
    if (!profiles.some((profile) => String(profile.id) === String(profileId))) {
      return res.status(403).json({ message: 'Este perfil não pertence à conta autenticada.' })
    }
    const accountId = account.user?.id || account.userId || account.id
    if (!accountId) return res.status(502).json({ message: 'A API de autenticação precisa retornar um ID estável da conta em /me.' })
    req.pushIdentity = {
      ownerId: String(accountId),
      profileId: String(profileId),
    }
    return next()
  } catch (error) {
    return res.status(502).json({ message: `Falha ao validar a sessão: ${error.message}` })
  }
}

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

app.get('/vapid-public-key', (req, res) => res.json({ publicKey: process.env.VAPID_PUBLIC_KEY }))
app.use(authenticate)

app.post('/subscriptions', async (req, res, next) => {
  try {
    if (!validateSubscription(req.body)) return res.status(400).json({ message: 'A assinatura de notificações do navegador é inválida ou não suportada.' })
    const { ownerId, profileId } = req.pushIdentity
    const subscription = req.body
    const result = await mutateStore((store) => {
      const existing = store.subscriptions.find((item) => item.endpoint === subscription.endpoint && item.ownerId === ownerId)
      if (existing) {
        existing.profileId = profileId
        existing.subscription = subscription
        return existing
      }
      const item = { id: randomUUID(), ownerId, profileId, endpoint: subscription.endpoint, subscription, createdAt: new Date().toISOString() }
      store.subscriptions.push(item)
      return item
    })
    return res.status(200).json({ id: result.id, profileId: result.profileId })
  } catch (error) { return next(error) }
})

app.delete('/subscriptions', async (req, res, next) => {
  try {
    const endpoint = req.body?.endpoint
    if (!isAllowedPushEndpoint(endpoint)) return res.status(400).json({ message: 'O destino da assinatura é inválido.' })
    const { ownerId, profileId } = req.pushIdentity
    await mutateStore((store) => {
      store.subscriptions = store.subscriptions.filter((item) => !(item.ownerId === ownerId && item.profileId === profileId && item.endpoint === endpoint))
    })
    return res.sendStatus(204)
  } catch (error) { return next(error) }
})

app.post('/medication-schedules', async (req, res, next) => {
  try {
    const { ownerId, profileId } = req.pushIdentity
    const subscriptions = await readStore()
    if (!subscriptions.subscriptions.some((item) => item.ownerId === ownerId && item.profileId === profileId)) {
      return res.status(409).json({ message: 'Ative as notificações neste perfil antes de configurar alarmes.' })
    }
    const schedule = createSchedule({
      ...req.body,
      ownerId,
      profileId,
      sourceId: req.body.medicationId,
      displayName: req.body.medicationName,
      title: 'Hora do seu medicamento',
      oneTime: false,
    })
    const result = await mutateStore((store) => {
      store.alarms = store.alarms.filter((item) => !(item.ownerId === ownerId && item.profileId === profileId && item.sourceId === schedule.sourceId && !item.oneTime))
      store.alarms.push(schedule)
      return schedule
    })
    return res.status(201).json({ id: result.id, nextAt: result.nextAt, endAt: result.endAt, intervalMinutes: result.intervalMinutes })
  } catch (error) { return next(error) }
})

app.delete('/medication-schedules/:medicationId', async (req, res, next) => {
  try {
    const { ownerId, profileId } = req.pushIdentity
    await mutateStore((store) => {
      store.alarms = store.alarms.filter((item) => !(item.ownerId === ownerId && item.profileId === profileId && item.sourceId === req.params.medicationId && !item.oneTime))
    })
    return res.sendStatus(204)
  } catch (error) { return next(error) }
})

app.get('/alarms', async (req, res, next) => {
  try {
    const { ownerId, profileId } = req.pushIdentity
    const store = await readStore()
    const alarms = store.alarms
      .filter((item) => item.ownerId === ownerId && item.profileId === profileId)
      .map((item) => ({
        id: item.sourceId || item.id,
        sourceId: item.sourceId,
        name: item.displayName || item.title,
        title: item.title,
        detail: item.oneTime ? 'Lembrete pontual' : item.cycleActiveDays && item.cyclePauseDays
          ? `Cartela: ${item.cycleActiveDays} dias de uso + ${item.cyclePauseDays} dias de pausa`
          : `A cada ${item.intervalMinutes % 60 === 0 ? `${item.intervalMinutes / 60} hora(s)` : `${item.intervalMinutes} minuto(s)`}`,
        date: item.localDate,
        time: item.localTime,
        startAt: item.startAt,
        nextAt: item.nextAt,
        endAt: item.endAt,
        intervalMinutes: item.intervalMinutes,
        cycleActiveDays: item.cycleActiveDays || null,
        cyclePauseDays: item.cyclePauseDays || null,
        durationDays: item.endAt && !item.oneTime ? (new Date(item.endAt).getTime() - new Date(item.startAt).getTime()) / 86_400_000 : null,
        oneTime: item.oneTime,
        status: item.enabled ? 'Ativo' : 'Concluído',
        __pushAlarm: true,
      }))
    return res.json(alarms)
  } catch (error) { return next(error) }
})

app.post('/alarms', async (req, res, next) => {
  try {
    const { ownerId, profileId } = req.pushIdentity
    const store = await readStore()
    if (!store.subscriptions.some((item) => item.ownerId === ownerId && item.profileId === profileId)) {
      return res.status(409).json({ message: 'Ative as notificações neste perfil antes de configurar alarmes.' })
    }
    const schedule = createSchedule({
      ...req.body,
      ownerId,
      profileId,
      title: req.body.title || 'Seu lembrete de saúde',
      oneTime: req.body.oneTime !== false,
    })
    const result = await mutateStore((nextStore) => {
      nextStore.alarms = nextStore.alarms.filter((item) => !(item.ownerId === ownerId && item.profileId === profileId && item.sourceId === schedule.sourceId && item.oneTime))
      nextStore.alarms.push(schedule)
      return schedule
    })
    return res.status(201).json({ id: result.id, nextAt: result.nextAt })
  } catch (error) { return next(error) }
})

app.delete('/alarms/:sourceId', async (req, res, next) => {
  try {
    const { ownerId, profileId } = req.pushIdentity
    await mutateStore((store) => {
      store.alarms = store.alarms.filter((item) => !(item.ownerId === ownerId && item.profileId === profileId && item.sourceId === req.params.sourceId))
    })
    return res.sendStatus(204)
  } catch (error) { return next(error) }
})

async function deliverDueAlarms() {
  const now = Date.now()
  const store = await readStore()
  const due = store.alarms.filter((alarm) => alarm.enabled && alarm.nextAt && new Date(alarm.nextAt).getTime() <= now)
  for (const alarm of due) {
    if (activeDispatches.has(alarm.id)) continue
    activeDispatches.add(alarm.id)
    try {
      const subscriptions = store.subscriptions.filter((item) => item.ownerId === alarm.ownerId && item.profileId === alarm.profileId)
      if (!subscriptions.length) {
        await mutateStore((current) => {
          const currentAlarm = current.alarms.find((item) => item.id === alarm.id)
          if (currentAlarm?.enabled) advanceSchedule(currentAlarm, now)
        })
        continue
      }
      const payload = JSON.stringify({ title: 'Katu’I · lembrete de saúde', body: alarm.title, url: '/?section=Alarmes', tag: alarm.id })
      const results = await Promise.allSettled(subscriptions.map((item) => webPush.sendNotification(item.subscription, payload)))
      const expiredEndpoints = new Set()
      let transientFailure = false
      results.forEach((result, index) => {
        if (result.status === 'rejected' && [404, 410].includes(result.reason?.statusCode)) {
          expiredEndpoints.add(subscriptions[index].endpoint)
        } else if (result.status === 'rejected') {
          transientFailure = true
          console.error(`Push delivery failed for schedule ${alarm.id}:`, result.reason?.message || result.reason)
        }
      })
      if (transientFailure && results.every((result) => result.status === 'rejected')) continue
      await mutateStore((current) => {
        current.subscriptions = current.subscriptions.filter((item) => !expiredEndpoints.has(item.endpoint))
        const currentAlarm = current.alarms.find((item) => item.id === alarm.id)
        if (currentAlarm?.enabled) {
          if (!current.subscriptions.some((item) => item.ownerId === alarm.ownerId && item.profileId === alarm.profileId)) {
            currentAlarm.enabled = false
            currentAlarm.nextAt = null
          } else {
            advanceSchedule(currentAlarm, now)
          }
        }
      })
    } finally {
      activeDispatches.delete(alarm.id)
    }
  }
}

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
