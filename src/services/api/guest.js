import { readFormData, readGuestData, writeGuestData, createId } from './localData'

export async function guestApiRequest(path, options) {
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
    const body = options.body instanceof FormData ? await readFormData(options.body) : JSON.parse(options.body || '{}')
    const item = { ...body, id: `guest-${collection}-${createId()}`, status: 'Ativo' }
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
    const body = options.body instanceof FormData ? await readFormData(options.body) : JSON.parse(options.body || '{}')
    Object.assign(collectionItems[itemIndex], body)
    await writeGuestData(data)
    return Promise.resolve(collectionItems[itemIndex])
  }
  return Promise.reject(new Error('A ação solicitada não está disponível no modo Guest.'))
}

