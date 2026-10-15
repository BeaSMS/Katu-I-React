const DB_NAME = 'katui-secure-storage'
const DB_VERSION = 1
const VALUES_STORE = 'encrypted-values'
const KEYS_STORE = 'crypto-keys'
const ENCRYPTION_KEY_ID = 'aes-gcm-key'
const SECURE_KEYS = new Set([
  'katui.guest-data',
  'katui.guest-profile',
  'katui.profiles',
  'katui.profile-draft',
  'katui.onboarding-draft',
])

const values = new Map()
let databasePromise
let encryptionKey
let initialized = false
let writeQueue = Promise.resolve()
let lastWriteError = null
const writeErrors = new Map()
let storageChannel

function storageError(message) {
  return new Error(message)
}

function openDatabase() {
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

function requestResult(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error || storageError('Não foi possível ler o armazenamento local seguro.'))
  })
}

function transactionResult(transaction) {
  return new Promise((resolve, reject) => {
    transaction.oncomplete = () => resolve()
    transaction.onerror = () => reject(transaction.error || storageError('Não foi possível gravar o armazenamento local seguro.'))
    transaction.onabort = () => reject(transaction.error || storageError('A gravação do armazenamento local seguro foi cancelada.'))
  })
}

async function getEncryptionKey(database) {
  const existing = await requestResult(database.transaction(KEYS_STORE, 'readonly').objectStore(KEYS_STORE).get(ENCRYPTION_KEY_ID))
  if (existing) return existing

  const key = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt'])
  const transaction = database.transaction(KEYS_STORE, 'readwrite')
  transaction.objectStore(KEYS_STORE).put(key, ENCRYPTION_KEY_ID)
  await transactionResult(transaction)
  return key
}

async function encryptValue(key, value) {
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const plaintext = new TextEncoder().encode(value)
  const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, plaintext)
  return { iv: Array.from(iv), ciphertext }
}

async function decryptValue(key, record) {
  const plaintext = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: new Uint8Array(record.iv) },
    key,
    record.ciphertext,
  )
  return new TextDecoder().decode(plaintext)
}

async function persistSnapshot() {
  const database = await openDatabase()
  if (!encryptionKey) encryptionKey = await getEncryptionKey(database)
  const encryptedValues = await Promise.all(
    [...values].map(async ([key, value]) => ({ key, ...(await encryptValue(encryptionKey, value)) })),
  )
  const transaction = database.transaction(VALUES_STORE, 'readwrite')
  const store = transaction.objectStore(VALUES_STORE)
  store.clear()
  encryptedValues.forEach((record) => store.put(record))
  await transactionResult(transaction)
}

async function persistValue(key, value) {
  const database = await openDatabase()
  if (!encryptionKey) encryptionKey = await getEncryptionKey(database)
  const record = value === null ? null : { key, ...(await encryptValue(encryptionKey, value)) }
  const transaction = database.transaction(VALUES_STORE, 'readwrite')
  if (value === null) {
    transaction.objectStore(VALUES_STORE).delete(key)
  } else {
    transaction.objectStore(VALUES_STORE).put(record)
  }
  await transactionResult(transaction)
}

function reportWriteError(key, error) {
  writeErrors.set(key, error)
  lastWriteError = error
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('katui:secure-storage-error', {
      detail: { message: error.message || 'Não foi possível salvar os dados locais protegidos.' },
    }))
  }
}

function enqueueValue(key, value) {
  const write = writeQueue.then(async () => {
    await persistValue(key, value)
    writeErrors.delete(key)
    lastWriteError = writeErrors.values().next().value || null
  })
  writeQueue = write.catch((error) => reportWriteError(key, error))
}

function broadcastValue(key, value) {
  storageChannel?.postMessage({ key, value })
}

export async function initializeSecureStorage() {
  if (initialized) return
  const database = await openDatabase()
  encryptionKey = await getEncryptionKey(database)
  const records = await requestResult(database.transaction(VALUES_STORE, 'readonly').objectStore(VALUES_STORE).getAll())
  const existingKeys = new Set()
  for (const record of records) {
    if (!SECURE_KEYS.has(record.key)) continue
    values.set(record.key, await decryptValue(encryptionKey, record))
    existingKeys.add(record.key)
  }

  const legacyValues = []
  for (const key of SECURE_KEYS) {
    const legacyValue = localStorage.getItem(key)
    if (legacyValue === null) continue
    if (!existingKeys.has(key)) {
      values.set(key, legacyValue)
      legacyValues.push(key)
    } else {
      legacyValues.push(key)
    }
  }

  if (legacyValues.length) {
    await persistSnapshot()
    legacyValues.forEach((key) => localStorage.removeItem(key))
  }
  if (typeof BroadcastChannel !== 'undefined') {
    storageChannel = new BroadcastChannel('katui-secure-storage')
    storageChannel.onmessage = ({ data }) => {
      if (data?.type === 'clear') values.clear()
      else if (!data || !SECURE_KEYS.has(data.key)) return
      else if (data.value === null) values.delete(data.key)
      else if (typeof data.value === 'string') values.set(data.key, data.value)
    }
  }
  initialized = true
}

function assertInitialized() {
  if (!initialized) throw storageError('O armazenamento local protegido ainda não foi inicializado.')
}

export const secureStorage = {
  getItem(key) {
    assertInitialized()
    if (!SECURE_KEYS.has(key)) throw storageError(`A chave ${key} não pertence ao armazenamento protegido.`)
    return values.has(key) ? values.get(key) : null
  },

  setItem(key, value) {
    assertInitialized()
    if (!SECURE_KEYS.has(key)) throw storageError(`A chave ${key} não pertence ao armazenamento protegido.`)
    const storedValue = String(value)
    values.set(key, storedValue)
    broadcastValue(key, storedValue)
    enqueueValue(key, storedValue)
  },

  removeItem(key) {
    assertInitialized()
    if (!SECURE_KEYS.has(key)) throw storageError(`A chave ${key} não pertence ao armazenamento protegido.`)
    values.delete(key)
    broadcastValue(key, null)
    enqueueValue(key, null)
  },
}

export async function clearSecureStorage() {
  assertInitialized()
  await writeQueue
  const database = await openDatabase()
  const transaction = database.transaction([VALUES_STORE, KEYS_STORE], 'readwrite')
  transaction.objectStore(VALUES_STORE).clear()
  transaction.objectStore(KEYS_STORE).delete(ENCRYPTION_KEY_ID)
  await transactionResult(transaction)
  values.clear()
  storageChannel?.postMessage({ type: 'clear' })
  encryptionKey = null
  lastWriteError = null
  writeErrors.clear()
}

export async function flushSecureStorage() {
  assertInitialized()
  await writeQueue
  if (writeErrors.size) throw lastWriteError
}
