export const VALUES_STORE = 'encrypted-values'
export const KEYS_STORE = 'crypto-keys'
export const ENCRYPTION_KEY_ID = 'aes-gcm-key'
export const SECURE_KEYS = new Set([
  'katui.guest-data',
  'katui.guest-profile',
  'katui.profiles',
  'katui.profile-draft',
  'katui.onboarding-draft',
  'katui.demo-data',
])
const DB_NAME = 'katui-secure-storage'
const DB_VERSION = 1
let databasePromise

export function storageError(message) {
  return new Error(message)
}

export function openDatabase() {
  if (databasePromise) return databasePromise
  if (!globalThis.indexedDB) return Promise.reject(storageError('O navegador não oferece armazenamento local seguro (IndexedDB).'))
  if (!globalThis.crypto?.subtle) return Promise.reject(storageError('O navegador não oferece criptografia segura para os dados locais.'))

  databasePromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const database = request.result
      if (!database.objectStoreNames.contains(VALUES_STORE)) database.createObjectStore(VALUES_STORE, { keyPath: 'key' })
      if (!database.objectStoreNames.contains(KEYS_STORE)) database.createObjectStore(KEYS_STORE)
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || storageError('Não foi possível abrir o armazenamento local seguro.'))
    request.onblocked = () => reject(storageError('A atualização do armazenamento local está bloqueada por outra aba. Feche as outras abas e tente novamente.'))
  }).catch((error) => {
    databasePromise = null
    throw error
  })
  return databasePromise
}

export function requestResult(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || storageError('Não foi possível ler o armazenamento local seguro.'))
  })
}

export function transactionResult(transaction) {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error || storageError('Não foi possível gravar o armazenamento local seguro.'))
    transaction.onabort = () => reject(transaction.error || storageError('A gravação do armazenamento local seguro foi cancelada.'))
  })
}

export async function getEncryptionKey(database) {
  const existing = await requestResult(database.transaction(KEYS_STORE, 'readonly').objectStore(KEYS_STORE).get(ENCRYPTION_KEY_ID))
  if (existing) return existing

  const key = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt'])
  const transaction = database.transaction(KEYS_STORE, 'readwrite')
  transaction.objectStore(KEYS_STORE).put(key, ENCRYPTION_KEY_ID)
  await transactionResult(transaction)
  return key
}

export async function encryptValue(key, value) {
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const plaintext = new TextEncoder().encode(value)
  const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, plaintext)
  return { iv: Array.from(iv), ciphertext }
}

export async function decryptValue(key, record) {
  const plaintext = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: new Uint8Array(record.iv) },
    key,
    record.ciphertext,
  )
  return new TextDecoder().decode(plaintext)
}
