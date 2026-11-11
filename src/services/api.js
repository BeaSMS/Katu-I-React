// Integração HTTP do Katu'I.
import { demoApiRequest } from './api/demo'
import { guestApiRequest } from './api/guest'

const API_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:3000/api' : '')
const AI_PRESCRIPTION_ENABLED = import.meta.env.VITE_AI_PRESCRIPTION_ENABLED === 'true'

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

export function isAiPrescriptionEnabled() {
  return AI_PRESCRIPTION_ENABLED
}

export function isDemoMode() {
  return Boolean(import.meta.env.DEV && localStorage.getItem('katui.demo-mode') === 'true')
}

export function isGuestMode() {
  return localStorage.getItem('katui.guest-mode') === 'true'
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

export { queueOfflineAction, flushOfflineQueue, getOfflineQueueSize, clearOfflineQueue } from './api/offlineQueue'
export { notifyResourceChange, notifyQueueError }
