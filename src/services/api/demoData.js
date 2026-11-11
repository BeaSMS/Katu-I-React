export const demoCredentials = { email: 'demo@katui.app', password: 'katui123' }

const demoCollections = ['medications', 'alarms', 'prescriptions', 'symptoms', 'vital-signs', 'appointments', 'exams', 'feminine-health', 'pregnancy', 'vaccines', 'documents', 'reports']

function dateOffset(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function timeOffset(hours) {
  const date = new Date()
  date.setHours(date.getHours() + hours)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const vitalHistoryDays = [-28, -21, -14, -7, 0]
const demoVitalReadings = {
  ana: {
    'Pressão arterial': [
      [118, 76],
      [121, 78],
      [116, 74],
      [120, 77],
      [119, 76],
    ],
    Glicose: [92, 88, 95, 90, 93],
    Batimentos: [72, 74, 70, 76, 73],
    Saturação: [98, 97, 99, 98, 98],
    Temperatura: [36.6, 36.8, 36.5, 36.7, 36.6],
    Peso: [62, 62.2, 61.9, 62.1, 62],
    Altura: [165, 165, 165, 165, 165],
  },
  lucas: {
    'Pressão arterial': [
      [104, 68],
      [105, 67],
      [102, 66],
      [104, 67],
      [103, 66],
    ],
    Glicose: [88, 91, 87, 90, 89],
    Batimentos: [86, 89, 85, 87, 84],
    Saturação: [98, 97, 98, 99, 98],
    Temperatura: [36.7, 36.6, 36.8, 36.5, 36.7],
    Peso: [31, 31.1, 31.2, 31.3, 31.4],
    Altura: [134, 134, 134, 134, 134],
  },
}

const vitalTypeIds = { 'Pressão arterial': 'pressure', Glicose: 'glucose', Batimentos: 'heart-rate', Saturação: 'oxygen', Temperatura: 'temperature', Peso: 'weight', Altura: 'height' }

function createDemoVitalHistory(profile) {
  return Object.entries(demoVitalReadings[profile]).flatMap(([type, values]) => values.map((value, index) => ({ id: `demo-${profile}-vital-history-${vitalTypeIds[type]}-${index + 1}`, type, ...(type === 'Pressão arterial' ? { systolic: value[0], diastolic: value[1] } : { value }), date: dateOffset(vitalHistoryDays[index]), time: `08:${String(15 + index * 5).padStart(2, '0')}`, notes: 'Registro fictício para demonstração.' })))
}

export function ensureDemoVitalHistory(demo) {
  let changed = false
  for (const profile of ['ana', 'lucas']) {
    const records = demo.recordsByProfile?.[`demo-profile-${profile}`]
    if (!records) continue
    const currentVitals = Array.isArray(records['vital-signs']) ? records['vital-signs'] : []
    if (currentVitals.some((record) => String(record.id || '').startsWith(`demo-${profile}-vital-history-`))) continue
    records['vital-signs'] = [...currentVitals, ...createDemoVitalHistory(profile)]
    changed = true
  }
  return changed
}

function makePdf(title) {
  const safeTitle = title.replace(/[^\x20-\x7E]/g, '').replace(/[()\\]/g, ' ')
  const content = `BT /F1 18 Tf 50 750 Td (${safeTitle}) Tj ET`
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>', '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>', '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>', `<< /Length ${content.length} >>\nstream\n${content}\nendstream`]
  let document = '%PDF-1.4\n'
  const offsets = [0]
  objects.forEach((object, index) => {
    offsets.push(document.length)
    document += `${index + 1} 0 obj\n${object}\nendobj\n`
  })
  const xrefOffset = document.length
  document += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  offsets.slice(1).forEach((offset) => {
    document += `${String(offset).padStart(10, '0')} 00000 n \n`
  })
  document += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`
  return `data:application/pdf;base64,${btoa(document)}`
}

function profilePreferences(darkMode = false) {
  return { darkMode, notificationsEnabled: true, weeklySummaryEnabled: true, accessibility: { textScale: 100, largeText: false, dyslexiaFont: false, highContrast: false, reduceMotion: false }, ai: { provider: 'system', model: '', configured: false, maskedKey: '' } }
}

function attachment(title, name) {
  return { name, contentType: 'application/pdf', size: 640, dataUrl: makePdf(title) }
}

export function createDemoData() {
  const today = dateOffset(0)
  const previousDay = dateOffset(-2)
  const nextDay = dateOffset(2)
  const [firstReminderTime, secondReminderTime] = [timeOffset(2), timeOffset(4)]
  const profiles = [
    { id: 'demo-profile-ana', name: 'Ana Costa', relation: 'Perfil principal', type: 'principal', age: 34, sex: 'feminino', weight: 62, heightCm: 165, allergies: 'Dipirona', specialConditions: 'Enxaqueca', themeColor: 'turquoise', preferences: profilePreferences() },
    { id: 'demo-profile-lucas', name: 'Lucas Costa', relation: 'Filho', type: 'dependente', age: 9, sex: 'masculino', weight: 31, heightCm: 134, allergies: 'Nenhuma conhecida', specialConditions: 'Asma leve', themeColor: 'sky', preferences: profilePreferences(true) },
  ]
  const anaRecords = Object.fromEntries(demoCollections.map((collection) => [collection, []]))
  Object.assign(anaRecords, {
    medications: [
      { id: 'demo-ana-med-1', name: 'Vitamina D', dosage: '1 cápsula', frequencyType: 'Uma vez ao dia', frequencyValue: '1', date: today, startDate: today, time: firstReminderTime, durationDays: '30', continuous: true, stock: '18', generateAlarms: true, status: 'Ativo' },
      { id: 'demo-ana-med-2', name: 'Magnésio', dosage: '1 comprimido', frequencyType: 'Uma vez ao dia', frequencyValue: '1', date: today, startDate: today, time: secondReminderTime, continuous: true, stock: '12', status: 'Ativo' },
    ],
    alarms: [{ id: 'demo-ana-alarm-1', name: 'Consulta de rotina', title: 'Lembrete de consulta', date: nextDay, time: '10:30', status: 'Ativo' }],
    prescriptions: [{ id: 'demo-ana-prescription-1', title: 'Prescrição - Dra. Helena', name: 'Prescrição - Dra. Helena', date: today, doctor: 'Dra. Helena Martins', notes: 'Uso diário conforme orientação médica.', status: 'Ativa', attachment: attachment('Prescricao ficticia - Ana Costa', 'prescricao-ana.pdf') }],
    symptoms: [
      { id: 'demo-ana-symptom-1', title: 'Dor de cabeça leve', symptom: 'Dor de cabeça', intensity: '3', date: today, time: '09:15', notes: 'Melhorou após descansar.', status: 'Registrado' },
      { id: 'demo-ana-symptom-2', title: 'Boa disposição', symptom: 'Disposição', intensity: '1', date: previousDay, time: '08:40', status: 'Registrado' },
    ],
    'vital-signs': createDemoVitalHistory('ana'),
    appointments: [
      { id: 'demo-ana-appointment-1', title: 'Consulta de rotina', name: 'Consulta de rotina', date: nextDay, time: '10:30', professional: 'Dra. Helena Martins', location: 'Clínica Bem Viver', status: 'Agendada' },
      { id: 'demo-ana-appointment-2', title: 'Retorno com nutricionista', name: 'Retorno com nutricionista', date: today, time: '15:00', professional: 'Marina Lopes', status: 'Agendada' },
    ],
    exams: [{ id: 'demo-ana-exam-1', title: 'Hemograma completo', name: 'Hemograma completo', date: today, laboratory: 'Laboratório Central', notes: 'Resultado fictício para demonstração.', status: 'Disponível', attachment: attachment('Resultado ficticio - hemograma', 'hemograma-ana.pdf') }],
    documents: [
      { id: 'demo-ana-document-1', title: 'Resultado de hemograma', name: 'Resultado de hemograma', date: today, description: 'Exame realizado no Laboratório Central.', category: 'Exame', attachment: attachment('Documento ficticio - Ana Costa', 'resultado-hemograma.pdf') },
      { id: 'demo-ana-document-2', title: 'Cartão de vacinação', name: 'Cartão de vacinação', date: previousDay, description: 'Cópia digital do cartão.', category: 'Vacinação', attachment: attachment('Cartao de vacinacao ficticio', 'cartao-vacinacao.pdf') },
    ],
    vaccines: [
      { id: 'demo-ana-vaccine-1', title: 'Influenza', name: 'Influenza', date: today, dose: 'Dose anual', location: 'UBS Central', status: 'Aplicada' },
      { id: 'demo-ana-vaccine-2', title: 'COVID-19', name: 'COVID-19', date: dateOffset(-90), dose: 'Reforço', status: 'Aplicada' },
    ],
    'feminine-health': [{ id: 'demo-ana-cycle-1', title: 'Início do ciclo', name: 'Menstruação', date: today, type: 'menstruation', notes: 'Registro fictício de ciclo.' }],
    pregnancy: [],
    reports: [{ id: 'demo-ana-report-1', title: 'Resumo de saúde', name: 'Resumo de saúde', date: today, description: 'Exemplo de relatório para demonstração.', status: 'Pronto', attachment: attachment('Resumo de saude ficticio', 'resumo-saude-ana.pdf') }],
  })

  const lucasRecords = Object.fromEntries(demoCollections.map((collection) => [collection, []]))
  Object.assign(lucasRecords, {
    medications: [{ id: 'demo-lucas-med-1', name: 'Xarope respiratório', dosage: '5 ml', frequencyType: 'A cada intervalo de horas', frequencyValue: '8', date: today, startDate: today, time: firstReminderTime, durationDays: '5', continuous: false, stock: '1 frasco', generateAlarms: true, status: 'Ativo' }],
    alarms: [{ id: 'demo-lucas-alarm-1', name: 'Lembrete de hidratação', title: 'Beber água', date: today, time: secondReminderTime, status: 'Ativo' }],
    prescriptions: [{ id: 'demo-lucas-prescription-1', title: 'Receita pediátrica', name: 'Receita pediátrica', date: today, doctor: 'Dr. Paulo Nunes', notes: 'Exemplo fictício de receita pediátrica.', status: 'Ativa', attachment: attachment('Prescricao pediatrica ficticia', 'prescricao-lucas.pdf') }],
    symptoms: [{ id: 'demo-lucas-symptom-1', title: 'Tosse leve', symptom: 'Tosse', intensity: '2', date: today, time: '07:30', notes: 'Sem febre.', status: 'Registrado' }],
    'vital-signs': createDemoVitalHistory('lucas'),
    appointments: [{ id: 'demo-lucas-appointment-1', title: 'Retorno pediátrico', name: 'Retorno pediátrico', date: nextDay, time: '14:00', professional: 'Dr. Paulo Nunes', location: 'Clínica da Criança', status: 'Agendada' }],
    exams: [{ id: 'demo-lucas-exam-1', title: 'Avaliação respiratória', name: 'Avaliação respiratória', date: today, notes: 'Registro fictício para demonstração.', status: 'Disponível', attachment: attachment('Avaliacao respiratoria ficticia', 'avaliacao-lucas.pdf') }],
    documents: [{ id: 'demo-lucas-document-1', title: 'Relatório pediátrico', name: 'Relatório pediátrico', date: today, description: 'Documento de exemplo do perfil Lucas.', category: 'Relatório', attachment: attachment('Relatorio pediatrico ficticio', 'relatorio-lucas.pdf') }],
    vaccines: [{ id: 'demo-lucas-vaccine-1', title: 'Influenza', name: 'Influenza', date: today, dose: 'Dose anual', location: 'UBS Central', status: 'Aplicada' }],
    'feminine-health': [],
    pregnancy: [],
    reports: [],
  })

  return { profiles, activeProfileId: profiles[0].id, recordsByProfile: { [profiles[0].id]: anaRecords, [profiles[1].id]: lucasRecords } }
}
