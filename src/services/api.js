// Integração HTTP e fila offline do Katu'I.
import { flushSecureStorage, secureStorage } from './secureStorage'

const API_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:3000/api' : '')
const OFFLINE_QUEUE_KEY = 'katui.pending-actions'
const QUEUE_DB_NAME = 'katui-offline-storage'
const QUEUE_STORE_NAME = 'offline-queue'
const QUEUE_KEY_ID = 'encryption-key'
let queueDbPromise
let queueMutation = Promise.resolve()

export async function apiRequest(path, options = {}) {
  if (isGuestMode()) return guestApiRequest(path, options)
  if (isDemoMode()) return demoApiRequest(path, options)
  if (!API_URL) {
    const error = new Error('Configure VITE_API_URL para conectar o site ao servidor.')
    error.code = 'API_URL_NOT_CONFIGURED'
    throw error
  }
  const { timeout = 10000, idempotencyKey, profileId: requestedProfileId, ...fetchOptions } = options
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeout)
  const token = localStorage.getItem('katui.auth-token')
  const profileId = requestedProfileId === undefined ? localStorage.getItem('katui.active-profile-id') : requestedProfileId
  const isFormData = fetchOptions.body instanceof FormData
  const headers = { ...(isFormData ? {} : { 'Content-Type': 'application/json' }), ...options.headers }
  if (token) headers.Authorization = `Bearer ${token}`
  if (profileId) headers['X-Profile-Id'] = profileId
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey

  let response
  try {
    response = await fetch(`${API_URL}${path}`, { ...fetchOptions, headers, signal: controller.signal })
  } catch (error) {
    const networkError = new Error(error.name === 'AbortError' ? 'A API demorou para responder.' : 'Servidor indisponível.')
    networkError.code = error.name === 'AbortError' ? 'TIMEOUT' : 'NETWORK_ERROR'
    throw networkError
  } finally {
    clearTimeout(timeoutId)
  }

  if (!response.ok) {
    let body
    try {
      body = await response.json()
    } catch {
      body = null
    }
    const error = new Error(body?.message || `Erro na API: ${response.status}`)
    error.status = response.status
    error.code = body?.code || (response.status === 401 ? 'UNAUTHORIZED' : response.status === 404 ? 'NOT_FOUND' : 'API_ERROR')
    throw error
  }

  if (response.status === 204) return null
  const contentType = response.headers.get('content-type') || ''
  return contentType.includes('application/json') ? response.json() : response.blob()
}

export function isNetworkError(error) {
  return error?.code === 'NETWORK_ERROR' || error?.code === 'TIMEOUT'
}

export function isApiConfigured() {
  return Boolean(API_URL)
}

export function isDemoMode() {
  return Boolean(import.meta.env.DEV && localStorage.getItem('katui.demo-mode') === 'true')
}

export function isGuestMode() {
  return localStorage.getItem('katui.guest-mode') === 'true'
}

function readGuestData() {
  const stored = secureStorage.getItem('katui.guest-data')
  if (!stored) throw new Error('Os dados locais do modo Guest não foram inicializados. Saia e entre novamente no modo Guest.')
  const data = JSON.parse(stored)
  if (!data || typeof data !== 'object') throw new Error('Os dados locais do modo Guest estão inválidos.')
  return data
}

async function writeGuestData(data) {
  secureStorage.setItem('katui.guest-data', JSON.stringify(data))
  await flushSecureStorage()
}

async function guestApiRequest(path, options) {
  const [pathname, query = ''] = path.split('?')
  const method = (options.method || 'GET').toUpperCase()
  if (pathname === '/health') return Promise.resolve({ status: 'guest-local' })
  if (pathname.startsWith('/ai/')) return Promise.reject(new Error('Os recursos de inteligência artificial não estão disponíveis no modo Guest.'))
  if (pathname === '/backup') return Promise.reject(new Error('O backup exige uma conta. Seus registros Guest ficam somente neste dispositivo.'))
  const data = readGuestData()
  if (pathname === '/overview') {
    return Promise.resolve({
      medications: data.medications,
      vitalSigns: data['vital-signs'],
      appointments: data.appointments,
      symptoms: data.symptoms,
    })
  }
  const parts = pathname.split('/').filter(Boolean)
  const collection = parts[0]
  const resourceId = parts[1] ? decodeURIComponent(parts[1]) : null
  const collectionItems = Array.isArray(data[collection]) ? data[collection] : null
  if (!collectionItems) return Promise.reject(new Error('Este recurso não está disponível no modo Guest.'))
  if (method === 'GET' && !resourceId) {
    const date = new URLSearchParams(query).get('date')
    return Promise.resolve(date ? collectionItems.filter((item) => !item.date || item.date === date) : collectionItems)
  }
  if (method === 'POST' && !resourceId) {
    const item = { ...JSON.parse(options.body || '{}'), id: `guest-${collection}-${createId()}`, status: 'Ativo' }
    collectionItems.push(item)
    await writeGuestData(data)
    return Promise.resolve(item)
  }
  const itemIndex = collectionItems.findIndex((item) => item.id === resourceId || item._id === resourceId)
  if (method === 'DELETE' && resourceId) {
    if (itemIndex >= 0) collectionItems.splice(itemIndex, 1)
    await writeGuestData(data)
    return Promise.resolve(null)
  }
  if (method === 'PATCH' && resourceId) {
    if (itemIndex < 0) return Promise.reject(new Error('O registro solicitado não foi encontrado neste dispositivo.'))
    Object.assign(collectionItems[itemIndex], JSON.parse(options.body || '{}'))
    await writeGuestData(data)
    return Promise.resolve(collectionItems[itemIndex])
  }
  return Promise.reject(new Error('A ação solicitada não está disponível no modo Guest.'))
}

