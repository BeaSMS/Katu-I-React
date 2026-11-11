import assert from 'node:assert/strict'
import test from 'node:test'
import { filterMedicalReportRecords, getMedicalReportDetails } from '../src/services/medicalReport.js'
import {
  getMedicalReportMedicationSummary,
  groupMedicalReportSymptoms,
  groupMedicalReportVitals,
} from '../src/services/medicalReportSummaries.js'

test('filtra registros por intervalo inclusivo e preserva o histórico sem datas', () => {
  const records = [
    { id: 'before', date: '2026-10-31' },
    { id: 'start', date: '2026-11-01T10:00:00.000Z' },
    { id: 'end', startDate: '2026-11-30' },
    { id: 'after', date: '2026-12-01' },
    { id: 'undated' },
  ]

  assert.deepEqual(
    filterMedicalReportRecords(records, '2026-11-01', '2026-11-30').map((record) => record.id),
    ['start', 'end'],
  )
  assert.equal(filterMedicalReportRecords(records).length, records.length)
})

test('recusa intervalos invertidos e apresenta valores clínicos registrados', () => {
  assert.throws(() => filterMedicalReportRecords([], '2026-11-30', '2026-11-01'), /data inicial/)
  assert.deepEqual(getMedicalReportDetails('vitalSigns', { systolic: 120, diastolic: 80, notes: 'Em repouso' }), [
    ['Valor', '120/80 mmHg'],
    ['Observação', 'Em repouso'],
  ])
})

test('resume tratamentos com vigência nos últimos 30 dias e uso contínuo ativo', () => {
  const summary = getMedicalReportMedicationSummary(
    [
      { id: 'recent', name: 'Tratamento recente', date: '2026-10-28', durationDays: '5', status: 'Ativo' },
      { id: 'continuous', name: 'Uso contínuo', date: '2025-01-01', continuous: true, status: 'Ativo' },
      { id: 'ended', name: 'Tratamento encerrado', date: '2026-10-01', durationDays: '4', status: 'Concluído' },
      { id: 'inactive', name: 'Uso contínuo suspenso', continuous: true, status: 'Suspenso' },
    ],
    '2026-11-11',
  )

  assert.equal(summary.firstDay, '2026-10-13')
  assert.deepEqual(
    summary.recent.map(({ record }) => record.id),
    ['recent', 'continuous'],
  )
  assert.deepEqual(
    summary.continuous.map(({ record }) => record.id),
    ['continuous'],
  )
})

test('agrupa sintomas por frequência e sinais vitais por tipo e data', () => {
  const symptoms = groupMedicalReportSymptoms([
    { symptom: 'Dor de cabeça', date: '2026-11-10' },
    { symptom: 'Náusea', date: '2026-11-09' },
    { symptom: 'Dor de cabeça', date: '2026-11-11', time: '08:30' },
  ])
  assert.deepEqual(
    symptoms.map(({ symptom, count }) => [symptom, count]),
    [
      ['Dor de cabeça', 2],
      ['Náusea', 1],
    ],
  )
  assert.equal(symptoms[0].occurrences[0].date, '2026-11-11')

  const vitals = groupMedicalReportVitals([
    { type: 'Peso', date: '2026-11-11' },
    { type: 'Pressão arterial', date: '2026-11-10' },
    { type: 'Peso', date: '2026-11-08' },
  ])
  assert.deepEqual(
    vitals.map(({ type }) => type),
    ['Peso', 'Pressão arterial'],
  )
  assert.deepEqual(
    vitals[0].records.map((record) => record.date),
    ['2026-11-08', '2026-11-11'],
  )
})
