import { apiRequest, isNetworkError, notifyQueueError, notifyResourceChange } from '../../api'
import { secureStorage, flushSecureStorage } from '../../secureStorage'
import { createId } from '../localData'
import { decryptItem, encryptItem, openQueueDb, readIndexedQueue, requestResult, serializeQueuedAction, writeIndexedQueue } from './storage'

const OFFLINE_QUEUE_KEY = 'katui.pending-actions'
let queueMutation = Promise.resolve()

function readLegacyQueue() {
  const stored = localStorage.getItem(OFFLINE_QUEUE_KEY)
  if (!stored) return []
  const queue = JSON.parse(stored)
  if (!Array.isArray(queue)) throw new Error('A fila offline antiga está corrompida.')
  return queue
}

function notifyQueueChange(count) {
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('katui:queue-change', { detail: { count } }))
}

async function readQueue() {
  const queue = await readIndexedQueue()
  const legacy = readLegacyQueue()
  if (!legacy.length) return queue
  const migrated = legacy.map((item) => ({ ...item, id: item.id || createId() }))
  const queueIds = new Set(queue.map((item) => item.id))
  const combined = [...queue, ...migrated.filter((item) => !queueIds.has(item.id))]
  await writeIndexedQueue(combined)
  localStorage.removeItem(OFFLINE_QUEUE_KEY)
  return combined
}

async function remapLocalProfile(localId, serverProfile, queue) {
  if (!localId || !serverProfile?.id) return queue
  const profiles = JSON.parse(secureStorage.getItem('katui.profiles') || '[]')
  const existing = profiles.find((profile) => profile.id === localId)
  if (existing) {
    const nextProfiles = profiles.map((profile) => profile.id === localId ? { ...profile, ...serverProfile, id: serverProfile.id } : profile)
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
    await flushSecureStorage()
    if (localStorage.getItem('katui.active-profile-id') === localId) {
      localStorage.setItem('katui.active-profile-id', serverProfile.id)
    }
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('katui:profile-id-change', { detail: { localId, profile: nextProfiles.find((profile) => profile.id === serverProfile.id) } }))
    }
  }
  return queue.map((action) => ({
    ...action,
    ...(action.profileId === localId ? { profileId: serverProfile.id } : {}),
    ...(action.data?.profileId === localId ? { data: { ...action.data, profileId: serverProfile.id } } : {}),
  }))
}

async function writeQueue(queue) {
  await writeIndexedQueue(queue)
  localStorage.removeItem(OFFLINE_QUEUE_KEY)
}

export function queueOfflineAction(action) {
  const item = { ...action, id: action.id || createId(), queuedAt: new Date().toISOString() }
  const mutation = queueMutation.then(async () => {
    const queue = await readQueue()
    queue.push(await serializeQueuedAction(item))
    await writeQueue(queue)
    notifyQueueChange(queue.length)
  })
  queueMutation = mutation.catch(() => {})
  return mutation.then(() => item.id)
}

export async function flushOfflineQueue() {
  if (typeof navigator !== 'undefined' && navigator.onLine === false) return 0
  await queueMutation
  let queue = await readQueue()
  let synced = 0
  const existingFailure = queue.find((action) => action.failed)
  if (existingFailure) notifyQueueError(existingFailure, new Error(existingFailure.failure || 'Uma ação offline precisa de atenção.'))
  let index = 0
  while (index < queue.length) {
    const action = queue[index]
    if (action.failed) {
      index += 1
      continue
    }
    const queuedFile = action.data?.file?.__file ? action.data.file : null
    let result
    try {
      if (queuedFile) {
        const { file: _file, ...data } = action.data
        const body = new FormData()
        body.append('file', new Blob([new Uint8Array(queuedFile.bytes)], { type: queuedFile.type }), queuedFile.name)
        Object.entries(data).forEach(([key, value]) => { if (value !== undefined && value !== null) body.append(key, String(value)) })
        result = await apiRequest(action.endpoint, { method: action.method || 'POST', idempotencyKey: action.id, profileId: action.profileId, body })
      } else {
        result = await apiRequest(action.endpoint, {
          method: action.method || 'POST',
          idempotencyKey: action.id,
          profileId: action.profileId,
          ...(action.method === 'DELETE' ? {} : { body: JSON.stringify(action.data || {}) }),
        })
      }
    } catch (error) {
      if (isNetworkError(error) || !error.status || error.status === 401 || error.status === 403 || error.status >= 500) throw error
      action.failed = true
      action.failure = error.message
      await writeQueue(queue)
      notifyQueueError(action, error)
      index += 1
      continue
    }
    if (action.localProfileId) queue = await remapLocalProfile(action.localProfileId, result?.profile || result, queue)
    queue = queue.filter((item) => item.id !== action.id)
    await writeQueue(queue)
    notifyQueueChange(queue.length)
    notifyResourceChange(action.endpoint)
    synced += 1
  }
  return synced
}

export async function getOfflineQueueSize() {
  await queueMutation
  return (await readQueue()).length
}

export function clearOfflineQueue() {
  const mutation = queueMutation.then(async () => {
    await writeQueue([])
    localStorage.removeItem(OFFLINE_QUEUE_KEY)
    notifyQueueChange(0)
  })
  queueMutation = mutation.catch(() => {})
  return mutation
}