function readDemoData() {
  const stored = localStorage.getItem('katui.demo-data')
  if (!stored) throw new Error('Os dados de demonstração não foram inicializados. Saia e entre novamente no modo de demonstração.')
  const data = JSON.parse(stored)
  if (!data || typeof data !== 'object') throw new Error('Os dados de demonstração estão inválidos.')
  return data
}

function writeDemoData(data) {
  localStorage.setItem('katui.demo-data', JSON.stringify(data))
}

function demoApiRequest(path, options) {
  const [pathname, query = ''] = path.split('?')
  const method = (options.method || 'GET').toUpperCase()
  if (pathname === '/health') return Promise.resolve({ status: 'demo' })
  const data = readDemoData()
  if (pathname === '/me') {
    const profiles = JSON.parse(secureStorage.getItem('katui.profiles') || 'null') || data.profiles
    const activeProfileId = localStorage.getItem('katui.active-profile-id') || data.activeProfileId
    return Promise.resolve({ profiles, activeProfileId })
  }
  if (pathname === '/overview') {
    return Promise.resolve({
      medications: data.medications,
      vitalSigns: data['vital-signs'],
      appointments: data.appointments,
      symptoms: data.symptoms,
    })
  }
  if (pathname === '/ai/prescription' && method === 'POST') {
    return Promise.resolve({
      id: `demo-prescription-${createId()}`,
      status: 'review',
      medications: [{ name: 'Vitamina D (exemplo)', dose: '1 cápsula', schedule: '08:00' }],
      warnings: ['Sugestão fictícia para demonstração. Confira sempre a receita original.'],
    })
  }
  if (pathname === '/profiles/me' && method === 'POST') {
    const profile = { ...data.profiles[0], ...JSON.parse(options.body || '{}') }
    data.profiles = [profile, ...data.profiles.slice(1)]
    writeDemoData(data)
    return Promise.resolve(profile)
  }
  if (pathname === '/profiles' && method === 'POST') {
    const profile = { ...JSON.parse(options.body || '{}'), id: `demo-profile-${createId()}` }
    data.profiles.push(profile)
    writeDemoData(data)
    return Promise.resolve(profile)
  }
  const aiSettingsMatch = pathname.match(/^\/profiles\/([^/]+)\/ai-settings$/)
  if (aiSettingsMatch && method === 'PATCH') {
    const profile = data.profiles.find((item) => item.id === decodeURIComponent(aiSettingsMatch[1]))
    if (profile) profile.preferences = { ...profile.preferences, ai: { provider: 'system', configured: false, ...(profile.preferences?.ai || {}), ...JSON.parse(options.body || '{}'), apiKey: undefined } }
    writeDemoData(data)
    return Promise.resolve(profile || { ok: true })
  }
  const profileMatch = pathname.match(/^\/profiles\/([^/]+)$/)
  if (profileMatch && method === 'PATCH') {
    const profile = data.profiles.find((item) => item.id === decodeURIComponent(profileMatch[1]))
    if (profile) Object.assign(profile, JSON.parse(options.body || '{}'))
    writeDemoData(data)
    return Promise.resolve(profile || { ok: true })
  }
  if (pathname === '/backup') return Promise.resolve({ ok: true })
  const prescriptionConfirm = pathname.match(/^\/prescriptions\/([^/]+)\/confirm$/)
  if (prescriptionConfirm && method === 'POST') {
    const prescription = {
      ...JSON.parse(options.body || '{}'),
      id: decodeURIComponent(prescriptionConfirm[1]),
      status: 'Confirmada',
    }
    data.prescriptions.push(prescription)
    writeDemoData(data)
    return Promise.resolve(prescription)
  }
  const parts = pathname.split('/').filter(Boolean)
  const collection = parts[0]
  const resourceId = parts[1] ? decodeURIComponent(parts[1]) : null
  const collectionItems = Array.isArray(data[collection]) ? data[collection] : null
  if (!collectionItems) return Promise.resolve({ ok: true })
  if (method === 'GET' && !resourceId) {
    const date = new URLSearchParams(query).get('date')
    return Promise.resolve(date ? collectionItems.filter((item) => !item.date || item.date === date) : collectionItems)
  }
  if (method === 'POST' && !resourceId) {
    const item = { ...JSON.parse(options.body || '{}'), id: `demo-${collection}-${createId()}`, status: 'Ativo' }
    collectionItems.push(item)
    writeDemoData(data)
    return Promise.resolve(item)
  }
  const itemIndex = collectionItems.findIndex((item) => item.id === resourceId || item._id === resourceId)
  if (method === 'DELETE' && resourceId) {
    if (itemIndex >= 0) collectionItems.splice(itemIndex, 1)
    writeDemoData(data)
    return Promise.resolve(null)
  }
  if (method === 'PATCH' && resourceId) {
    if (itemIndex >= 0) Object.assign(collectionItems[itemIndex], JSON.parse(options.body || '{}'))
    writeDemoData(data)
    return Promise.resolve(itemIndex >= 0 ? collectionItems[itemIndex] : { ok: true })
  }
  return Promise.resolve({ ok: true })
}

