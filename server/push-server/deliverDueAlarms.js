import webPush from 'web-push'
import { advanceSchedule } from '../alarm-schedule.js'
import { activeDispatches, mutateStore, readStore } from './store.js'

export async function deliverDueAlarms() {
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

