import { buildMedicationSchedule } from './alarmSchedule'

const PUSH_API_URL = import.meta.env.VITE_PUSH_API_URL || ''

export function isDesktopShell() {
  if (typeof window === 'undefined') return false
  return window.location.protocol === 'file:' || new URLSearchParams(window.location.search).get('desktop') === '1'
}

export function isPushConfigured() {
  return Boolean(PUSH_API_URL) && !isDesktopShell()
}

async function pushRequest(path, { token, profileId, method = 'GET', body } = {}) {
  if (isDesktopShell()) throw new Error('Notificações push em segundo plano estão disponíveis somente na versão web.')
  if (!PUSH_API_URL) throw new Error('O serviço de notificações em segundo plano não está configurado.')
  if (!token) throw new Error('Entre em uma conta para ativar notificações em segundo plano.')
  const response = await fetch(`${PUSH_API_URL.replace(/\/$/, '')}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(profileId ? { 'X-Profile-Id': profileId } : {}),
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  if (!response.ok) {
    let result
    try { result = await response.json() } catch { result = null }
    throw new Error(result?.message || `O serviço de notificações respondeu com erro (${response.status}).`)
  }
  if (response.status === 204) return null
  return response.json()
}

function decodeApplicationServerKey(key) {
  const padding = '='.repeat((4 - key.length % 4) % 4)
  const base64 = (key + padding).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0))
}

export async function enableBackgroundPush({ token, profileId }) {
  if (!isPushConfigured()) throw new Error('O serviço de notificações em segundo plano não está configurado neste site.')
  if (!('Notification' in window) || !('serviceWorker' in navigator) || !('PushManager' in window)) {
    throw new Error('Este navegador não oferece notificações push em segundo plano.')
  }
  if (!window.isSecureContext) throw new Error('Notificações em segundo plano exigem HTTPS (ou localhost para testes).')

  const permission = Notification.permission === 'default' ? await Notification.requestPermission() : Notification.permission
  if (permission !== 'granted') throw new Error('Permita notificações nas configurações do navegador para ativar os alarmes.')

  await navigator.serviceWorker.register('/service-worker.js', { scope: '/' })
  const readyRegistration = await navigator.serviceWorker.ready
  const publicKey = await pushRequest('/vapid-public-key', { token, profileId })
  let subscription = await readyRegistration.pushManager.getSubscription()
  if (!subscription) {
    subscription = await readyRegistration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: decodeApplicationServerKey(publicKey.publicKey),
    })
  }
  await pushRequest('/subscriptions', { token, profileId, method: 'POST', body: subscription.toJSON() })
  return subscription
}

export async function disableBackgroundPush({ token, profileId }) {
  if (!('serviceWorker' in navigator)) return
  const registration = await navigator.serviceWorker.getRegistration('/')
  const subscription = await registration?.pushManager.getSubscription()
  if (!subscription) return
  await pushRequest('/subscriptions', {
    token,
    profileId,
    method: 'DELETE',
    body: { endpoint: subscription.endpoint },
  })
  await subscription.unsubscribe()
}

export function scheduleMedicationReminders(medication, auth) {
  return pushRequest('/medication-schedules', {
    ...auth,
    method: 'POST',
    body: buildMedicationSchedule(medication),
  })
}

export function removeMedicationReminders(medicationId, auth) {
  return pushRequest(`/medication-schedules/${encodeURIComponent(medicationId)}`, { ...auth, method: 'DELETE' })
}

export function scheduleSingleReminder(reminder, alarmId, auth) {
  if (!alarmId) throw new Error('O servidor não retornou o ID do alarme para agendamento.')
  if (!reminder.date || !reminder.time) throw new Error('Informe a data e o horário do alarme.')
  const startAt = new Date(`${reminder.date}T${reminder.time}`)
  if (Number.isNaN(startAt.getTime()) || startAt.getTime() <= Date.now()) throw new Error('Escolha uma data e horário futuros para o alarme.')
  return pushRequest('/alarms', {
    ...auth,
    method: 'POST',
    body: { sourceId: alarmId, startAt: startAt.toISOString(), localDate: reminder.date, localTime: reminder.time, timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone, title: 'Seu lembrete de saúde', displayName: reminder.name },
  })
}

export function removeSingleReminder(alarmId, auth) {
  return pushRequest(`/alarms/${encodeURIComponent(alarmId)}`, { ...auth, method: 'DELETE' })
}

export function listBackgroundAlarms(auth) {
  return pushRequest('/alarms', auth)
}

export function scheduleRestoredAlarm(alarm, auth) {
  if (!alarm?.id && !alarm?.sourceId) throw new Error('O alarme restaurado não possui um identificador válido.')
  const startAt = alarm.startAt || (alarm.date && alarm.time ? new Date(`${alarm.date}T${alarm.time}`).toISOString() : '')
  if (!startAt) throw new Error('Não foi possível recuperar o horário original do alarme.')
  return pushRequest('/alarms', {
    ...auth,
    method: 'POST',
    body: {
      sourceId: alarm.sourceId || alarm.id,
      startAt,
      localDate: alarm.localDate || alarm.date,
      localTime: alarm.localTime || alarm.time,
      intervalMinutes: alarm.intervalMinutes,
      durationDays: alarm.durationDays,
      timeZone: alarm.timeZone || Intl.DateTimeFormat().resolvedOptions().timeZone,
      oneTime: Boolean(alarm.oneTime),
      title: alarm.title || alarm.name || 'Seu lembrete de saúde',
      displayName: alarm.displayName || alarm.name,
    },
  })
}