function notifyResourceChange(endpoint) {
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('katui:resource-change', { detail: { endpoint } }))
}

function notifyQueueError(action, error) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('katui:queue-error', {
      detail: { endpoint: action.endpoint, message: error.message, id: action.id },
    }))
  }
}

export const healthApi = {
  getOverview: () => apiRequest('/overview'),
  getMedications: () => apiRequest('/medications'),
  getVitalSigns: () => apiRequest('/vital-signs'),
}

export const resourceApi = {
  list: (endpoint, params = '') => apiRequest(`${endpoint}${params ? `?${params}` : ''}`),
  get: (endpoint, id) => apiRequest(`${endpoint}/${id}`),
  create: (endpoint, data) => apiRequest(endpoint, { method: 'POST', body: JSON.stringify(data) }),
  update: (endpoint, id, data) => apiRequest(`${endpoint}/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),
  remove: (endpoint, id) => apiRequest(`${endpoint}/${id}`, { method: 'DELETE' }),
}

function createId() {
  return globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function openQueueDb() {
  if (queueDbPromise) return queueDbPromise
  if (!globalThis.indexedDB) return Promise.reject(new Error('O navegador não oferece armazenamento offline seguro (IndexedDB).'))
  if (!globalThis.crypto?.subtle) return Promise.reject(new Error('O navegador não oferece criptografia segura para armazenar dados offline.'))
  queueDbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(QUEUE_DB_NAME, 1)
    request.onupgradeneeded = () => request.result.createObjectStore(QUEUE_STORE_NAME)
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || new Error('INDEXED_DB_ERROR'))
  })
  return queueDbPromise
}

function requestResult(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || new Error('INDEXED_DB_ERROR'))
  })
}

async function getEncryptionKey(db) {
  const read = db.transaction(QUEUE_STORE_NAME, 'readonly').objectStore(QUEUE_STORE_NAME).get(QUEUE_KEY_ID)
  const existing = await requestResult(read)
  if (existing) return existing
  const key = await globalThis.crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt'])
  const write = db.transaction(QUEUE_STORE_NAME, 'readwrite').objectStore(QUEUE_STORE_NAME).put(key, QUEUE_KEY_ID)
  await requestResult(write)
  return key
}

async function encryptItem(item, key) {
  const iv = globalThis.crypto.getRandomValues(new Uint8Array(12))
  const encoded = new TextEncoder().encode(JSON.stringify(item))
  const encrypted = await globalThis.crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encoded)
  return { id: item.id, queuedAt: item.queuedAt, iv: Array.from(iv), payload: Array.from(new Uint8Array(encrypted)) }
}

async function decryptItem(record, key) {
  const decrypted = await globalThis.crypto.subtle.decrypt({ name: 'AES-GCM', iv: new Uint8Array(record.iv) }, key, new Uint8Array(record.payload))
  return JSON.parse(new TextDecoder().decode(decrypted))
}

async function serializeQueuedAction(action) {
  const file = action.data?.file
  if (!(file instanceof Blob)) return action
  const bytes = Array.from(new Uint8Array(await file.arrayBuffer()))
  return { ...action, data: { ...action.data, file: { __file: true, name: file.name || action.data.fileName || 'arquivo', type: file.type || 'application/octet-stream', lastModified: file.lastModified || Date.now(), bytes } } }
}

async function readIndexedQueue() {
  const db = await openQueueDb()
  const key = await getEncryptionKey(db)
  const record = await requestResult(db.transaction(QUEUE_STORE_NAME, 'readonly').objectStore(QUEUE_STORE_NAME).get('items'))
  if (!record?.length) return []
  return Promise.all(record.map((item) => decryptItem(item, key)))
}

async function writeIndexedQueue(queue) {
  const db = await openQueueDb()
  const key = await getEncryptionKey(db)
  const encrypted = await Promise.all(queue.map((item) => encryptItem(item, key)))
  const transaction = db.transaction(QUEUE_STORE_NAME, 'readwrite')
  transaction.objectStore(QUEUE_STORE_NAME).put(encrypted, 'items')
  return new Promise((resolve, reject) => {
    transaction.oncomplete = resolve
    transaction.onerror = () => reject(transaction.error || new Error('INDEXED_DB_ERROR'))
  })
}

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
