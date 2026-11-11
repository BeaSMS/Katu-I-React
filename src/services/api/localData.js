import { flushSecureStorage, secureStorage } from '../secureStorage'

export function readGuestData() {
  const stored = secureStorage.getItem('katui.guest-data')
  if (!stored) throw new Error('Os dados locais do modo Guest não foram inicializados. Saia e entre novamente no modo Guest.')
  const data = JSON.parse(stored)
  if (!data || typeof data !== 'object') throw new Error('Os dados locais do modo Guest estão inválidos.')
  return data
}

export async function readFormData(body) {
  const data = {}
  for (const [key, value] of body.entries()) {
    if (key === 'file' && value instanceof Blob) {
      const bytes = new Uint8Array(await value.arrayBuffer())
      const name = value.name || 'arquivo'
      const contentType = value.type || (/\.pdf$/i.test(name) ? 'application/pdf' : /\.(jpe?g)$/i.test(name) ? 'image/jpeg' : /\.png$/i.test(name) ? 'image/png' : 'application/octet-stream')
      let binary = ''
      for (let offset = 0; offset < bytes.length; offset += 0x8000) {
        binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000))
      }
      data.attachment = {
        name,
        contentType,
        size: value.size,
        dataUrl: `data:${contentType};base64,${btoa(binary)}`,
      }
    } else {
      data[key] = value
    }
  }
  return data
}

export async function writeGuestData(data) {
  secureStorage.setItem('katui.guest-data', JSON.stringify(data))
  await flushSecureStorage()
}


export function readDemoData() {
  const stored = secureStorage.getItem('katui.demo-data')
  if (!stored) throw new Error('Os dados de demonstração não foram inicializados. Saia e entre novamente no modo de demonstração.')
  const data = JSON.parse(stored)
  if (!data || typeof data !== 'object') throw new Error('Os dados de demonstração estão inválidos.')
  return data
}

export async function writeDemoData(data) {
  secureStorage.setItem('katui.demo-data', JSON.stringify(data))
  await flushSecureStorage()
}


export function createId() {
  return globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

