import { mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const directory = dirname(fileURLToPath(import.meta.url))
const storePath = resolve(process.env.PUSH_STORE_PATH || resolve(directory, '..', 'data', 'push-store.json'))
const emptyStore = () => ({ subscriptions: [], alarms: [] })
let storeMutation = Promise.resolve()
export const activeDispatches = new Set()

export async function readStore() {
  try {
    const data = JSON.parse(await readFile(storePath, 'utf8'))
    if (!Array.isArray(data.subscriptions) || !Array.isArray(data.alarms)) throw new Error('Invalid push store shape.')
    return data
  } catch (error) {
    if (error.code === 'ENOENT') return emptyStore()
    throw error
  }
}

export async function mutateStore(mutator) {
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
