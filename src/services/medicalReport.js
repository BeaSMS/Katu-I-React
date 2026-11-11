export const medicalReportSections = [
  { id: 'profile', label: 'Perfil, alergias e condições informadas', endpoints: [] },
  { id: 'medications', label: 'Medicamentos', endpoints: ['/medications'] },
  { id: 'vitalSigns', label: 'Sinais vitais', endpoints: ['/vital-signs'] },
  { id: 'symptoms', label: 'Sintomas', endpoints: ['/symptoms'] },
  { id: 'appointments', label: 'Consultas', endpoints: ['/appointments'] },
  { id: 'exams', label: 'Exames', endpoints: ['/exams'] },
  { id: 'prescriptions', label: 'Receitas e documentos anexados', endpoints: ['/prescriptions', '/documents'] },
  { id: 'vaccines', label: 'Vacinas', endpoints: ['/vaccines'] },
  { id: 'alarms', label: 'Alarmes', endpoints: ['/alarms'] },
  { id: 'feminineHealth', label: 'Saúde feminina e gestação', endpoints: ['/feminine-health', '/pregnancy'] },
]

export function getMedicalReportDate(record) {
  const value = record?.date || record?.startDate || record?.localDate || ''
  const date = String(value).slice(0, 10)
  return /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : ''
}

export function filterMedicalReportRecords(records, dateFrom = '', dateTo = '') {
  if (!Array.isArray(records)) throw new Error('Os registros do relatório precisam estar em uma lista.')
  if (dateFrom && dateTo && dateFrom > dateTo) throw new Error('A data inicial precisa ser anterior à data final.')
  if (!dateFrom && !dateTo) return records
  return records.filter((record) => {
    const date = getMedicalReportDate(record)
    return date && (!dateFrom || date >= dateFrom) && (!dateTo || date <= dateTo)
  })
}

function printableValue(value) {
  if (Array.isArray(value)) return value.filter(Boolean).join(', ')
  if (typeof value === 'string' || typeof value === 'number') return String(value).trim()
  return ''
}

function entriesFrom(record, fields) {
  return fields.map(([label, value]) => [label, printableValue(value)]).filter(([, value]) => value)
}

export function getMedicalReportTitle(sectionId, record) {
  if (sectionId === 'vitalSigns') return printableValue(record.type) || 'Sinal vital'
  if (sectionId === 'symptoms') return printableValue(record.symptom || record.name || record.title) || 'Sintoma'
  return printableValue(record.name || record.title || record.label || record.type) || 'Registro'
}

export function getMedicalReportDetails(sectionId, record) {
  if (sectionId === 'medications') {
    const frequency = [record.frequencyType, record.frequencyValue].filter(Boolean).join(' ')
    return entriesFrom(record, [
      ['Dose', record.dosage || record.dose],
      ['Finalidade', record.purpose],
      ['Frequência', frequency],
      ['Horário', record.time],
      ['Situação', record.status],
      ['Observação', record.notes],
    ])
  }
  if (sectionId === 'vitalSigns') {
    const systolic = record.systolic ?? record.sistolica
    const diastolic = record.diastolic ?? record.diastolica
    const value =
      systolic != null && diastolic != null
        ? `${systolic}/${diastolic}${record.unit ? ` ${record.unit}` : ' mmHg'}`
        : [record.value, record.unit].filter((part) => part != null && part !== '').join(' ')
    return entriesFrom(record, [
      ['Valor', value],
      ['Observação', record.notes],
    ])
  }
  if (sectionId === 'symptoms') {
    return entriesFrom(record, [
      ['Tipo', record.type],
      ['Intensidade', record.intensity],
      ['Duração', record.duration],
      ['Fatores', record.factors],
      ['Observação', record.notes],
    ])
  }
  if (sectionId === 'appointments') {
    return entriesFrom(record, [
      ['Profissional', record.professional || record.doctor],
      ['Especialidade', record.specialty || record.detail],
      ['Local', record.location],
      ['Horário', record.time],
      ['Situação', record.status],
      ['Observação', record.notes],
    ])
  }
  if (sectionId === 'exams') {
    return entriesFrom(record, [
      ['Categoria', record.category || record.examType],
      ['Laboratório', record.laboratory],
      ['Situação', record.status],
      ['Arquivo', record.attachment?.name],
      ['Observação', record.notes],
    ])
  }
  if (sectionId === 'prescriptions') {
    return entriesFrom(record, [
      ['Categoria', record.category],
      ['Profissional', record.doctor],
      ['Situação', record.status],
      ['Arquivo', record.attachment?.name],
      ['Observação', record.notes || record.description],
    ])
  }
  if (sectionId === 'vaccines') {
    return entriesFrom(record, [
      ['Dose', record.dose],
      ['Local', record.location],
      ['Situação', record.status],
      ['Observação', record.notes],
    ])
  }
  if (sectionId === 'alarms') {
    return entriesFrom(record, [
      ['Horário', record.time],
      ['Situação', record.status],
      ['Observação', record.notes],
    ])
  }
  return entriesFrom(record, [
    ['Data final', record.endDate],
    ['Fluxo', record.flow],
    ['Sintomas', record.physicalSymptoms],
    ['Humor', record.mood],
    ['Observação', record.notes || record.description],
  ])
}

export function getMedicalReportProfileDetails(profile) {
  return entriesFrom(profile || {}, [
    ['Idade', profile?.age ? `${profile.age} anos` : ''],
    ['Sexo informado', profile?.sex],
    ['Alergias informadas', profile?.allergies],
    ['Condições informadas', profile?.specialConditions || profile?.conditions],
  ])
}
