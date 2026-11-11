import { getMedicalReportDate, getMedicalReportTitle } from './medicalReport.js'

function printableText(value) {
  if (Array.isArray(value)) return value.filter(Boolean).join(', ')
  return typeof value === 'string' || typeof value === 'number' ? String(value).trim() : ''
}

function shiftDate(dateValue, days) {
  const date = new Date(`${dateValue}T00:00:00`)
  date.setDate(date.getDate() + days)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function localDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getMedicationDates(record) {
  const startDate = getMedicalReportDate({ date: record.startDate || record.date })
  const duration = Number(record.durationDays)
  const endDate =
    getMedicalReportDate({ date: record.endDate }) ||
    (!record.continuous && startDate && Number.isInteger(duration) && duration > 0
      ? shiftDate(startDate, duration - 1)
      : '')
  return { startDate, endDate }
}

function isMedicationActive(record) {
  const status = String(record.status || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
  return !['cancelado', 'concluido', 'encerrado', 'expirado', 'inativo', 'suspenso'].includes(status)
}

export function getMedicalReportMedicationSummary(records, today = localDateKey(new Date())) {
  if (!Array.isArray(records)) throw new Error('Os medicamentos do relatório precisam estar em uma lista.')
  const firstDay = shiftDate(today, -29)
  const medications = records.map((record) => ({ record, ...getMedicationDates(record) }))
  const isContinuous = (record) => record.continuous === true || record.continuous === 'true'
  const recent = medications.filter(
    ({ startDate, endDate }) => startDate && startDate <= today && (!endDate || endDate >= firstDay),
  )
  const continuous = medications.filter(
    ({ record, startDate, endDate }) =>
      isContinuous(record) &&
      isMedicationActive(record) &&
      (!startDate || startDate <= today) &&
      (!endDate || endDate >= today),
  )
  const byName = (first, second) =>
    String(first.record.name || '').localeCompare(String(second.record.name || ''), 'pt-BR')
  return { firstDay, recent: recent.sort(byName), continuous: continuous.sort(byName) }
}

export function groupMedicalReportSymptoms(records) {
  if (!Array.isArray(records)) throw new Error('Os sintomas do relatório precisam estar em uma lista.')
  const groups = new Map()
  for (const record of records) {
    const symptom = getMedicalReportTitle('symptoms', record)
    if (!groups.has(symptom)) groups.set(symptom, [])
    groups.get(symptom).push({
      date: getMedicalReportDate(record),
      time: printableText(record.time),
      intensity: printableText(record.intensity),
    })
  }
  return [...groups.entries()]
    .map(([symptom, occurrences]) => ({
      symptom,
      count: occurrences.length,
      occurrences: occurrences.sort((first, second) =>
        `${second.date} ${second.time}`.localeCompare(`${first.date} ${first.time}`),
      ),
    }))
    .sort((first, second) => second.count - first.count || first.symptom.localeCompare(second.symptom, 'pt-BR'))
}

export function groupMedicalReportVitals(records) {
  if (!Array.isArray(records)) throw new Error('Os sinais vitais do relatório precisam estar em uma lista.')
  const groups = new Map()
  for (const record of records) {
    const type = printableText(record.type) || 'Sinal vital'
    if (!groups.has(type)) groups.set(type, [])
    groups.get(type).push(record)
  }
  return [...groups.entries()]
    .map(([type, entries]) => ({
      type,
      records: entries.sort((first, second) =>
        `${getMedicalReportDate(first)} ${first.time || ''}`.localeCompare(
          `${getMedicalReportDate(second)} ${second.time || ''}`,
        ),
      ),
    }))
    .sort((first, second) => first.type.localeCompare(second.type, 'pt-BR'))
}
