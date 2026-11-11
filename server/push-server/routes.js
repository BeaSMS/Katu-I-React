import { randomUUID } from 'node:crypto'
import { createSchedule } from '../alarm-schedule.js'
import { mutateStore, readStore } from './store.js'

const pushEndpointHosts = ['fcm.googleapis.com', 'updates.push.services.mozilla.com', 'web.push.apple.com', 'notify.windows.com']

function isAllowedPushEndpoint(value) {
  let url
  try { url = new URL(value) } catch { return false }
  if (url.protocol !== 'https:') return false
  return pushEndpointHosts.some((host) => url.hostname === host || url.hostname.endsWith(`.${host}`))
}

function validateSubscription(subscription) {
  return Boolean(subscription && isAllowedPushEndpoint(subscription.endpoint) && typeof subscription.keys?.p256dh === 'string' && typeof subscription.keys?.auth === 'string')
}

export function registerPushRoutes(app, authenticate) {
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
}
