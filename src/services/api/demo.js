import { secureStorage } from '../secureStorage'
import { readDemoData, writeDemoData, readFormData, createId } from './localData'

export async function demoApiRequest(path, options) {
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
    await writeDemoData(data)
    return Promise.resolve(profile)
  }
  if (pathname === '/profiles' && method === 'POST') {
    const profile = { ...JSON.parse(options.body || '{}'), id: `demo-profile-${createId()}` }
    data.profiles.push(profile)
    await writeDemoData(data)
    return Promise.resolve(profile)
  }
  const aiSettingsMatch = pathname.match(/^\/profiles\/([^/]+)\/ai-configuracoes$/)
  if (aiSettingsMatch && method === 'PATCH') {
    const profile = data.profiles.find((item) => item.id === decodeURIComponent(aiSettingsMatch[1]))
    if (profile) profile.preferences = { ...profile.preferences, ai: { provider: 'system', configured: false, ...(profile.preferences?.ai || {}), ...JSON.parse(options.body || '{}'), apiKey: undefined } }
    await writeDemoData(data)
    return Promise.resolve(profile || { ok: true })
  }
  const profileMatch = pathname.match(/^\/profiles\/([^/]+)$/)
  if (profileMatch && method === 'PATCH') {
    const profile = data.profiles.find((item) => item.id === decodeURIComponent(profileMatch[1]))
    if (profile) Object.assign(profile, JSON.parse(options.body || '{}'))
    await writeDemoData(data)
    return Promise.resolve(profile || { ok: true })
  }
  if (pathname === '/backup') return Promise.resolve({ ok: true })
  const prescriptionConfirm = pathname.match(/^\/prescriptions\/([^/]+)\/confirm$/)
  if (prescriptionConfirm && method === 'POST') {
    const body = JSON.parse(options.body || '{}')
    const prescriptionId = decodeURIComponent(prescriptionConfirm[1])
    const prescription = data.prescriptions.find((item) => String(item.id || item._id) === prescriptionId)
    const savedMedications = (body.medications || []).map((medication) => ({
      ...medication,
      dosage: medication.dosage || medication.dose || '',
      id: `demo-medications-${createId()}`,
      prescriptionId,
      status: 'Ativo',
    }))
    if (prescription) {
      Object.assign(prescription, { status: 'Confirmada', analysisId: body.analysisId, medicationIds: savedMedications.map((item) => item.id) })
    } else {
      data.prescriptions.push({
        id: prescriptionId,
        status: 'Confirmada',
        analysisId: body.analysisId,
        medicationIds: savedMedications.map((item) => item.id),
      })
    }
    data.medications.push(...savedMedications)
    await writeDemoData(data)
    return Promise.resolve({ prescription: prescription || data.prescriptions[data.prescriptions.length - 1], medications: savedMedications })
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
    const body = options.body instanceof FormData ? await readFormData(options.body) : JSON.parse(options.body || '{}')
    const item = { ...body, id: `demo-${collection}-${createId()}`, status: 'Ativo' }
    collectionItems.push(item)
    await writeDemoData(data)
    return Promise.resolve(item)
  }
  const itemIndex = collectionItems.findIndex((item) => item.id === resourceId || item._id === resourceId)
  if (method === 'DELETE' && resourceId) {
    if (itemIndex >= 0) collectionItems.splice(itemIndex, 1)
    await writeDemoData(data)
    return Promise.resolve(null)
  }
  if (method === 'PATCH' && resourceId) {
    const body = options.body instanceof FormData ? await readFormData(options.body) : JSON.parse(options.body || '{}')
    if (itemIndex >= 0) Object.assign(collectionItems[itemIndex], body)
    await writeDemoData(data)
    return Promise.resolve(itemIndex >= 0 ? collectionItems[itemIndex] : { ok: true })
  }
  return Promise.resolve({ ok: true })
}

