const QUEUE_DB_NAME = 'katui-offline-storage'
const QUEUE_STORE_NAME = 'offline-queue'
const QUEUE_KEY_ID = 'encryption-key'
let queueDbPromise

export function openQueueDb() {
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

export function requestResult(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || new Error('INDEXED_DB_ERROR'))
  })
}

export async function getEncryptionKey(db) {
  const read = db.transaction(QUEUE_STORE_NAME, 'readonly').objectStore(QUEUE_STORE_NAME).get(QUEUE_KEY_ID)
  const existing = await requestResult(read)
  if (existing) return existing
  const key = await globalThis.crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt'])
  const write = db.transaction(QUEUE_STORE_NAME, 'readwrite').objectStore(QUEUE_STORE_NAME).put(key, QUEUE_KEY_ID)
  await requestResult(write)
  return key
}

export async function encryptItem(item, key) {
  const iv = globalThis.crypto.getRandomValues(new Uint8Array(12))
  const encoded = new TextEncoder().encode(JSON.stringify(item))
  const encrypted = await globalThis.crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encoded)
  return { id: item.id, queuedAt: item.queuedAt, iv: Array.from(iv), payload: Array.from(new Uint8Array(encrypted)) }
}

export async function decryptItem(record, key) {
  const decrypted = await globalThis.crypto.subtle.decrypt({ name: 'AES-GCM', iv: new Uint8Array(record.iv) }, key, new Uint8Array(record.payload))
  return JSON.parse(new TextDecoder().decode(decrypted))
}

export async function serializeQueuedAction(action) {
  const file = action.data?.file
  if (!(file instanceof Blob)) return action
  const bytes = Array.from(new Uint8Array(await file.arrayBuffer()))
  return { ...action, data: { ...action.data, file: { __file: true, name: file.name || action.data.fileName || 'arquivo', type: file.type || 'application/octet-stream', lastModified: file.lastModified || Date.now(), bytes } } }
}

export async function readIndexedQueue() {
  const db = await openQueueDb()
  const key = await getEncryptionKey(db)
  const record = await requestResult(db.transaction(QUEUE_STORE_NAME, 'readonly').objectStore(QUEUE_STORE_NAME).get('items'))
  if (!record?.length) return []
  return Promise.all(record.map((item) => decryptItem(item, key)))
}

export async function writeIndexedQueue(queue) {
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

