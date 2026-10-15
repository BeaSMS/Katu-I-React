import { useEffect, useRef, useState } from 'react'
import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from './services/api'
import { disableBackgroundPush, enableBackgroundPush, isDesktopShell, isPushConfigured, listBackgroundAlarms, removeMedicationReminders, removeSingleReminder, scheduleMedicationReminders, scheduleRestoredAlarm, scheduleSingleReminder } from './services/push'
import { dateKeyForFemaleHealth, getCycleDayKind, getCycleInsights } from './services/femaleHealth'
import { getContraceptiveDay, getContraceptivePattern, getContraceptivePatternDescription } from './services/contraceptive'
import { moduleNavigation, supportNavigation, getResourceForLabel } from './services/resources'
import { clearSecureStorage, flushSecureStorage, secureStorage } from './services/secureStorage'

const medications = []
const defaultOnboardingForm = { name: '', age: '', sex: 'nao-informado', weight: '', height: '', allergies: '', conditions: '', aiEnabled: false }
const defaultAccessibility = { textScale: 100, largeText: false, dyslexiaFont: false, highContrast: false, reduceMotion: false }
const defaultProfilePreferences = { darkMode: false, notificationsEnabled: true, weeklySummaryEnabled: true, accessibility: defaultAccessibility, ai: { provider: 'system', model: '', configured: false, maskedKey: '' } }
const guestCollections = ['medications', 'alarms', 'prescriptions', 'symptoms', 'vital-signs', 'appointments', 'exams', 'feminine-health', 'pregnancy', 'vaccines', 'documents', 'reports']
const emptyGuestData = () => Object.fromEntries(guestCollections.map((collection) => [collection, []]))
const profileThemeOptions = [
  { id: 'turquoise', label: 'Turquesa', color: '#009C9C', soft: '#E2F5F3', dark: '#66C8C5', ink: '#FFFFFF' },
  { id: 'gold', label: 'Dourado', color: '#D89A27', soft: '#FFF1CD', dark: '#F0B94F', ink: '#4A3510' },
  { id: 'sky', label: 'Azul', color: '#2F8FB0', soft: '#E5F3F8', dark: '#80C5D9', ink: '#FFFFFF' },
  { id: 'purple', label: 'Violeta', color: '#7054A5', soft: '#EEE9F7', dark: '#B8A5D7', ink: '#FFFFFF' },
  { id: 'rose', label: 'Rosa', color: '#C66B88', soft: '#F9E8EE', dark: '#E4A6B9', ink: '#FFFFFF' },
  { id: 'green', label: 'Verde', color: '#4A9A73', soft: '#E6F4ED', dark: '#91D1B2', ink: '#FFFFFF' },
]
const getProfileTheme = (profile) => profileThemeOptions.find((option) => option.id === profile?.themeColor || option.color.toLowerCase() === String(profile?.corHex || '').toLowerCase()) || profileThemeOptions[0]

function getDueLocalReminders(data, now = Date.now()) {
  const reminders = []
  const alarms = Array.isArray(data.alarms) ? data.alarms : []
  alarms.forEach((alarm) => {
    if (!alarm.date || !alarm.time) return
    const dueAt = new Date(`${alarm.date}T${alarm.time}`).getTime()
    if (dueAt <= now && now - dueAt < 30_000) reminders.push({ key: `alarm:${alarm.id || dueAt}`, title: 'Lembrete de saúde', body: 'Você tem um cuidado agendado no Katu’I.' })
  })
  const medications = Array.isArray(data.medications) ? data.medications : []
  medications.forEach((medication) => {
    if (!medication.generateAlarms || !medication.date || !medication.time) return
    const frequency = Number(medication.frequencyValue)
    if (!Number.isInteger(frequency) || frequency < 1 || frequency > 24) return
    const intervalMinutes = String(medication.frequencyType || '').toLowerCase().includes('vezes')
      ? Math.round(1440 / frequency)
      : frequency * 60
    const startAt = new Date(`${medication.date}T${medication.time}`).getTime()
    const interval = intervalMinutes * 60_000
    if (!Number.isFinite(startAt) || startAt > now) return
    const elapsed = Math.floor((now - startAt) / interval)
    const dueAt = startAt + elapsed * interval
    const endAt = medication.continuous ? Infinity : startAt + Number(medication.durationDays || 0) * 86_400_000
    if (dueAt <= now && now - dueAt < 30_000 && dueAt < endAt) {
      reminders.push({ key: `medication:${medication.id || medication.name}:${dueAt}`, title: 'Hora do seu medicamento', body: 'Abra o Katu’I para conferir seu lembrete.' })
    }
  })
  return reminders
}

function getAlarmTimestamp(alarm) {
  const directTimestamp = alarm.nextAt || alarm.startAt
  if (directTimestamp) {
    const timestamp = new Date(directTimestamp).getTime()
    if (Number.isFinite(timestamp)) return timestamp
  }
  if (!alarm.date || !alarm.time) return null
  const timestamp = new Date(`${alarm.date}T${alarm.time}`).getTime()
  return Number.isFinite(timestamp) ? timestamp : null
}

function getLocalMedicationAlarm(medication, now = Date.now()) {
  if (!medication.generateAlarms || !medication.date || !medication.time) return null
  const frequency = Number(medication.frequencyValue)
  if (!Number.isInteger(frequency) || frequency < 1 || frequency > 24) return null
  const intervalMinutes = String(medication.frequencyType || '').toLowerCase().includes('vezes')
    ? Math.round(1440 / frequency)
    : frequency * 60
  const interval = intervalMinutes * 60_000
  const startAt = new Date(`${medication.date}T${medication.time}`).getTime()
  if (!Number.isFinite(startAt)) return null
  const elapsedIntervals = startAt > now ? 0 : Math.floor((now - startAt) / interval) + 1
  const nextAt = startAt + elapsedIntervals * interval
  const endAt = medication.continuous ? Infinity : startAt + Number(medication.durationDays || 0) * 86_400_000
  if (nextAt >= endAt) return null
  return {
    id: `medication:${medication.id || medication.name}`,
    name: `Hora do medicamento${medication.name ? ` · ${medication.name}` : ''}`,
    title: 'Lembrete de medicamento',
    detail: `A cada ${intervalMinutes % 60 === 0 ? `${intervalMinutes / 60} hora(s)` : `${intervalMinutes} minuto(s)`}`,
    nextAt: new Date(nextAt).toISOString(),
    sourceKind: 'medication',
  }
}

function Icon({ name, size = 20 }) {
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' }
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    pill: <><path d="M9 3h6v3h3a2 2 0 0 1 2 2v13H4V8a2 2 0 0 1 2-2h3V3Z"/><path d="M9 3h6M8 12h8M12 9v6"/></>,
    spark: <><path d="m12 3-1.4 5.6L5 10l5.6 1.4L12 17l1.4-5.6L19 10l-5.6-1.4Z"/><path d="m19 16-.7 2.3L16 19l2.3.7L19 22l.7-2.3L22 19l-2.3-.7Z"/></>,
    heart: <><path d="M20.8 8.8c0 5.5-8.8 10.4-8.8 10.4S3.2 14.3 3.2 8.8A4.8 4.8 0 0 1 12 6a4.8 4.8 0 0 1 8.8 2.8Z"/></>,
    profile: <><circle cx="12" cy="8" r="3.5"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/></>,
    pulse: <><path d="M3 12h4l2-7 4 14 2-7h6"/><circle cx="12" cy="12" r="9" opacity=".18"/></>,
    calendar: <><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M16 2v4M8 2v4M3 9h18M8 13h2M14 13h2M8 17h2"/></>,
    file: <><path d="M6 2h8l4 4v16H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z"/><path d="M14 2v5h5M8 12h8M8 16h6"/></>,
    bell: <><path d="M6 9a6 6 0 0 1 12 0c0 7 3 7 3 9H3c0-2 3-2 3-9Z"/><path d="M10 21h4"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    check: <path d="m5 12 4 4L19 6"/>,
    location: <><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    trend: <><path d="M4 17 10 11l4 4 6-7"/><path d="M15 8h5v5"/></>,
    sun: <><circle cx="12" cy="12" r="3.5"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4"/></>,
    cloud: <><path d="M7 18h10a4 4 0 0 0 .5-8A5.5 5.5 0 0 0 7 8.5 4.5 4.5 0 0 0 7 18Z"/></>,
    brain: <><path d="M9 4a3 3 0 0 0-3 3v1a3 3 0 0 0-1 5 3 3 0 0 0 3 5h1V4Z"/><path d="M15 4a3 3 0 0 1 3 3v1a3 3 0 0 1 1 5 3 3 0 0 1-3 5h-1V4ZM9 8h6M9 12h6M9 16h6"/></>,
    sad: <><circle cx="12" cy="12" r="9"/><path d="M8 9h.01M16 9h.01M8.5 16a5 5 0 0 1 7 0"/></>,
    battery: <><rect x="3" y="7" width="17" height="10" rx="2"/><path d="M21 10v4M6 10v4M10 10v4"/></>,
    bolt: <path d="m13 2-8 12h6l-1 8 8-12h-6l1-8Z"/>,
    help: <><circle cx="12" cy="12" r="9"/><path d="M9.7 9a2.4 2.4 0 1 1 4.1 1.7c-.9.8-1.8 1.2-1.8 2.5M12 17h.01"/></>,
    sliders: <><path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/></>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.8 1.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-2.5V20a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1-1.8-1.8.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.6-1H6v-2.5h.2a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 1.8-1.8.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.6V5h2.5v.2a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1 1.8 1.8-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v2.5h-.2a1.7 1.7 0 0 0-1.6 1Z"/></>,
    droplet: <><path d="M12 3c-2.6 4-6.5 7.4-6.5 11.2a6.5 6.5 0 0 0 13 0C18.5 10.4 14.6 7 12 3Z"/><path d="M9 15a3 3 0 0 0 3 3"/></>,
    thermometer: <><path d="M14 14.8V5a3 3 0 0 0-6 0v9.8a5 5 0 1 0 6 0Z"/><path d="M11 11v7"/></>,
    scale: <><path d="M5 4h14l2 16H3L5 4Z"/><path d="m12 8 2.5 3M8 15h8"/></>,
    ruler: <><path d="M4 7h16v10H4z"/><path d="M8 7v4M12 7v3M16 7v4"/></>,
  }
  return <svg {...common}>{paths[name]}</svg>
}

function Brand() {
  return <div className="brand"><img className="brand-logo" src={`${import.meta.env.BASE_URL}katui-logo.png`} alt="Katu'I"/><small>cuidado que acompanha</small></div>
}

function EmptyState({ icon = 'file', title = 'Ainda não há registros', message = 'Quando você adicionar informações, elas aparecerão aqui.', action, onAction }) {
  return <div className="empty-state"><div className="empty-state-icon"><Icon name={icon} size={23}/></div><strong>{title}</strong><p>{message}</p>{action && <button className="soft-btn" onClick={onAction}><Icon name="plus" size={14}/> {action}</button>}</div>
}

function localDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function readGuestProfile() {
  try {
    const profile = JSON.parse(secureStorage.getItem('katui.guest-profile') || 'null')
    return profile && typeof profile === 'object' ? profile : null
  } catch {
    return null
  }
}

function parseDateKey(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : null
}

function recordDate(record) {
  const value = record?.date || record?.dateTime || record?.datetime || record?.recordedAt || record?.recorded_at || record?.createdAt || record?.created_at || record?.dataHoraRegistro || record?.dataHora || record?.dataRegistro || record?.horario || record?.startDate || record?.dataInicio
  return String(value || '').slice(0, 10)
}

function recordOccursOnDate(record, date, resource) {
  if (!record || !date) return false
  if (resource === 'alarms' && record.nextAt) {
    const nextOccurrence = new Date(record.nextAt)
    return Number.isFinite(nextOccurrence.getTime()) && localDateKey(nextOccurrence) === date
  }
  if (resource === 'medications') {
    const start = String(record.startDate || record.dataInicio || record.date || recordDate(record) || '').slice(0, 10)
    const end = String(record.endDate || record.dataFim || '').slice(0, 10)
    // Undated legacy/demo treatments remain visible for today, not for every historical day.
    return start || end ? (!start || date >= start) && (!end || date <= end) : date === localDateKey(new Date())
  }
  const recordDay = recordDate(record)
  return recordDay === date
}

function hasCalendarRecordOnDate(records, date, resource) {
  return records.some((record) => {
    const recordResource = record.__calendarResource || resource
    if (recordResource === 'medications') {
      const start = String(record.startDate || record.dataInicio || record.date || recordDate(record) || '').slice(0, 10)
      const end = String(record.endDate || record.dataFim || '').slice(0, 10)
      return start || end ? (!start || date >= start) && (!end || date <= end) : date === localDateKey(new Date())
    }
    return recordDate(record) === date
  })
}

const calendarWeekdays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
const calendarMonths = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']
const calendarResources = [
  { resource: 'medications', endpoint: '/medications', label: 'Medicamentos' },
  { resource: 'alarms', endpoint: '/alarms', label: 'Alarmes' },
  { resource: 'prescriptions', endpoint: '/prescriptions', label: 'Receitas' },
  { resource: 'symptoms', endpoint: '/symptoms', label: 'Sintomas' },
  { resource: 'vitalSigns', endpoint: '/vital-signs', label: 'Sinais vitais' },
  { resource: 'appointments', endpoint: '/appointments', label: 'Consultas' },
  { resource: 'exams', endpoint: '/exams', label: 'Exames' },
]

function CalendarCard({ selectedDate, onDateSelect, records = [], resource = '', title = 'Calendário', expanded: expandedProp, onExpandedChange, cardRef, loadError = '' }) {
  const [internalExpanded, setInternalExpanded] = useState(false)
  const expanded = expandedProp ?? internalExpanded
  const setExpanded = (value) => {
    const nextExpanded = typeof value === 'function' ? value(expanded) : value
    if (onExpandedChange) onExpandedChange(nextExpanded)
    else setInternalExpanded(nextExpanded)
  }
  const selected = parseDateKey(selectedDate) || new Date()
  const monthStart = new Date(selected.getFullYear(), selected.getMonth(), 1)
  const daysInMonth = new Date(selected.getFullYear(), selected.getMonth() + 1, 0).getDate()
  const selectedMonth = calendarMonths[selected.getMonth()]
  const selectedMonthLabel = `${selectedMonth.charAt(0).toUpperCase()}${selectedMonth.slice(1)}`
  const weekStart = new Date(selected)
  weekStart.setDate(selected.getDate() - selected.getDay())
  const shiftMonth = (amount) => {
    const target = new Date(selected.getFullYear(), selected.getMonth() + amount, 1)
    target.setDate(Math.min(selected.getDate(), new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()))
    onDateSelect(localDateKey(target))
  }
  const dayButton = (date, compact = false) => {
    const key = localDateKey(date)
    const isSelected = key === selectedDate
    const types = [...new Set(records.filter((record) => hasCalendarRecordOnDate([record], key, resource)).map((record) => record.__calendarResource || resource).filter(Boolean))]
    const hasAlarm = types.includes('alarms')
    const hasAgenda = types.includes('appointments') || types.includes('exams')
    const activityLabel = types.length ? `, registros: ${types.map((type) => calendarResources.find((item) => item.resource === type)?.label || type).join(', ')}` : ''
    const scheduleLabel = `${hasAlarm ? ', alarme configurado' : ''}${hasAgenda ? ', item agendado' : ''}`
    return <button key={key} type="button" className={`calendar-day${compact ? ' compact' : ''}${isSelected ? ' selected' : ''}`} aria-pressed={isSelected} aria-label={`${calendarWeekdays[date.getDay()]}, ${date.getDate()} ${calendarMonths[date.getMonth()]}${activityLabel}${scheduleLabel}`} onClick={() => onDateSelect(key)}>
      {(hasAlarm || hasAgenda) && <span className="calendar-day-badges" aria-hidden="true">{hasAlarm && <span className="calendar-day-badge alarm" title="Alarme configurado"><Icon name="clock" size={10}/></span>}{hasAgenda && <span className="calendar-day-badge agenda" title="Consulta ou exame agendado"><Icon name="calendar" size={10}/></span>}</span>}
      <span className="calendar-day-name">{calendarWeekdays[date.getDay()]}</span>
      <strong>{date.getDate()}</strong>
      {types.length > 0 && <span className="calendar-day-dots" aria-hidden="true">{types.map((type) => <i key={type} className={`calendar-day-dot ${type}`} />)}</span>}
    </button>
  }
  const monthDays = Array.from({ length: monthStart.getDay() + daysInMonth }, (_, index) => {
    const day = index - monthStart.getDay() + 1
    return day > 0 ? new Date(selected.getFullYear(), selected.getMonth(), day) : null
  })
  return <section ref={cardRef} className={`calendar-card${expanded ? ' expanded' : ''}`} aria-label={`${title}, ${calendarMonths[selected.getMonth()]} de ${selected.getFullYear()}`}>
    <div className="calendar-card-header">
      <div className="calendar-month-label">
        <Icon name="calendar" size={18}/>
        <span>{title}<strong>Vendo dados de {selected.getDate()} de {selectedMonthLabel} de {selected.getFullYear()}</strong></span>
      </div>
      <div className="calendar-controls">
        <button type="button" aria-label="Mês anterior" onClick={() => shiftMonth(-1)}><Icon name="chevron" size={16}/></button>
        <button type="button" aria-label="Próximo mês" onClick={() => shiftMonth(1)}><Icon name="chevron" size={16}/></button>
      </div>
    </div>
    {!expanded ? <div className="calendar-week">{Array.from({ length: 7 }, (_, offset) => { const date = new Date(weekStart); date.setDate(weekStart.getDate() + offset); return dayButton(date, true) })}</div> : <>
      <div className="calendar-weekday-headings">{calendarWeekdays.map((day) => <span key={day}>{day}</span>)}</div>
      <div className="calendar-month-grid">{monthDays.map((date, index) => date ? dayButton(date) : <span key={`blank-${index}`} aria-hidden="true"/>)}</div>
    </>}
    <button className="calendar-selection" type="button" aria-expanded={expanded} onClick={() => setExpanded((value) => !value)}>
      <strong>{expanded ? 'Recolher calendário' : 'Ver mês completo'}</strong>
      <Icon name="chevron" size={16}/>
    </button>
    {loadError && <p className="calendar-load-error" role="status">{loadError}</p>}
  </section>
}

function ModulePage({ active, onAction, darkMode, onToggleDarkMode, profile, onPregnancyChange, selectedDate, onDateSelect, calendarRecords, calendarLoadError, isGuest, usePushAlarms }) {
  const pageData = {
    'Medicamentos': { eyebrow: 'CUIDADO CONTÍNUO', title: 'Medicamentos e estoque', description: 'Organize seus tratamentos e acompanhe o que precisa ser reposto.', icon: 'pill', action: 'Adicionar medicamento' },
    'Sinais vitais': { eyebrow: 'ACOMPANHAMENTO', title: 'Sinais vitais', description: 'Veja a evolução dos seus registros e acompanhe seus indicadores.', icon: 'pulse', action: 'Novo registro' },
    'Consultas': { eyebrow: 'SUA AGENDA', title: 'Consultas', description: 'Tenha seus próximos compromissos de saúde sempre à mão.', icon: 'calendar', action: 'Agendar consulta' },
    'Documentos': { eyebrow: 'SEUS ARQUIVOS', title: 'Documentos de saúde', description: 'Receitas, exames e relatórios organizados em um só lugar.', icon: 'file', action: 'Adicionar documento' },
    'Alarmes': { eyebrow: 'LEMBRETES INTELIGENTES', title: 'Alarmes', description: 'Configure lembretes para medicamentos, consultas e cuidados importantes.', icon: 'bell', action: 'Criar alarme' },
    'Receitas': { eyebrow: 'ORGANIZAÇÃO INTELIGENTE', title: 'Receitas', description: isGuest ? 'Organize suas prescrições manualmente neste dispositivo.' : 'Organize suas prescrições e, quando quiser, use a leitura inteligente para revisar os dados.', icon: 'file', action: 'Ler receita com IA' },
    'Exames': { eyebrow: 'SEUS RESULTADOS', title: 'Exames', description: 'Guarde resultados e acompanhe a evolução dos seus exames de saúde.', icon: 'file', action: 'Adicionar exame' },
    'Perfis e dependentes': { eyebrow: 'CUIDADO EM FAMÍLIA', title: 'Perfis e dependentes', description: 'Gerencie filhos, idosos e outras pessoas que dependem do seu cuidado.', icon: 'profile', action: 'Adicionar perfil' },
    'Backup pessoal': { eyebrow: 'SEUS DADOS', title: 'Backup pessoal', description: 'Exporte, restaure e acompanhe a segurança dos seus dados de saúde.', icon: 'file', action: 'Criar backup' },
    'Diário de sintomas': { eyebrow: 'COMO VOCÊ ESTÁ?', title: 'Diário de sintomas', description: 'Registre seus sintomas e ajude a identificar padrões ao longo do tempo.', icon: 'pulse', action: 'Registrar sintoma' },
    'Saúde feminina': { eyebrow: 'CUIDADO ÍNTIMO', title: 'Saúde feminina', description: 'Acompanhe seu ciclo, sintomas e lembretes com privacidade.', icon: 'calendar', action: null },
    'Carteira de vacinação': { eyebrow: 'PROTEÇÃO EM DIA', title: 'Carteira de vacinação', description: 'Confira suas vacinas e os próximos reforços recomendados.', icon: 'file', action: 'Adicionar vacina' },
    'Configurações': { eyebrow: 'SEU ESPAÇO', title: 'Configurações', description: 'Ajuste sua experiência no Katu’I e suas preferências de acessibilidade.', icon: 'settings', action: null },
  }
  const data = pageData[active] || pageData['Medicamentos']
  if (active === 'Configurações') return <SettingsPage darkMode={darkMode} onToggleDarkMode={onToggleDarkMode} />
  return <>
    <section className="page-title-row"><div><p className="eyebrow">{data.eyebrow}</p><h1>{data.title}</h1><p className="page-description">{data.description}</p></div>{data.action && <button className="primary-btn" onClick={() => onAction(active === 'Receitas' && !isGuest ? { kind: 'ai', title: data.action, endpoint: '/ai/prescription' } : active === 'Receitas' ? { kind: 'form', title: 'Adicionar receita', endpoint: '/prescriptions' } : active === 'Sinais vitais' ? { kind: 'form', title: data.action, endpoint: '/vital-signs', vitalType: '' } : { kind: 'form', title: data.action, endpoint: getResourceForLabel(active)?.endpoint || `/${active.toLowerCase().replaceAll(' ', '-')}` })}><Icon name={active === 'Receitas' && !isGuest ? 'spark' : 'plus'} size={16}/> {active === 'Receitas' && isGuest ? 'Adicionar receita' : data.action}</button>}</section>
    {active === 'Saúde feminina' ? <FemaleHealthPage profile={profile} onPregnancyChange={onPregnancyChange} onAction={onAction} selectedDate={selectedDate} /> : <ResourcePage active={active} icon={data.icon} endpoint={getResourceForLabel(active)?.endpoint} resource={getResourceForLabel(active)?.resource || ''} selectedDate={selectedDate} onDateSelect={onDateSelect} calendarRecords={calendarRecords} calendarLoadError={calendarLoadError} onAction={onAction} usePushAlarms={active === 'Alarmes' && usePushAlarms} />}
  </>
}

function MedicationPage({ onAction }) {
  return <div className="module-stack"><div className="summary-strip"><div><span>Tratamentos ativos</span><strong>0 medicamentos</strong></div><div><span>Próximo lembrete</span><strong>Nenhum agendado</strong></div><div><span>Estoque geral</span><strong className="good-text">Sem dados</strong></div></div><div className="module-grid"><div className="panel wide-panel"><div className="panel-header"><div><h3>Meus medicamentos</h3><p>Seus medicamentos aparecerão aqui após o primeiro cadastro.</p></div></div><EmptyState icon="pill" title="Nenhum medicamento cadastrado" message="Adicione seus medicamentos para controlar doses, horários e estoque." action="Adicionar medicamento" onAction={() => onAction({ kind: 'form', title: 'Adicionar medicamento', endpoint: '/medications' })}/></div><div className="panel stock-panel"><div className="panel-header"><div><h3>Estoque</h3><p>Acompanhamento de reposição</p></div></div><EmptyState icon="pill" title="Estoque vazio" message="O estoque será calculado quando você cadastrar um medicamento." /></div></div></div>
}

function VitalsPage({ onAction }) {
  const openVital = (vitalType) => onAction({ kind: 'form', title: `Registrar ${vitalType.toLowerCase()}`, endpoint: '/vital-signs', vitalType })
  return <div className="module-grid vitals-layout"><div className="panel wide-panel"><div className="panel-header"><div><h3>Histórico de sinais vitais</h3><p>Seus registros aparecerão aqui após o primeiro lançamento.</p></div></div><EmptyState icon="pulse" title="Nenhum sinal vital registrado" message="Registre pressão, temperatura ou glicemia para começar a acompanhar sua evolução." action="Fazer primeiro registro" onAction={() => openVital('')}/></div><div className="panel quick-panel"><h3>Registrar agora</h3><p>Escolha um indicador para começar seu histórico.</p><button className="quick-item" onClick={() => openVital('Pressão arterial')}><span className="quick-icon orange">♥</span><span><strong>Pressão arterial</strong><small>Adicionar primeira medição</small></span><Icon name="chevron" size={16}/></button><button className="quick-item" onClick={() => openVital('Temperatura')}><span className="quick-icon blue">°</span><span><strong>Temperatura</strong><small>Adicionar primeira medição</small></span><Icon name="chevron" size={16}/></button><button className="quick-item" onClick={() => openVital('Glicose')}><span className="quick-icon rose">◌</span><span><strong>Glicose</strong><small>Adicionar primeira medição</small></span><Icon name="chevron" size={16}/></button><button className="quick-item" onClick={() => openVital('Peso')}><span className="quick-icon teal">⚖</span><span><strong>Peso</strong><small>Adicionar primeira medição</small></span><Icon name="chevron" size={16}/></button></div></div>
}

function QuickAddMenu({ onAction, femaleProfile, isGuest }) {
  const [submenu, setSubmenu] = useState(null)
  const close = () => setSubmenu(null)
  const action = (payload) => { close(); onAction(payload) }
  return <div className="quick-add-menu" role="menu">
    <button className="quick-add-item" onClick={() => action({ kind: 'form', title: 'Registrar sintoma', endpoint: '/symptoms' })}><span className="quick-icon purple">✦</span><strong>Sintoma</strong><Icon name="chevron" size={15}/></button>
    <button className="quick-add-item" onClick={() => action({ kind: 'form', title: 'Adicionar medicamento', endpoint: '/medications' })}><span className="quick-icon teal">＋</span><strong>Remédio</strong><Icon name="chevron" size={15}/></button>
    <div className="quick-add-group"><button className="quick-add-item" aria-expanded={submenu === 'vitals'} onClick={() => setSubmenu(submenu === 'vitals' ? null : 'vitals')}><span className="quick-icon rose">♥</span><strong>Sinais vitais</strong><Icon name="chevron" size={15}/></button>{submenu === 'vitals' && <div className="quick-add-submenu">{[['Glicose','Glicose'],['Pressão arterial','Pressão arterial'],['Peso','Peso'],['Temperatura','Temperatura'],['Batimentos','Batimentos'],['Saturação','Saturação'],['Altura','Altura']].map(([label, vitalType]) => <button key={label} onClick={() => action({ kind: 'form', title: `Registrar ${label.toLowerCase()}`, endpoint: '/vital-signs', vitalType })}>{label}</button>)}</div>}</div>
    <button className="quick-add-item" onClick={() => action({ kind: 'form', title: 'Agendar consulta', endpoint: '/appointments' })}><span className="quick-icon blue">⌁</span><strong>Consulta</strong><Icon name="chevron" size={15}/></button>
    <button className="quick-add-item" onClick={() => action({ kind: 'form', title: 'Adicionar exame', endpoint: '/exams' })}><span className="quick-icon pink">□</span><strong>Exame</strong><Icon name="chevron" size={15}/></button>
    {femaleProfile && <div className="quick-add-group"><button className="quick-add-item" aria-expanded={submenu === 'female'} onClick={() => setSubmenu(submenu === 'female' ? null : 'female')}><span className="quick-icon magenta">♡</span><strong>Saúde feminina</strong><Icon name="chevron" size={15}/></button>{submenu === 'female' && <div className="quick-add-submenu"><button onClick={() => action({ kind: 'form', title: 'Início de ciclo', endpoint: '/feminine-health' })}>Início de ciclo</button><button onClick={() => action({ kind: 'form', title: 'Sintomas e fluxo', endpoint: '/feminine-health' })}>Sintomas e fluxo</button></div>}</div>}
    <button className="quick-add-item" onClick={() => action({ kind: 'form', title: 'Adicionar vacina', endpoint: '/vaccines' })}><span className="quick-icon green">✓</span><strong>Vacina</strong><Icon name="chevron" size={15}/></button>
    <button className="quick-add-item" onClick={() => action(isGuest ? { kind: 'form', title: 'Adicionar receita', endpoint: '/prescriptions' } : { kind: 'ai', title: 'Ler receita com IA', endpoint: '/ai/prescription' })}><span className="quick-icon gold">✦</span><strong>Receita</strong><Icon name="chevron" size={15}/></button>
  </div>
}

function AppointmentsPage({ onAction }) {
  return <div className="module-grid appointments-layout"><div className="panel wide-panel"><div className="panel-header"><div><h3>Próximos compromissos</h3><p>Seus compromissos aparecerão aqui.</p></div></div><EmptyState icon="calendar" title="Nenhuma consulta agendada" message="Adicione sua primeira consulta ou exame para organizar sua agenda de saúde." action="Agendar consulta" onAction={() => onAction({ kind: 'form', title: 'Agendar consulta', endpoint: '/appointments' })}/></div><div className="panel reminder-panel"><h3>Lembretes</h3><p>Seus lembretes aparecerão aqui.</p><EmptyState icon="bell" title="Sem lembretes" message="Nenhum lembrete foi configurado." /></div></div>
}

function DocumentsPage({ onAction }) {
  return <div className="module-grid documents-layout"><div className="panel wide-panel"><div className="panel-header"><div><h3>Arquivos recentes</h3><p>Seus documentos aparecerão aqui depois do primeiro envio.</p></div></div><EmptyState icon="file" title="Nenhum documento adicionado" message="Envie receitas, exames ou outros arquivos para mantê-los organizados." action="Adicionar documento" onAction={() => onAction({ kind: 'form', title: 'Adicionar documento', endpoint: '/documents' })}/></div><div className="panel ai-panel"><div className="ai-spark">✦</div><h3>Leitura inteligente</h3><p>Envie uma receita e o Katu’I ajuda a organizar os medicamentos.</p><button className="soft-btn ai-btn" onClick={() => onAction({ kind: 'ai', title: 'Leitura inteligente de receita', endpoint: '/ai/prescription' })}>✦ Ler receita com IA <Icon name="arrow" size={14}/></button><button className="outline-btn report-btn" onClick={() => onAction({ kind: 'form', title: 'Criar relatório PDF', endpoint: '/reports' })}><Icon name="file" size={14}/> Criar relatório PDF</button></div></div>
}

function FemaleHealthPage({ profile, onPregnancyChange, onAction, selectedDate }) {
  const storageKey = `katui.pregnancy.${profile?.id || 'current'}`
  const [pregnancy, setPregnancy] = useState(() => {
    try { return JSON.parse(localStorage.getItem(storageKey)) || { enabled: false, startDate: '' } } catch { return { enabled: false, startDate: '' } }
  })
  const [startDate, setStartDate] = useState(pregnancy.startDate || '')
  const [pregnancyConfirming, setPregnancyConfirming] = useState(false)
  const [pregnancyError, setPregnancyError] = useState('')
  const [records, setRecords] = useState([])
  const [medicationRecords, setMedicationRecords] = useState([])
  const [calendarActionDate, setCalendarActionDate] = useState('')
  const [femaleSelectedDate, setFemaleSelectedDate] = useState(selectedDate || dateKeyForFemaleHealth(new Date()))
  const [pregnancyMenuOpen, setPregnancyMenuOpen] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [refreshKey, setRefreshKey] = useState(0)
  const [calendarMonth, setCalendarMonth] = useState(() => {
    const selected = selectedDate ? new Date(`${selectedDate}T00:00:00`) : new Date()
    return new Date(selected.getFullYear(), selected.getMonth(), 1)
  })

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(storageKey)) || { enabled: false, startDate: '' }
      setPregnancy(saved)
      setStartDate(saved.startDate || '')
      setPregnancyConfirming(false)
      setPregnancyError('')
    } catch {
      setPregnancy({ enabled: false, startDate: '' })
      setStartDate('')
    }
  }, [storageKey])

  useEffect(() => {
    if (selectedDate) setFemaleSelectedDate(selectedDate)
  }, [selectedDate])

  useEffect(() => {
    let mounted = true
    const loadRecords = async () => {
      setLoading(true)
      setLoadError('')
      try {
        const [healthRecords, medications] = await Promise.all([
          resourceApi.list('/feminine-health'),
          resourceApi.list('/medications'),
        ])
        if (!Array.isArray(healthRecords) || !Array.isArray(medications)) throw new Error('A API retornou uma lista inválida para Saúde feminina.')
        if (mounted) {
          setRecords(healthRecords)
          setMedicationRecords(medications)
        }
      } catch (error) {
        if (mounted) setLoadError(error.message || 'Não foi possível carregar os dados de Saúde feminina.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    loadRecords()
    const onResourceChange = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (['/feminine-health', '/medications'].some((resource) => endpoint === resource || endpoint.startsWith(`${resource}/`))) setRefreshKey((current) => current + 1)
    }
    window.addEventListener('katui:resource-change', onResourceChange)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', onResourceChange)
    }
  }, [profile?.id, refreshKey])

  const cycleInsights = getCycleInsights(records)
  const cycles = cycleInsights.cycles
  const openCycle = cycles.find((cycle) => !cycle.endDate)
  const dailyLogs = records.filter((record) => record.recordType === 'daily-symptom' || (!record.recordType && !record.startDate && !record.recordType?.includes('contraceptive')))
    .sort((first, second) => String(second.date || '').localeCompare(String(first.date || '')))
  const contraceptiveLogs = records.filter((record) => record.recordType === 'contraceptive-dose')
  const contraceptive = medicationRecords.find((medication) => String(medication.purpose || medication.category || '').toLowerCase().includes('anticoncepcional'))
  const today = dateKeyForFemaleHealth(new Date())
  const todayDose = contraceptiveLogs.find((record) => record.date === today && (!contraceptive || String(record.medicationId) === String(contraceptive.id || contraceptive._id)))
  const contraceptiveDay = getContraceptiveDay(contraceptive?.date, today, contraceptive?.contraceptivePackType)

  const requestActivation = () => {
    if (!startDate) { setPregnancyError('Informe a data que será usada como início do acompanhamento.'); return }
    setPregnancyError('')
    setPregnancyConfirming(true)
  }
  const confirmActivation = async () => {
    const next = { enabled: true, startDate }
    localStorage.setItem(storageKey, JSON.stringify(next))
    setPregnancy(next)
    setPregnancyConfirming(false)
    await onPregnancyChange(next)
  }
  const disablePregnancy = async () => {
    if (!window.confirm('Desativar o acompanhamento de gestação deste perfil?')) return
    const next = { enabled: false, startDate: '' }
    localStorage.setItem(storageKey, JSON.stringify(next))
    setPregnancy(next)
    setStartDate('')
    await onPregnancyChange(next)
  }
  const beginCycle = (date = femaleSelectedDate || today) => onAction({
    kind: 'form',
    title: 'Registrar início de ciclo',
    endpoint: '/feminine-health',
    data: { recordType: 'menstrual-cycle', date },
  })
  const finishCycle = (cycle, date = today) => onAction({
    kind: 'form',
    title: 'Registrar fim de ciclo',
    endpoint: `/feminine-health/${encodeURIComponent(cycle.id || cycle._id)}`,
    method: 'PATCH',
    data: { ...cycle, recordType: 'menstrual-cycle', endDate: date },
  })
  const openDailyLog = (date) => {
    const existing = dailyLogs.find((record) => record.date === date)
    onAction({
      kind: 'form',
      title: 'Registrar sintomas e fluxo',
      endpoint: existing ? `/feminine-health/${encodeURIComponent(existing.id || existing._id)}` : '/feminine-health',
      method: existing ? 'PATCH' : 'POST',
      data: { ...existing, recordType: 'daily-symptom', date },
    })
  }
  const logContraceptiveDose = (taken) => onAction({
    kind: 'form',
    title: 'Registrar tomada de anticoncepcional',
    endpoint: '/feminine-health',
    data: { recordType: 'contraceptive-dose', medicationId: contraceptive?.id || contraceptive?._id || '', name: contraceptive?.name || '', date: today, time: new Date().toTimeString().slice(0, 5), taken },
  })
  const configureContraceptive = () => {
    const startTime = new Date(Date.now() + 10 * 60_000)
    const time = `${String(startTime.getHours()).padStart(2, '0')}:${String(startTime.getMinutes()).padStart(2, '0')}`
    onAction({
      kind: 'form',
      title: 'Configurar anticoncepcional',
      endpoint: '/medications',
      data: { name: '', date: today, time, purpose: 'Anticoncepcional', frequencyType: 'Vezes ao dia', frequencyValue: '1', continuous: true, generateAlarms: true, contraceptivePackType: '21' },
    })
  }
  const openCalendarDay = (date) => {
    setFemaleSelectedDate(date)
    setCalendarActionDate(date)
  }
  const selectCalendarAction = (callback) => {
    setCalendarActionDate('')
    callback()
  }
  const changeCalendarMonth = (amount) => setCalendarMonth((month) => new Date(month.getFullYear(), month.getMonth() + amount, 1))
  const monthStart = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), 1)
  const firstVisibleDate = new Date(monthStart.getFullYear(), monthStart.getMonth(), -monthStart.getDay() + 1)
  const calendarDays = Array.from({ length: 42 }, (_, index) => new Date(firstVisibleDate.getFullYear(), firstVisibleDate.getMonth(), firstVisibleDate.getDate() + index))
  const formatDate = (value) => {
    if (!value) return ''
    const date = new Date(`${String(value).slice(0, 10)}T00:00:00`)
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
  }
  const formatPrediction = (date) => date?.toLocaleDateString('pt-BR', { day: 'numeric', month: 'long' }) || 'Aguardando mais registros'

  return <>
  <div className="female-health-layout">
    {loadError && <p className="form-error female-health-error" role="alert">{loadError}</p>}
    <section className="female-health-intro">
      <div><span className="female-health-kicker">SEU CICLO, NO SEU TEMPO</span><h2>Acompanhe seu ciclo</h2><p>Registre datas, sintomas e humor para consultar seu histórico neste perfil.</p></div>
      <button className="primary-btn" onClick={() => openCycle ? finishCycle(openCycle, today) : beginCycle()} disabled={Boolean(openCycle && today < openCycle.startDate)}><Icon name={openCycle ? 'check' : 'plus'} size={16}/>{openCycle ? 'Terminar ciclo' : 'Registrar início do fluxo'}</button>
    </section>

    <section className="female-health-summary-grid">
      <article className="female-health-stat"><span className="female-stat-icon"><Icon name="calendar" size={19}/></span><div><small>Próxima menstruação estimada</small><strong>{formatPrediction(cycleInsights.nextPeriodDate)}</strong><span>{cycleInsights.averageCycleLength ? `Média de ${cycleInsights.averageCycleLength} dias entre ciclos` : 'Registre ao menos dois inícios de ciclo'}</span></div></article>
      <article className="female-health-stat"><span className="female-stat-icon fertile"><Icon name="spark" size={19}/></span><div><small>Janela fértil estimada</small><strong>{cycleInsights.fertileStart ? `${formatDate(dateKeyForFemaleHealth(cycleInsights.fertileStart))} – ${formatDate(dateKeyForFemaleHealth(cycleInsights.fertileEnd))}` : 'Sem estimativa disponível'}</strong><span>Estimativa baseada nos ciclos registrados</span></div></article>
      <article className="female-health-stat"><span className="female-stat-icon period"><Icon name="clock" size={19}/></span><div><small>Duração média do fluxo</small><strong>{cycleInsights.completedPeriodCount ? `${cycleInsights.averagePeriodLength} dias` : 'Sem dados suficientes'}</strong><span>{cycleInsights.completedPeriodCount ? 'Média dos períodos encerrados' : 'A média será calculada após encerrar um ciclo'}</span></div></article>
    </section>

    <p className="female-health-disclaimer" role="note">As previsões são aproximadas, podem variar e não devem ser usadas para evitar ou planejar uma gravidez nem substituir orientação profissional.</p>

    <div className="female-health-main-grid">
      <section className="panel female-cycle-calendar">
        <div className="female-panel-heading"><div><h3>Calendário do ciclo</h3><p>Selecione um dia para registrar sintomas e fluxo.</p></div><div className="female-calendar-controls"><button type="button" aria-label="Mês anterior" onClick={() => changeCalendarMonth(-1)}>‹</button><strong>{calendarMonth.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}</strong><button type="button" aria-label="Próximo mês" onClick={() => changeCalendarMonth(1)}>›</button></div></div>
        <div className="female-calendar-grid female-weekday-row">{['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map((day, index) => <span key={`${day}-${index}`}>{day}</span>)}</div>
        <div className="female-calendar-grid">{calendarDays.map((day) => {
          const kind = getCycleDayKind(day, cycleInsights)
          const isCurrentMonth = day.getMonth() === calendarMonth.getMonth()
          const dayKey = dateKeyForFemaleHealth(day)
          const isSelected = dayKey === femaleSelectedDate
          return <button key={dayKey} type="button" className={`female-calendar-day${isCurrentMonth ? '' : ' outside-month'}${kind ? ` ${kind}` : ''}${isSelected ? ' selected' : ''}`} aria-label={`${day.toLocaleDateString('pt-BR')}${kind ? `, ${kind === 'period' ? 'fluxo registrado' : kind === 'fertile' ? 'janela fértil estimada' : 'menstruação estimada'}` : ''}`} aria-pressed={isSelected} onClick={() => openCalendarDay(dayKey)}><span>{day.getDate()}</span>{kind && <i/>}</button>
        })}</div>
        <div className="female-calendar-legend"><span><i className="period"/> Fluxo registrado</span><span><i className="predicted-period"/> Período estimado</span><span><i className="fertile"/> Janela fértil estimada</span></div>
      </section>
    </div>

    <section className="panel female-section-panel">
      <div className="female-panel-heading"><div><h3>Anticoncepcional e lembretes</h3><p>Use um lembrete para registrar o horário da sua rotina.</p></div><span className="female-cycle-icon"><Icon name="pill" size={19}/></span></div>
      {contraceptive ? <div className="female-contraceptive-row"><div><strong>{contraceptive.name}</strong><span>{getContraceptivePatternDescription(contraceptive, today) || (contraceptive.time ? `Lembrete diário às ${contraceptive.time}` : 'Lembrete configurado')}</span>{contraceptive.time && <small>Tomada diária às {contraceptive.time}{contraceptive.generateAlarms ? ' · alarmes ativados' : ''}</small>}</div><div className="female-dose-actions">{todayDose ? <span className={`female-dose-status${todayDose.taken ? ' taken' : ' missed'}`}>{todayDose.taken ? 'Registrado hoje' : 'Marcado como não tomado'}</span> : contraceptiveDay?.phase === 'pause' ? <span className="female-dose-status">Pausa da cartela · sem tomada prevista hoje</span> : <><button type="button" className="soft-btn" onClick={() => logContraceptiveDose(true)}>Marcar tomada</button><button type="button" className="text-btn" onClick={() => logContraceptiveDose(false)}>Não tomei</button></>}</div></div> : <div className="female-empty-inline"><p>Nenhum anticoncepcional cadastrado.</p><button type="button" className="soft-btn" onClick={configureContraceptive}><Icon name="plus" size={14}/> Configurar anticoncepcional</button></div>}
    </section>

    <div className="female-history-grid">
      <section className="panel female-section-panel">
        <div className="female-panel-heading"><div><h3>Histórico de ciclos</h3><p>Datas registradas neste perfil</p></div></div>
        {loading ? <p className="female-muted">Carregando ciclos…</p> : cycles.length ? <div className="female-history-list">{cycles.slice(0, 8).map((cycle) => <article className="female-history-item" key={cycle.id || cycle._id || cycle.startDate}><span className={`female-history-mark${cycle.endDate ? '' : ' active'}`}/><div><strong>{formatDate(cycle.startDate)}{cycle.endDate ? ` – ${formatDate(cycle.endDate)}` : ' · em andamento'}</strong><small>{cycle.flow ? `Fluxo: ${cycle.flow}` : 'Registro de menstruação'}{cycle.physicalSymptoms ? ` · ${cycle.physicalSymptoms}` : ''}</small></div>{!cycle.endDate && <button type="button" className="text-btn" onClick={() => finishCycle(cycle)}>Encerrar</button>}</article>)}</div> : <p className="female-muted">Ainda não há ciclos registrados. Use “Registrar início do fluxo” para começar.</p>}
      </section>
      <section className="panel female-section-panel">
        <div className="female-panel-heading"><div><h3>Sintomas e humor</h3><p>Registros diários recentes</p></div><button type="button" className="text-btn" onClick={() => openDailyLog(selectedDate || today)}>Registrar <Icon name="plus" size={14}/></button></div>
        {loading ? <p className="female-muted">Carregando registros…</p> : dailyLogs.length ? <div className="female-history-list">{dailyLogs.slice(0, 6).map((record) => <button type="button" className="female-daily-item" key={record.id || record._id || record.date} onClick={() => openDailyLog(record.date)}><strong>{formatDate(record.date)}</strong><span>{[record.flow && `Fluxo ${record.flow.toLowerCase()}`, record.physicalSymptoms, record.mood].filter(Boolean).join(' · ') || 'Registro do dia'}</span></button>)}</div> : <p className="female-muted">Toque em um dia do calendário para registrar fluxo, sintomas e humor.</p>}
      </section>
    </div>

    <section className="panel female-pregnancy-panel">
      <button type="button" className="female-pregnancy-toggle" aria-expanded={pregnancyMenuOpen} onClick={() => setPregnancyMenuOpen((open) => !open)}><span className="female-cycle-icon"><Icon name="heart" size={19}/></span><span><strong>Acompanhamento de gestação</strong><small>{pregnancy.enabled ? `Ativo · início em ${formatDate(pregnancy.startDate)}` : 'Recurso opcional e privado'}</small></span><span className="female-pregnancy-chevron" aria-hidden="true">{pregnancyMenuOpen ? '−' : '+'}</span></button>
      {pregnancyMenuOpen && (pregnancy.enabled ? <div className="female-pregnancy-active"><span>Acompanhamento ativo · início informado em {formatDate(pregnancy.startDate)}</span><button type="button" className="text-btn" onClick={disablePregnancy}>Desativar</button></div> : pregnancyConfirming ? <div className="female-pregnancy-confirm"><p>Usar {formatDate(startDate)} como data de início do acompanhamento?</p><div><button type="button" className="cancel-btn" onClick={() => setPregnancyConfirming(false)}>Voltar</button><button type="button" className="primary-btn" onClick={confirmActivation}>Confirmar</button></div></div> : <div className="female-pregnancy-form"><label>Data de início<input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)}/></label><button type="button" className="soft-btn" onClick={requestActivation}>Ativar acompanhamento</button>{pregnancyError && <small className="form-error">{pregnancyError}</small>}</div>)}
    </section>
  </div>
  {calendarActionDate && (() => {
    const actionDate = calendarActionDate
    const isFutureDate = actionDate > today
    const canFinishCycle = Boolean(openCycle) && actionDate >= openCycle.startDate && !isFutureDate
    const dateLabel = formatDate(actionDate)
    return <div className="modal-backdrop female-day-action-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setCalendarActionDate('')}><section className="modal-card female-day-action-modal" role="dialog" aria-modal="true" aria-labelledby="female-day-action-title"><button type="button" className="modal-close" aria-label="Fechar" onClick={() => setCalendarActionDate('')}>×</button><div className="modal-icon"><Icon name="calendar" size={22}/></div><p className="eyebrow">AÇÕES DO DIA</p><h2 id="female-day-action-title">{dateLabel}</h2><p className="modal-subtitle">O que você gostaria de registrar?</p><div className="female-day-action-options"><button type="button" className="female-day-action-option" disabled={isFutureDate} onClick={() => selectCalendarAction(() => openDailyLog(actionDate))}><span className="female-day-action-icon"><Icon name="pulse" size={19}/></span><span><strong>Adicionar sintomas e fluxo</strong><small>Registre sintomas, humor ou intensidade do fluxo</small></span><Icon name="chevron" size={16}/></button>{openCycle ? <button type="button" className="female-day-action-option finish" disabled={!canFinishCycle} onClick={() => selectCalendarAction(() => finishCycle(openCycle, actionDate))}><span className="female-day-action-icon"><Icon name="check" size={19}/></span><span><strong>Terminar ciclo</strong><small>{actionDate < openCycle.startDate ? `Escolha uma data após ${formatDate(openCycle.startDate)}` : isFutureDate ? 'Não é possível encerrar em uma data futura' : 'Marque este dia como o último dia do fluxo'}</small></span><Icon name="chevron" size={16}/></button> : <button type="button" className="female-day-action-option start" disabled={isFutureDate} onClick={() => selectCalendarAction(() => beginCycle(actionDate))}><span className="female-day-action-icon"><Icon name="plus" size={19}/></span><span><strong>Marcar primeiro dia do ciclo</strong><small>Inicie um novo registro de ciclo neste dia</small></span><Icon name="chevron" size={16}/></button>}</div>{isFutureDate && <small className="female-day-action-note">Não é possível registrar dados em uma data futura.</small>}</section></div>
  })()}
  </>
}

function ResourcePage({ active, icon, endpoint, resource, selectedDate, onDateSelect, calendarRecords, calendarLoadError, onAction, usePushAlarms = false }) {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [synced, setSynced] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  useEffect(() => {
    let mounted = true
    setLoading(true)
    setLoadError('')
    const recordsRequest = usePushAlarms
      ? listBackgroundAlarms({ token: localStorage.getItem('katui.auth-token'), profileId: localStorage.getItem('katui.active-profile-id') })
      : resourceApi.list(endpoint || '/records')
    recordsRequest.then((data) => {
      if (!mounted) return
      if (!Array.isArray(data)) throw new Error('A API retornou uma lista inválida.')
      setRecords(data)
      setSynced(true)
    }).catch((error) => {
      if (mounted) setLoadError(error.message || 'Não foi possível carregar os registros.')
    }).finally(() => mounted && setLoading(false))
    const handleResourceChange = (event) => {
      const changedEndpoint = event.detail?.endpoint || ''
      if (changedEndpoint === endpoint || changedEndpoint.startsWith(`${endpoint}/`)) setRefreshKey((value) => value + 1)
    }
    window.addEventListener('katui:resource-change', handleResourceChange)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', handleResourceChange)
    }
  }, [endpoint, refreshKey, usePushAlarms])
  const visibleRecords = resource === 'alarms' ? records.filter((record) => record.status !== 'Concluído') : records.filter((record) => recordOccursOnDate(record, selectedDate, resource))
  const refresh = () => setRefreshKey((value) => value + 1)
  return <div className="resource-layout"><CalendarCard selectedDate={selectedDate} onDateSelect={onDateSelect} records={calendarRecords} resource={resource} loadError={calendarLoadError}/><div className="module-grid"><div className="panel wide-panel"><div className="panel-header"><div><h3>Registros de {active.toLowerCase()}</h3><p>{loading ? 'Carregando seus registros...' : loadError || (synced ? 'Dados atualizados' : 'Você ainda não adicionou nenhum registro.')}</p></div><button className="panel-link" onClick={refresh} disabled={loading}>Atualizar <Icon name="arrow" size={14}/></button></div><div className="resource-list">{visibleRecords.length ? visibleRecords.map((item, index) => {
    const id = item.id || item._id
    const title = item.title || item.name || item.label || 'Registro'
    const detail = item.detail || item.description || item.date || 'Sincronizado com o servidor'
    const status = item.status || 'Ativo'
    return <div className="resource-row" key={id || `${title}-${index}`}><div className="resource-icon"><Icon name={icon} size={18}/></div><div className="resource-main"><strong>{title}</strong><span>{detail}</span></div><span className="resource-status">{status}</span>{id && <button className="row-action" aria-label={`Remover ${title}`} onClick={() => onAction({ kind: 'confirm', method: 'DELETE', title: 'Remover registro?', message: `O registro “${title}” será removido.`, endpoint: `${endpoint || '/records'}/${encodeURIComponent(id)}`, data: item })}>•••</button>}</div>
  }) : !loading && !loadError && <EmptyState icon={icon} title={`Nenhum registro em ${active.toLowerCase()} nesta data`} message="Escolha outro dia no calendário ou adicione um novo registro." action={`Adicionar em ${active}`} onAction={() => onAction({ kind: 'form', title: `Adicionar em ${active}`, endpoint: endpoint || '/records' })}/>}</div></div></div></div>
}

function EmptyModulePage({ active, icon, onAction }) {
  return <div className="empty-module panel"><div className="empty-icon"><Icon name={icon} size={28}/></div><h2>{active}</h2><p>Esta área está pronta para receber seus registros. Comece adicionando uma nova informação de saúde.</p><button className="primary-btn" onClick={() => onAction({ kind: 'form', title: `Adicionar em ${active}`, endpoint: `/${active.toLowerCase().replaceAll(' ', '-')}` })}><Icon name="plus" size={16}/> Adicionar registro</button></div>
}

function AiSettings({ settings, onSave }) {
  const current = { provider: 'system', model: '', configured: false, maskedKey: '', ...(settings || {}) }
  const [provider, setProvider] = useState(current.provider)
  const [model, setModel] = useState(current.model || '')
  const [apiKey, setApiKey] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const isCustom = provider !== 'system'
  const canSave = !isCustom || Boolean(apiKey.trim()) || current.configured
  const save = async () => {
    if (!canSave) return
    setSaving(true)
    setMessage('')
    try {
      await onSave({ provider, model: model.trim(), apiKey: apiKey.trim() })
      setApiKey('')
      setMessage('Preferência salva. A chave não fica guardada neste navegador.')
    } catch {
      setMessage('Não foi possível salvar agora. Tente novamente quando houver conexão.')
    } finally {
      setSaving(false)
    }
  }
  const remove = async () => {
    setProvider('system')
    setModel('')
    setApiKey('')
    setSaving(true)
    try {
      await onSave({ provider: 'system', model: '', apiKey: '' })
      setMessage('Chave removida. O Katu’I usará a IA padrão do sistema.')
    } catch {
      setMessage('Não foi possível remover a configuração agora.')
    } finally {
      setSaving(false)
    }
  }
  return <div className="ai-settings"><div className="ai-settings-intro"><div className="ai-settings-icon">✦</div><div><strong>Escolha sua IA</strong><span>Use a IA padrão do Katu’I ou conecte uma chave própria para este perfil.</span></div></div><label>Provedor<select value={provider} onChange={(event) => setProvider(event.target.value)}><option value="system">IA padrão do Katu’I</option><option value="openai">OpenAI</option><option value="google">Google Gemini</option><option value="anthropic">Anthropic Claude</option></select></label>{isCustom && <><label>Modelo (opcional)<input value={model} onChange={(event) => setModel(event.target.value)} placeholder="Ex.: modelo recomendado pelo provedor" /></label><label>Chave de API{current.configured && <small className="field-hint">Configurada: {current.maskedKey || '••••••••'}</small>}<input type="password" value={apiKey} onChange={(event) => setApiKey(event.target.value)} placeholder={current.configured ? 'Digite apenas para substituir' : 'Cole sua chave de API'} autoComplete="new-password" /></label><div className="privacy-notice"><strong>A chave fica protegida</strong><span>Ela é enviada somente ao servidor autenticado por HTTPS e não é salva no navegador, no backup ou no histórico.</span></div></>}<div className="ai-settings-actions"><button className="primary-btn" disabled={!canSave || saving} onClick={save}>{saving ? 'Salvando...' : 'Salvar configuração'}</button>{current.configured && <button className="cancel-btn" disabled={saving} onClick={remove}>Remover chave</button>}</div>{message && <small className="form-message">{message}</small>}</div>
}

function HelpPage() {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(null)
  const faqs = [
    ['Primeiros passos', 'Como começo a usar o Katu’I?', 'Conclua o tutorial inicial ou pule para preencher os dados depois. Em seguida, escolha um perfil e use Adicionar registro para cadastrar medicamentos, consultas, sintomas e outros cuidados.'],
    ['Dados e sincronização', 'O que acontece quando estou sem internet?', 'Os registros comuns ficam em uma fila local criptografada e serão enviados quando a conexão voltar. O indicador de nuvem no topo mostra quantos itens ainda aguardam sincronização.'],
    ['Dados e sincronização', 'Como sei se meus dados foram enviados?', 'Quando não houver itens pendentes, o ícone de nuvem exibirá um check. A confirmação de um formulário offline significa que ele foi salvo neste dispositivo, não que já chegou ao servidor.'],
    ['Privacidade', 'Meus dados ficam seguros?', 'O Katu’I usa autenticação e deve usar HTTPS ao conversar com o servidor. Os dados pendentes são protegidos no armazenamento local, mas mantenha seu dispositivo atualizado e protegido por senha.'],
    ['Perfis', 'Posso cuidar de mais de uma pessoa?', 'Sim. Use o seletor de perfil no topo para alternar entre você e dependentes. Preferências como tema, acessibilidade e IA são independentes para cada perfil.'],
    ['Inteligência artificial', 'Qual IA posso usar?', 'Você pode usar a IA padrão do Katu’I ou configurar um provedor próprio em Configurações > Inteligência artificial. A chave própria não é armazenada no navegador e exige conexão para ser salva.'],
    ['Inteligência artificial', 'A IA salva uma receita automaticamente?', 'Não. A leitura gera uma sugestão revisável. Você precisa conferir os dados e confirmar antes que qualquer medicamento seja cadastrado.'],
    ['Acessibilidade', 'Como desfazer uma ação ou usar atalhos?', 'Depois de criar ou remover um registro, use Ctrl+Z (ou Command+Z no Mac) para desfazer. O atalho não interfere na edição de texto: Ctrl+C e Ctrl+V continuam funcionando normalmente nos campos. Ative o feedback sonoro em Configurações > Preferências gerais se quiser ouvir confirmações.'],
    ['Acessibilidade', 'Como aumentar o tamanho das letras?', 'Abra Configurações > Acessibilidade e escolha o tamanho desejado. O layout foi preparado para reorganizar menus e formulários em escalas maiores.'],
  ]
  const filtered = faqs.filter(([category, question, answer]) => (category + ' ' + question + ' ' + answer).toLowerCase().includes(query.toLowerCase()))
  return <div className="help-page"><section className="page-title-row"><div><p className="eyebrow">CENTRAL DE AJUDA</p><h1>Como podemos ajudar?</h1><p className="page-description">Encontre respostas rápidas sobre seus dados, perfis e recursos do Katu’I.</p></div></section><div className="help-search"><Icon name="search" size={18}/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Busque uma dúvida, como “offline” ou “IA”" aria-label="Buscar perguntas frequentes" /></div><div className="faq-list">{filtered.length ? filtered.map(([category, question, answer]) => { const id = category + '-' + question; const isOpen = open === id; return <article className={isOpen ? 'faq-item open' : 'faq-item'} key={id}><button className="faq-question" aria-expanded={isOpen} onClick={() => setOpen(isOpen ? null : id)}><span><small>{category}</small><strong>{question}</strong></span><Icon name="chevron" size={17}/></button>{isOpen && <p className="faq-answer">{answer}</p>}</article> }) : <div className="panel faq-empty"><div className="empty-icon"><Icon name="search" size={24}/></div><strong>Nenhuma resposta encontrada</strong><p>Tente buscar por outra palavra.</p></div>}</div></div>
}

function SettingsPage({ profile, darkMode, onToggleDarkMode, notificationsEnabled, onToggleNotifications, weeklySummaryEnabled, onToggleWeeklySummary, audioFeedbackEnabled, onToggleAudioFeedback, pushEnabled, pushConfigured, notificationPermission, onTogglePush, onEnableLocalNotifications, accessibility, onToggleAccessibility, onExportBackup, onImportBackup, onClearLocalData, onSyncBackup, onSaveProfileTheme, aiSettings, onSaveAiSettings, isGuest = false, onRegister }) {
  const [tab, setTab] = useState('general')
  const [colorOpen, setColorOpen] = useState(false)
  const desktopShell = isDesktopShell()
  const tabs = [['general', 'Preferências gerais'], ...(!isGuest ? [['ai', 'Inteligência artificial']] : []), ['accessibility', 'Acessibilidade'], ['privacy', 'Privacidade e segurança'], ...(!isGuest ? [['backup', 'Backup pessoal']] : [])]
  const toggle = (label, enabled, onChange) => <button className={enabled ? 'toggle on' : 'toggle'} aria-label={label} aria-pressed={enabled} onClick={onChange}><i></i></button>
  const textScale = Number(accessibility.textScale || (accessibility.largeText ? 125 : 100))
  return <div className="settings-grid"><div className="panel settings-nav">{tabs.map(([value, label]) => <button key={value} className={tab === value ? 'settings-tab active' : 'settings-tab'} onClick={() => setTab(value)}>{label}</button>)}</div><div className="panel settings-content">
    {tab === 'general' && <><h3>Preferências gerais</h3><p>Escolha como o Katu’I funciona {isGuest ? 'neste dispositivo' : `para ${profile?.name || 'este perfil'}`}.</p>{isGuest && <div className="guest-settings-notice"><strong>Modo Guest</strong><span>Seus registros ficam somente neste navegador. Crie uma conta para ativar backup e sincronização.</span><button className="soft-btn" onClick={onRegister}>Registre-se para fazer backup <Icon name="arrow" size={14}/></button></div>}<div className="setting-row"><div><strong>Notificações de medicamentos</strong><span>Receba lembretes no horário certo para este perfil.</span></div>{toggle('Alternar notificações de medicamentos', notificationsEnabled, onToggleNotifications)}</div><div className="setting-row"><div><strong>Resumo semanal</strong><span>Receba um panorama dos registros deste perfil.</span></div>{toggle('Alternar resumo semanal', weeklySummaryEnabled, onToggleWeeklySummary)}</div><div className="setting-row"><div><strong>Modo escuro</strong><span>{darkMode ? 'Ativado para este perfil.' : 'Use uma aparência mais confortável à noite.'}</span></div>{toggle('Alternar modo escuro', darkMode, onToggleDarkMode)}</div><div className="setting-row"><div><strong>Feedback sonoro</strong><span>Toques breves ao salvar, desfazer ou encontrar um erro. Desativado por padrão.</span></div>{toggle('Alternar feedback sonoro', audioFeedbackEnabled, onToggleAudioFeedback)}</div>{!isGuest && <div className="setting-row profile-theme-setting"><div><strong>Cor do perfil</strong><span>Personalize os destaques de {profile?.name || 'este perfil'} sem alterar os outros perfis.</span></div><button className="soft-btn" onClick={() => setColorOpen(true)}>Escolher cor</button></div>}{colorOpen && <ProfileColorModal profile={profile} onClose={() => setColorOpen(false)} onSave={(themeId) => { setColorOpen(false); onSaveProfileTheme(themeId) }}/>}</>}
    {tab === 'general' && <div className="setting-row"><div><strong>{isGuest || desktopShell ? 'Lembretes locais' : 'Alarmes em segundo plano'}</strong><span>{desktopShell ? 'No app desktop, os lembretes só avisam enquanto o Katu’I estiver aberto. Push em segundo plano está disponível na versão web.' : isGuest ? 'Os alarmes Guest só avisam enquanto o Katu’I estiver aberto neste dispositivo.' : pushConfigured ? 'Receba lembretes de medicamentos mesmo quando o site estiver fechado.' : 'O serviço de notificações do servidor ainda precisa ser configurado.'}</span></div>{isGuest || desktopShell ? notificationPermission === 'granted' ? <span className="security-status">Permissão ativa · com o app aberto</span> : <button className="soft-btn" onClick={onEnableLocalNotifications}>Ativar neste dispositivo</button> : pushConfigured ? toggle('Alternar alarmes em segundo plano', pushEnabled, onTogglePush) : <span className="security-status">Não configurado</span>}</div>}
    {tab === 'ai' && <><h3>Inteligência artificial</h3><p>Escolha qual serviço será usado para organizar receitas e outros documentos deste perfil.</p><AiSettings settings={aiSettings} onSave={onSaveAiSettings}/></>}
    {tab === 'accessibility' && <><h3>Acessibilidade</h3><p>Adapte a experiência para ficar mais confortável e fácil de usar.</p><div className="setting-row accessibility-size-row"><div><strong>Tamanho do texto</strong><span>Escolha o tamanho que fica mais confortável para você. O layout se reorganiza automaticamente.</span></div><select className="accessibility-size-select" aria-label="Escolher tamanho do texto" value={textScale} onChange={(event) => onToggleAccessibility('textScale', Number(event.target.value))}>{[100, 125, 150, 175, 200].map((size) => <option key={size} value={size}>{size}%</option>)}</select></div><div className="setting-row"><div><strong>Fonte para dislexia</strong><span>Usa uma fonte com formas mais fáceis de diferenciar.</span></div>{toggle('Alternar fonte para dislexia', accessibility.dyslexiaFont, () => onToggleAccessibility('dyslexiaFont'))}</div><div className="setting-row"><div><strong>Alto contraste</strong><span>Reforça a diferença entre texto, fundo, controles e foco do teclado.</span></div>{toggle('Alternar alto contraste', accessibility.highContrast, () => onToggleAccessibility('highContrast'))}</div><div className="setting-row"><div><strong>Reduzir animações</strong><span>Evita movimentos desnecessários na interface.</span></div>{toggle('Alternar redução de animações', accessibility.reduceMotion, () => onToggleAccessibility('reduceMotion'))}</div></>}
    {tab === 'privacy' && <><h3>Privacidade e segurança</h3><p>Controle o que permanece salvo neste dispositivo.</p><div className="privacy-notice"><strong>Seus dados são seus.</strong><span>O Katu’I não inclui tokens de acesso nos arquivos de backup e só envia dados ao servidor quando a sincronização estiver disponível.</span></div><div className="setting-row"><div><strong>Sessão atual</strong><span>O acesso do servidor é enviado somente em chamadas autenticadas.</span></div><span className="security-status">Protegida</span></div><div className="setting-row"><div><strong>Dados locais</strong><span>Remove rascunhos, perfis, preferências e ações pendentes deste dispositivo.</span></div><button className="danger-btn small-danger" onClick={onClearLocalData}>Apagar dados locais</button></div></>}
    {tab === 'backup' && <><h3>Backup pessoal</h3><p>Exporte seus dados para guardar uma cópia ou importe um backup anterior.</p><div className="backup-actions"><button className="primary-btn" onClick={onExportBackup}><Icon name="arrow" size={16}/> Exportar backup</button><label className="soft-btn import-btn"><Icon name="file" size={15}/> Importar backup<input className="file-input" type="file" accept="application/json,.json" onChange={(event) => { const file = event.target.files?.[0]; if (file) onImportBackup(file); event.target.value = '' }}/></label></div><div className="privacy-notice"><strong>Formato seguro</strong><span>O arquivo exportado contém preferências e dados locais do Katu’I, mas nunca inclui seu token de autenticação.</span></div><button className="outline-btn backup-sync-btn" onClick={onSyncBackup}>Sincronizar backup quando possível <Icon name="arrow" size={14}/></button></>}
  </div></div>
}

function SymptomForm({ form, setForm }) {
  const set = (key, value) => setForm({ ...form, [key]: value })
  const physicalLocations = [['Geral', 'pulse'], ['Cabeça', 'profile'], ['Costas', 'pulse'], ['Peito', 'heart'], ['Estômago', 'heart'], ['Braços', 'pulse'], ['Pernas', 'pulse']]
  const moodOptions = {
    Positivo: [['Alegre', 'sun'], ['Calmo', 'spark'], ['Motivado', 'bolt'], ['Produtivo', 'spark'], ['Grato', 'heart'], ['Energizado', 'bolt'], ['Eufórico', 'spark']],
    Neutro: [['Normal', 'check'], ['Pensativo', 'spark'], ['Cansado', 'battery'], ['Apático', 'pulse']],
    Baixo: [['Triste', 'cloud'], ['Desanimado', 'battery'], ['Solitário', 'profile'], ['Sensível', 'cloud'], ['Sem energia', 'battery'], ['Depressivo', 'sad']],
    Tenso: [['Ansioso', 'cloud'], ['Irritado', 'bolt'], ['Estressado', 'cloud'], ['Agitado', 'spark'], ['Sob pressão', 'pulse'], ['Confuso', 'brain']],
  }
  const input = (key, label, placeholder, icon) => <label className="symptom-field">{icon && <Icon name={icon} size={15}/>}<span>{label}</span><input value={form[key] || ''} onChange={(event) => set(key, event.target.value)} placeholder={placeholder || ''}/></label>
  return <div className="symptom-form">
    <div className="symptom-kind-tabs"><button className={form.symptomKind === 'FISICO' ? 'selected' : ''} onClick={() => set('symptomKind', 'FISICO')}><Icon name="pulse" size={16}/> Físico</button><button className={form.symptomKind === 'PSICOLOGICO' ? 'selected' : ''} onClick={() => set('symptomKind', 'PSICOLOGICO')}><Icon name="heart" size={16}/> Humor</button></div>
    {form.symptomKind === 'FISICO' ? <>
      {input('quality', 'O que você está sentindo?', 'Ex.: Dor de cabeça', 'pulse')}
      <div className="symptom-section-title"><Icon name="location" size={15}/> Localização</div>
      <div className="symptom-chip-grid">{physicalLocations.map(([label, icon]) => <button key={label} className={form.location === label ? 'symptom-chip selected' : 'symptom-chip'} onClick={() => set('location', label)}><Icon name={icon} size={15}/>{label}</button>)}</div>
      {input('location', 'Ou digite o local...', 'Ex.: lado esquerdo da cabeça', 'location')}
      {input('duration', 'Quando começou / quanto dura?', 'Ex.: desde ontem', 'clock')}
      {input('factors', 'O que piora ou melhora?', 'Opcional', 'search')}
      {input('impact', 'Como afeta seu dia?', 'Opcional', 'trend')}
      <label className="symptom-check"><input type="checkbox" checked={Boolean(form.incapacitating)} onChange={(event) => set('incapacitating', event.target.checked)}/> Isso te impede de fazer algo?</label>
    </> : <>
      <div className="symptom-section-title"><Icon name="brain" size={15}/> O clima geral</div>
      <div className="mood-grid">{['Positivo', 'Neutro', 'Baixo', 'Tenso'].map((mood) => <button key={mood} className={form.climate === mood ? 'selected' : ''} onClick={() => setForm({ ...form, climate: mood, nuances: [] })}><Icon name={mood === 'Positivo' ? 'sun' : mood === 'Neutro' ? 'cloud' : mood === 'Baixo' ? 'sad' : 'brain'} size={17}/>{mood}</button>)}</div>
      {form.climate && <><div className="symptom-section-title"><Icon name="spark" size={15}/> Nuances</div><div className="symptom-chip-grid">{(moodOptions[form.climate] || []).map(([label, icon]) => { const selected = (form.nuances || []).includes(label); return <button key={label} className={selected ? 'symptom-chip selected' : 'symptom-chip'} onClick={() => set('nuances', selected ? form.nuances.filter((item) => item !== label) : [...(form.nuances || []), label])}><Icon name={icon} size={14}/>{label}</button> })}</div></>}
      {input('factors', 'Gatilhos ou motivos', 'Opcional: por que se sente assim?', 'search')}
    </>}
    <div className="symptom-intensity"><div><span>{form.symptomKind === 'FISICO' ? 'Intensidade da dor' : 'Força do sentimento'}</span><strong>{Number(form.intensity || 5) <= 3 ? (form.symptomKind === 'FISICO' ? 'Leve' : 'Estável') : Number(form.intensity || 5) <= 7 ? (form.symptomKind === 'FISICO' ? 'Moderada' : 'Presente') : (form.symptomKind === 'FISICO' ? 'Intensa' : 'Muito forte')}</strong></div><input type="range" min="1" max="10" value={form.intensity || 5} onChange={(event) => set('intensity', event.target.value)}/></div>
  </div>
}

function SymptomModal({ action, form, setForm, onClose, onSubmit, connection, canSubmit }) {
  const save = () => onSubmit({ ...action, data: { ...form, name: form.quality || form.climate, type: form.symptomKind, quality: form.symptomKind === 'PSICOLOGICO' ? `[${form.climate}] ${(form.nuances || []).join(', ')}` : form.quality } })
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="modal-card symptom-modal"><button className="modal-close" onClick={onClose}>×</button><div className="modal-icon"><Icon name={form.symptomKind === 'FISICO' ? 'pulse' : 'brain'} size={22}/></div><h2>{action.title}</h2><p className="modal-subtitle">Escolha entre um sintoma físico ou como você está se sentindo e preencha os detalhes do registro.</p><SymptomForm form={form} setForm={setForm}/>{!canSubmit && <small className="form-error">{form.symptomKind === 'FISICO' ? 'Informe o que você está sentindo.' : 'Escolha o clima geral do seu humor.'}</small>}<div className="modal-actions"><button className="cancel-btn" onClick={onClose}>Cancelar</button><button className="primary-btn" disabled={!canSubmit} onClick={save}>Registrar</button></div><small className="sync-note">{connection === 'online' ? 'Será sincronizado com o servidor.' : 'Salvo localmente e sincronizado quando o servidor conectar.'}</small></div></div>
}

function DependentModal({ action, onClose, onSubmit, connection, submissionError, submitting }) {
  const steps = ['Nome', 'Vínculo', 'Idade', 'Sexo', 'Peso', 'Altura', 'Alergias', 'Condições', 'Revisão']
  const [step, setStep] = useState(0)
  const [form, setForm] = useState({ name: '', relation: '', age: '', sex: 'nao-informado', weight: '', height: '', allergies: '', conditions: '' })
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value })
  const validation = step === 0 && form.name.trim().length < 2 ? 'Informe pelo menos 2 caracteres.' : step === 2 && form.age && (Number(form.age) < 0 || Number(form.age) > 120) ? 'Informe uma idade entre 0 e 120 anos.' : step === 4 && form.weight && (Number(form.weight) <= 0 || Number(form.weight) > 500) ? 'Informe um peso válido.' : step === 5 && form.height && (Number(form.height) < 30 || Number(form.height) > 250) ? 'Informe uma altura entre 30 e 250 cm.' : ''
  const canContinue = !validation && (step !== 0 || form.name.trim().length > 1)
  const finish = () => onSubmit({ ...action, data: { name: form.name.trim(), relation: form.relation || 'Dependente', age: form.age ? Number(form.age) : null, sex: form.sex, weight: form.weight ? Number(form.weight) : null, heightCm: form.height ? Number(form.height) : null, allergies: form.allergies.trim() || null, specialConditions: form.conditions.trim() || null, themeColor: 'turquoise', corHex: profileThemeOptions[0].color, preferences: { ...defaultProfilePreferences } } })
  const next = () => canContinue && (step === steps.length - 1 ? finish() : setStep(step + 1))
  if (step === 6 || step === 7) return <DependentHealthStep step={step} steps={steps} form={form} setForm={setForm} onClose={onClose} onBack={() => setStep(step - 1)} onNext={() => setStep(step + 1)} connection={connection}/>
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="modal-card dependent-modal"><button className="modal-close" onClick={onClose}>×</button><div className="modal-icon"><Icon name="profile" size={22}/></div><p className="eyebrow">NOVO PERFIL DE CUIDADO</p><h2>{step === steps.length - 1 ? 'Confira os dados' : 'Vamos conhecer essa pessoa'}</h2><p className="modal-subtitle">Etapa {step + 1} de {steps.length}. Você poderá alterar essas informações depois.</p><div className="dependent-progress">{steps.map((label, index) => <i key={label} className={index < step ? 'done' : index === step ? 'current' : ''} />)}</div>{step === 0 && <div className="dependent-step"><strong>Como podemos chamar?</strong><p>Use o nome da pessoa que você vai acompanhar.</p><input autoFocus value={form.name} onChange={update('name')} placeholder="Digite o nome" /></div>}{step === 1 && <div className="dependent-step"><strong>Qual é o vínculo?</strong><p>Isso ajuda a organizar seus perfis.</p><div className="dependent-choices">{['Filho(a)', 'Pai ou mãe', 'Avô ou avó', 'Cônjuge', 'Outro'].map((relation) => <button key={relation} className={form.relation === relation ? 'selected' : ''} onClick={() => setForm({ ...form, relation })}>{relation}</button>)}</div></div>}{step === 2 && <div className="dependent-step"><strong>Qual a idade?</strong><p>Essa informação ajuda a personalizar alguns cuidados.</p><div className="unit-input"><input autoFocus type="number" min="0" max="120" value={form.age} onChange={update('age')} placeholder="00"/><span>anos</span></div></div>}{step === 3 && <div className="dependent-step"><strong>Qual o sexo biológico?</strong><p>Usado apenas para habilitar cuidados específicos, quando necessário.</p><div className="dependent-choices">{[['feminino','Feminino'],['masculino','Masculino'],['intersexo','Intersexo'],['nao-informado','Prefiro não informar']].map(([value, label]) => <button key={value} className={form.sex === value ? 'selected' : ''} onClick={() => setForm({ ...form, sex: value })}>{label}</button>)}</div></div>}{step === 4 && <div className="dependent-step"><strong>Qual o peso?</strong><p>Você poderá atualizar essa informação depois.</p><div className="unit-input"><input autoFocus type="number" min="0" max="500" value={form.weight} onChange={update('weight')} placeholder="00"/><span>kg</span></div></div>}{step === 5 && <div className="dependent-step"><strong>Qual a altura?</strong><p>Usaremos essa informação apenas para cálculos de saúde.</p><div className="unit-input"><input autoFocus type="number" min="30" max="250" value={form.height} onChange={update('height')} placeholder="000"/><span>cm</span></div></div>}{step === 6 && <div className="dependent-step"><strong>Há alguma alergia?</strong><p>Inclua medicamentos, alimentos ou outras alergias importantes.</p><textarea autoFocus value={form.allergies} onChange={update('allergies')} placeholder="Digite aqui ou escreva ‘Nenhuma’" /></div>}{step === 7 && <div className="dependent-step"><strong>Há alguma condição especial?</strong><p>Compartilhe algo importante para o cuidado dessa pessoa.</p><textarea autoFocus value={form.conditions} onChange={update('conditions')} placeholder="Digite aqui ou escreva ‘Nenhuma’" /></div>}{step === 8 && <div className="dependent-summary"><div><span>Nome</span><strong>{form.name || 'Não informado'}</strong></div><div><span>Vínculo</span><strong>{form.relation || 'Dependente'}</strong></div><div><span>Idade</span><strong>{form.age ? `${form.age} anos` : 'Não informado'}</strong></div><div><span>Sexo</span><strong>{form.sex === 'nao-informado' ? 'Prefiro não informar' : form.sex}</strong></div><div><span>Peso</span><strong>{form.weight ? `${form.weight} kg` : 'Não informado'}</strong></div><div><span>Altura</span><strong>{form.height ? `${form.height} cm` : 'Não informado'}</strong></div></div>}{validation && <small className="form-error">{validation}</small>}<div className="modal-actions"><button className="cancel-btn" onClick={step ? () => setStep(step - 1) : onClose}>{step ? 'Voltar' : 'Cancelar'}</button><button className="primary-btn" disabled={!canContinue} onClick={next}>{step === steps.length - 1 ? 'Criar perfil' : 'Continuar'} <Icon name="arrow" size={15}/></button></div><small className="sync-note">{connection === 'online' ? 'Será sincronizado com o servidor.' : 'Salvo localmente e sincronizado quando o servidor conectar.'}</small></div></div>
}

function DependentHealthStep({ step, steps, form, setForm, onClose, onBack, onNext, connection }) {
  const isAllergy = step === 6
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="modal-card dependent-modal"><button className="modal-close" onClick={onClose}>×</button><div className="modal-icon"><Icon name="heart" size={22}/></div><p className="eyebrow">NOVO PERFIL DE CUIDADO</p><h2>{isAllergy ? 'Você tem alguma alergia?' : 'Acompanhamento de saúde'}</h2><p className="modal-subtitle">Etapa {step + 1} de {steps.length}. Selecione opções ou adicione outras manualmente.</p><div className="dependent-progress">{steps.map((label, index) => <i key={label} className={index < step ? 'done' : index === step ? 'current' : ''} />)}</div><div className="dependent-step dependent-health-step"><HealthTagsInput kind={isAllergy ? 'allergy' : 'condition'} value={isAllergy ? form.allergies : form.conditions} onChange={(value) => setForm({ ...form, [isAllergy ? 'allergies' : 'conditions']: value })}/></div><div className="modal-actions"><button className="cancel-btn" onClick={onBack}>Voltar</button><button className="primary-btn" onClick={onNext}>Continuar <Icon name="arrow" size={15}/></button></div><small className="sync-note">{connection === 'online' ? 'Será sincronizado com o servidor.' : 'Salvo localmente e sincronizado quando o servidor conectar.'}</small></div></div>
}

function ProfileColorModal({ profile, onClose, onSave }) {
  const [selected, setSelected] = useState(getProfileTheme(profile).id)
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="modal-card profile-color-modal"><button className="modal-close" onClick={onClose}>×</button><div className="modal-icon"><Icon name="sun" size={22}/></div><p className="eyebrow">PERSONALIZE SEU PERFIL</p><h2>Escolha uma cor</h2><p className="modal-subtitle">A cor aparece no menu e nos principais destaques enquanto você acompanha {profile?.name || 'este perfil'}.</p><div className="color-choice-grid">{profileThemeOptions.map((option) => <button type="button" key={option.id} className={selected === option.id ? 'color-choice selected' : 'color-choice'} onClick={() => setSelected(option.id)}><span className="color-swatch" style={{ background: option.color }}></span><strong>{option.label}</strong>{selected === option.id && <Icon name="check" size={14}/>}</button>)}</div><div className="modal-actions"><button className="cancel-btn" onClick={onClose}>Cancelar</button><button className="primary-btn" onClick={() => onSave(selected)}>Salvar cor</button></div></div></div>
}

function PrescriptionReview({ action, onClose, onSubmit, pushEnabled }) {
  const medications = Array.isArray(action.result?.medications) ? action.result.medications : []
  const currentDate = new Date()
  const localToday = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`
  const [selected, setSelected] = useState(() => medications.map((_, index) => index))
  const [plans, setPlans] = useState(() => medications.map(() => ({ date: localToday, time: currentDate.toTimeString().slice(0, 5), frequencyType: 'A cada intervalo de horas', frequencyValue: '', durationDays: '', continuous: false, generateAlarms: false })))
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const updatePlan = (index, key, value) => setPlans((current) => current.map((plan, planIndex) => planIndex === index ? { ...plan, [key]: value } : plan))
  const confirm = async () => {
    setSubmitting(true)
    setError('')
    try {
      const selectedMedications = selected.map((index) => {
        const plan = plans[index]
        if (plan.generateAlarms) {
          const frequency = Number(plan.frequencyValue)
          const duration = Number(plan.durationDays)
          if (!plan.date || !plan.time || !Number.isInteger(frequency) || frequency < 1 || frequency > 24 || (!plan.continuous && (!Number.isInteger(duration) || duration < 1 || duration > 3650))) {
            throw new Error(`Informe uma frequência, data, horário e duração válidos para ${medications[index].name || 'o medicamento selecionado'}.`)
          }
        }
        return { ...medications[index], ...plan, clientId: String(index), ...(plan.generateAlarms ? {} : { generateAlarms: false }) }
      })
      await onSubmit({
        kind: 'prescription-confirm',
        title: 'Receita confirmada',
        endpoint: `/prescriptions/${encodeURIComponent(action.result.id)}/confirm`,
        data: { medications: selectedMedications },
      })
    } catch (submitError) {
      setError(submitError.message || 'Não foi possível confirmar a receita.')
    } finally {
      setSubmitting(false)
    }
  }
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="modal-card prescription-review"><button className="modal-close" onClick={onClose}>×</button><div className="modal-icon">✦</div><h2>Revise a sugestão</h2><p className="modal-subtitle">Confira os dados reconhecidos. Nenhum medicamento será salvo até você confirmar.</p>{action.result?.warnings?.length > 0 && <div className="prescription-warnings"><strong>Atenção</strong>{action.result.warnings.map((warning, index) => <p key={index}>{typeof warning === 'string' ? warning : warning.message || JSON.stringify(warning)}</p>)}</div>}<div className="prescription-medications">{medications.length ? medications.map((medication, index) => <div className="prescription-medication-card" key={`${medication.name || 'medication'}-${index}`}><label className="prescription-medication"><input type="checkbox" checked={selected.includes(index)} onChange={() => setSelected((items) => items.includes(index) ? items.filter((item) => item !== index) : [...items, index])}/><span><strong>{medication.name || 'Medicamento sem nome'}</strong><small>{[medication.dose, medication.schedule].filter(Boolean).join(' · ') || 'Confira os detalhes com a receita original.'}</small></span></label>{selected.includes(index) && pushEnabled && <div className="prescription-alarm-plan"><label><input type="checkbox" checked={plans[index].generateAlarms} onChange={(event) => updatePlan(index, 'generateAlarms', event.target.checked)}/>Configurar alarmes para este medicamento</label>{plans[index].generateAlarms && <div className="prescription-alarm-fields"><label>Data inicial<input type="date" value={plans[index].date} onChange={(event) => updatePlan(index, 'date', event.target.value)}/></label><label>Horário inicial<input type="time" value={plans[index].time} onChange={(event) => updatePlan(index, 'time', event.target.value)}/></label><label>Frequência<select value={plans[index].frequencyType} onChange={(event) => updatePlan(index, 'frequencyType', event.target.value)}><option>A cada intervalo de horas</option><option>Vezes ao dia</option></select></label><label>Quantidade (1 a 24)<input type="number" min="1" max="24" step="1" value={plans[index].frequencyValue} onChange={(event) => updatePlan(index, 'frequencyValue', event.target.value)}/></label><label className="prescription-continuous"><input type="checkbox" checked={plans[index].continuous} onChange={(event) => updatePlan(index, 'continuous', event.target.checked)}/>Uso contínuo</label>{!plans[index].continuous && <label>Duração (dias)<input type="number" min="1" max="3650" step="1" value={plans[index].durationDays} onChange={(event) => updatePlan(index, 'durationDays', event.target.value)}/></label>}</div>}</div>}</div>) : <p className="prescription-empty">A análise não sugeriu medicamentos. Você ainda pode confirmar a receita para salvá-la.</p>}{medications.length > 0 && !pushEnabled && <p className="prescription-empty">Para configurar alarmes, ative primeiro as notificações em segundo plano nas Configurações.</p>}</div>{error && <small className="form-error">{error}</small>}<div className="modal-actions"><button className="cancel-btn" disabled={submitting} onClick={onClose}>Cancelar</button><button className="primary-btn" disabled={submitting || (medications.length > 0 && selected.length === 0)} onClick={confirm}>{submitting ? 'Confirmando...' : 'Confirmar seleção'}</button></div></div></div>
}

function ActionModal({ action, onClose, onSubmit, connection, selectedDate, pushEnabled }) {
  const currentTime = new Date().toTimeString().slice(0, 5)
  const initialForm = { name: '', detail: '', date: selectedDate || '', time: currentTime, notes: '', type: action.vitalType || '', value: '', systolic: '', diastolic: '', quality: '', location: '', duration: '', factors: '', impact: '', severity: '', category: '', examType: '', medication: '', flow: '', mood: '', physicalSymptoms: '', unknownDate: false, dosage: '', purpose: '', continuous: false, durationDays: '7', quantity: '30', minimumStock: '5', dosePerTake: '1', unit: 'un', frequencyType: 'A cada intervalo de horas', frequencyValue: '8', generateAlarms: true, symptomKind: 'FISICO', climate: '', nuances: [], intensity: 5, incapacitating: false, ...(action.data || {}) }
  const [form, setForm] = useState(initialForm)
  useEffect(() => setForm({ ...initialForm, ...(action.data || {}) }), [action])
  const [fileName, setFileName] = useState('')
  const [file, setFile] = useState(null)
  const [submissionError, setSubmissionError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const vitalTypeOptions = [
    { label: 'Pressão arterial', icon: 'heart' },
    { label: 'Glicose', icon: 'droplet' },
    { label: 'Batimentos', icon: 'pulse' },
    { label: 'Saturação', icon: 'battery' },
    { label: 'Temperatura', icon: 'thermometer' },
    { label: 'Peso', icon: 'scale' },
    { label: 'Altura', icon: 'ruler' },
  ]
  const isConfirm = action.kind === 'confirm'
  const isAi = action.kind === 'ai'
  const endpoint = action.endpoint || ''
  const title = action.title.toLowerCase()
  const isMedication = endpoint === '/medications' || title.includes('medicamento') || title.includes('remédio')
  const isContraceptiveSetup = isMedication && title.includes('anticoncepcional')
  const isAlarm = endpoint === '/alarms' || title.includes('alarme')
  const isPrescription = endpoint === '/ai/prescription' || endpoint === '/prescriptions' || title.includes('receita')
  const isVitals = endpoint === '/vital-signs' || title.includes('sinal vital') || title.includes('pressão') || title.includes('temperatura')
  const isAppointment = endpoint === '/appointments' || title.includes('consulta')
  const isExam = endpoint === '/exams' || title.includes('exame')
  const isSymptom = endpoint === '/symptoms' || (title.includes('sintoma') && !endpoint.startsWith('/feminine-health') && !title.includes('ciclo') && !title.includes('fluxo') && !title.includes('anticoncepcional'))
  const isFemale = !isMedication && (endpoint === '/feminine-health' || endpoint.startsWith('/feminine-health/') || title.includes('saúde feminina') || title.includes('ciclo') || title.includes('sintoma do ciclo') || title.includes('anticoncepcional'))
  const isCycleEnd = isFemale && (title.includes('fim de ciclo') || Boolean(form.endDate))
  const isFemaleDailyLog = isFemale && !isCycleEnd && !title.includes('início de ciclo') && !title.includes('tomada de anticoncepcional')
  const isVaccine = endpoint === '/vaccines' || title.includes('vacina')
  const specialized = isMedication || isAlarm || isPrescription || isVitals || isAppointment || isExam || isSymptom || isFemale || isVaccine
  const vitalType = form.type || action.vitalType || ''
  const vitalValue = vitalType === 'Pressão arterial' ? form.systolic && form.diastolic : form.value
  const symptomReady = form.symptomKind === 'FISICO' ? form.quality : form.climate
  const femaleValue = isCycleEnd ? form.endDate : title.includes('início de ciclo') ? form.date : title.includes('tomada de anticoncepcional') ? form.date && form.time : form.date && [form.flow, form.physicalSymptoms, form.mood, form.notes].some((value) => String(value || '').trim())
  const requiredValue = isMedication ? isContraceptiveSetup ? form.name && form.date && form.time && form.contraceptivePackType : form.name : isAlarm ? form.name && form.date && form.time : isPrescription ? (isAi ? file : form.name) : isVitals ? vitalType && vitalValue : isAppointment ? form.name : isExam ? form.name : isSymptom ? symptomReady : isFemale ? femaleValue : isVaccine ? form.name : form.name
  const isFutureVitals = isVitals && form.date && form.time && new Date(`${form.date}T${form.time}`).getTime() > Date.now()
  const isFutureAlarmInvalid = isAlarm && form.date && form.time && new Date(`${form.date}T${form.time}`).getTime() <= Date.now()
  const medicationFrequency = Number(form.frequencyValue)
  const medicationStart = new Date(`${form.date}T${form.time}`)
  const medicationScheduleInvalid = isMedication && form.generateAlarms && (!form.date || !form.time || Number.isNaN(medicationStart.getTime()) || (!isContraceptiveSetup && medicationStart.getTime() <= Date.now()) || !Number.isInteger(medicationFrequency) || medicationFrequency < 1 || medicationFrequency > 24 || (!form.continuous && (!Number.isInteger(Number(form.durationDays)) || Number(form.durationDays) < 1 || Number(form.durationDays) > 3650)))
  const today = dateKeyForFemaleHealth(new Date())
  const femaleDateError = isCycleEnd
    ? form.endDate > today ? 'O último dia do fluxo não pode ser uma data futura.' : form.date && form.endDate < form.date ? 'O último dia do fluxo não pode ser anterior ao início.' : ''
    : form.date > today ? 'Escolha uma data de hoje ou anterior.' : ''
  const femaleDateInvalid = isFemale && Boolean(femaleDateError)
  const canSubmit = isConfirm || (Boolean(requiredValue) && !isFutureVitals && !isFutureAlarmInvalid && !medicationScheduleInvalid && !femaleDateInvalid)
  const update = (key) => (event) => setForm(key === 'type' ? { ...form, type: event.target.value, value: '', systolic: '', diastolic: '' } : { ...form, [key]: event.target.value })
  const submit = async (nextAction) => {
    setSubmitting(true)
    setSubmissionError('')
    try {
      await onSubmit(nextAction)
    } catch (error) {
      setSubmissionError(error.message || 'Não foi possível salvar. Tente novamente.')
    } finally {
      setSubmitting(false)
    }
  }
  const submittedForm = isContraceptiveSetup ? (() => {
    const pattern = getContraceptivePattern(form.contraceptivePackType)
    return {
      ...form,
      purpose: 'Anticoncepcional',
      dosage: form.contraceptivePackType === 'continuous' ? 'Uso contínuo' : `Cartela ${form.contraceptivePackType} dias`,
      continuous: true,
      frequencyType: 'Vezes ao dia',
      frequencyValue: '1',
      contraceptiveActiveDays: pattern?.activeDays,
      contraceptivePauseDays: pattern?.pauseDays,
    }
  })() : form
  const fields = []
  if (isContraceptiveSetup) fields.push({ key: 'name', label: 'Nome do anticoncepcional', placeholder: 'Ex.: Selene', required: true }, { key: 'contraceptivePackType', label: 'Padrão da cartela', type: 'contraceptive-pack', required: true }, { key: 'date', label: 'Data de início da cartela', type: 'date', required: true }, { key: 'time', label: 'Horário diário da tomada', type: 'time', required: true }, { key: 'generateAlarms', label: 'Criar lembrete diário', type: 'checkbox' })
  else if (isMedication) fields.push({ key: 'name', label: 'Nome do remédio', placeholder: 'Ex.: Losartana 50 mg', required: true }, { key: 'dosage', label: 'Dosagem', placeholder: 'Ex.: 1 comprimido' }, { key: 'purpose', label: 'Finalidade', placeholder: 'Ex.: Controle da pressão' }, { key: 'date', label: 'Data inicial', type: 'date' }, { key: 'time', label: 'Horário inicial', type: 'time' }, { key: 'frequencyType', label: 'Frequência', type: 'select', options: ['A cada intervalo de horas', 'Vezes ao dia'] }, { key: 'frequencyValue', label: 'Quantidade da frequência (1 a 24)', type: 'number', min: 1, max: 24, step: 1, placeholder: 'Ex.: 8' }, { key: 'continuous', label: 'Uso contínuo', type: 'checkbox' }, ...(form.continuous ? [] : [{ key: 'durationDays', label: 'Duração (dias)', type: 'number', min: 1, max: 3650, step: 1, placeholder: 'Ex.: 7' }]), { key: 'quantity', label: 'Quantidade em estoque', type: 'number', placeholder: 'Ex.: 30' }, { key: 'minimumStock', label: 'Estoque mínimo', type: 'number', placeholder: 'Ex.: 5' }, { key: 'dosePerTake', label: 'Doses por tomada', type: 'number', placeholder: 'Ex.: 1' }, { key: 'unit', label: 'Unidade de medida', placeholder: 'Ex.: comprimidos, ml, gotas' }, { key: 'generateAlarms', label: 'Criar alarmes automaticamente', type: 'checkbox' })
  if (isAlarm) fields.push({ key: 'name', label: 'Título / descrição', placeholder: 'Ex.: Tomar remédio', required: true }, { key: 'medication', label: 'Medicamento vinculado', placeholder: 'Opcional' }, { key: 'date', label: 'Data', type: 'date' }, { key: 'time', label: 'Horário', type: 'time' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  if (isPrescription && !isAi) fields.push({ key: 'name', label: 'Título da receita', placeholder: 'Ex.: Receita de uso contínuo', required: true })
  if (isPrescription) fields.push({ key: 'date', label: 'Data da receita', type: 'date' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  if (isVitals) {
    fields.push({ key: 'type', label: 'Tipo de registro', type: 'vital-type', required: true })
    const vitalFields = {
      'Pressão arterial': [{ key: 'systolic', label: 'Sistólica (mmHg)', placeholder: 'Ex.: 120', required: true }, { key: 'diastolic', label: 'Diastólica (mmHg)', placeholder: 'Ex.: 80', required: true }],
      Glicose: [{ key: 'value', label: 'Glicose (mg/dL)', placeholder: 'Ex.: 95', required: true }],
      Batimentos: [{ key: 'value', label: 'Batimentos (BPM)', placeholder: 'Ex.: 72', required: true }],
      Saturação: [{ key: 'value', label: 'Saturação (%)', placeholder: 'Ex.: 98', required: true }],
      Temperatura: [{ key: 'value', label: 'Temperatura (°C)', placeholder: 'Ex.: 36,5', required: true }],
      Peso: [{ key: 'value', label: 'Peso (kg)', placeholder: 'Ex.: 70,5', required: true }],
      Altura: [{ key: 'value', label: 'Altura (cm)', placeholder: 'Ex.: 170', required: true }],
    }
    fields.push(...(vitalFields[vitalType] || []), { key: 'date', label: 'Data', type: 'date' }, { key: 'time', label: 'Hora', type: 'time' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  }
  if (isAppointment) fields.push({ key: 'name', label: 'Nome do médico', placeholder: 'Ex.: Dra. Ana Silva', required: true }, { key: 'detail', label: 'Especialidade', placeholder: 'Ex.: Cardiologia' }, { key: 'location', label: 'Local / clínica' }, { key: 'date', label: 'Data', type: 'date' }, { key: 'time', label: 'Hora', type: 'time' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  if (isExam) fields.push({ key: 'name', label: 'Nome do exame', placeholder: 'Ex.: Hemograma', required: true }, { key: 'category', label: 'Categoria', type: 'select', options: ['Outros', 'Laboratório', 'Imagem', 'Cardiologia', 'Endoscopia'] }, { key: 'examType', label: 'Tipo de resultado', type: 'select', options: ['Imagem única', 'Série temporal', 'Resultado numérico'] }, { key: 'date', label: 'Data do exame', type: 'date' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  if (isSymptom) fields.push({ key: 'date', label: 'Data do registro', type: 'date' })
  if (isFemale && isCycleEnd) fields.push({ key: 'endDate', label: 'Último dia do fluxo', type: 'date', required: true })
  else if (isFemale && title.includes('início de ciclo')) fields.push({ key: 'date', label: 'Data de início do fluxo', type: 'date', required: true }, { key: 'flow', label: 'Intensidade do fluxo', type: 'select', options: ['Leve', 'Média', 'Intensa'] }, { key: 'physicalSymptoms', label: 'Sintomas ou observações', type: 'textarea' })
  else if (isFemale && title.includes('tomada de anticoncepcional')) fields.push({ key: 'date', label: 'Data', type: 'date', required: true }, { key: 'time', label: 'Horário tomado', type: 'time', required: true }, { key: 'taken', label: 'Tomei a dose', type: 'checkbox' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  else if (isFemale) fields.push({ key: 'date', label: 'Data do registro', type: 'date', required: true }, { key: 'flow', label: 'Intensidade do fluxo', type: 'select', options: ['Sem fluxo', 'Leve', 'Média', 'Intensa'] }, { key: 'physicalSymptoms', label: 'Sintomas físicos', type: 'textarea' }, { key: 'mood', label: 'Humor', type: 'select', options: ['Estável', 'Triste', 'Ansiosa', 'Irritada', 'Disposta'] }, { key: 'notes', label: 'Observações', type: 'textarea' })
  if (isVaccine) fields.push({ key: 'name', label: 'Nome da vacina', placeholder: 'Ex.: Influenza', required: true }, { key: 'date', label: 'Data em que foi tomada', type: 'date' }, { key: 'unknownDate', label: 'Não sei a data exata', type: 'checkbox' }, { key: 'notes', label: 'Observações (lote, local etc.)', type: 'textarea' })
  if (!specialized && !isAi) fields.push({ key: 'name', label: 'Nome ou título', placeholder: 'Digite um nome', required: true }, { key: 'detail', label: 'Detalhes', placeholder: 'Digite os detalhes' }, { key: 'date', label: 'Data', type: 'date' }, { key: 'notes', label: 'Observações', type: 'textarea' })
  if (action.kind === 'ai-review') return <PrescriptionReview action={action} onClose={onClose} onSubmit={onSubmit} pushEnabled={pushEnabled}/>
  if (action.kind === 'success') return <div className="modal-backdrop"><div className="modal-card success-modal"><div className="success-check"><Icon name="check" size={27}/></div><h2>{action.title}</h2><p>{action.message || 'A informação foi salva com sucesso no seu espaço de saúde.'}</p><button className="primary-btn modal-primary" onClick={onClose}>Continuar</button></div></div>
  if (endpoint === '/profiles' && !isConfirm && !isAi) return <><DependentModal action={action} onClose={onClose} onSubmit={submit} connection={connection}/>{submissionError && <div className="submission-error-toast" role="alert">{submissionError}</div>}</>
  if (isSymptom && !isConfirm && !isAi) return <><SymptomModal action={action} form={form} setForm={setForm} onClose={onClose} onSubmit={submit} connection={connection} canSubmit={Boolean(symptomReady)}/>{submissionError && <div className="submission-error-toast" role="alert">{submissionError}</div>}</>
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <div className="modal-card">
      <button className="modal-close" onClick={onClose}>×</button>
      <div className="modal-icon">{isAi ? '✦' : isConfirm ? '!' : <Icon name="plus" size={22}/>}</div>
      <h2>{action.title}</h2>
      <p className="modal-subtitle">{isConfirm ? action.message : isAi ? 'A IA analisa o arquivo e sugere os dados da receita. Você revisa tudo antes de salvar.' : isContraceptiveSetup ? 'Cadastre este método separado dos medicamentos comuns. Escolha o padrão da cartela para acompanhar dias de uso e pausa.' : 'Preencha os dados abaixo. Você poderá editar essas informações depois.'}</p>
      {isAi && <div className="upload-zone"><div className="upload-icon"><Icon name="file" size={22}/></div><strong>{fileName || 'Arraste uma receita aqui'}</strong><span>ou escolha um arquivo PDF, JPG ou PNG</span><label className="soft-btn">Escolher arquivo<input className="file-input" type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={(event) => { const selected = event.target.files?.[0] || null; setFile(selected); setFileName(selected?.name || '') }} /></label><small>Seus documentos são protegidos e privados.</small></div>}
      {isConfirm ? <div className="confirm-warning">Você poderá desfazer esta remoção logo após confirmar.</div> : !isAi && <div className={`modal-form${isContraceptiveSetup ? ' contraceptive-form' : ''}`}>{fields.map((field) => field.type === 'textarea' ? <label key={field.key}>{field.label}<textarea value={form[field.key] || ''} onChange={update(field.key)} placeholder={field.placeholder || 'Opcional'} /></label> : field.type === 'vital-type' ? <fieldset className="vital-type-field" key={field.key}><legend>{field.label}</legend><div className="vital-type-grid" role="group" aria-label={field.label}>{vitalTypeOptions.map((option) => <button key={option.label} type="button" className={`vital-type-option${vitalType === option.label ? ' selected' : ''}`} aria-pressed={vitalType === option.label} onClick={() => setForm({ ...form, type: option.label, value: '', systolic: '', diastolic: '' })}><Icon name={option.icon} size={21}/><span>{option.label}</span></button>)}</div></fieldset> : field.type === 'contraceptive-pack' ? <fieldset className="contraceptive-pack-field" key={field.key}><legend>{field.label}</legend><div className="contraceptive-pack-options" role="group" aria-label={field.label}>{[['21','21 dias','21 dias de uso e 7 dias de pausa'],['24','24 dias','24 dias de uso e 4 dias de pausa'],['28','28 dias','Sem pausa entre cartelas'],['continuous','Contínuo','Sem pausa programada']].map(([value,label,description]) => <button type="button" key={value} className={`contraceptive-pack-option${form.contraceptivePackType === value ? ' selected' : ''}`} aria-pressed={form.contraceptivePackType === value} onClick={() => setForm({ ...form, contraceptivePackType: value })}><strong>{label}</strong><small>{description}</small></button>)}</div><small className="contraceptive-pack-note">Confirme o esquema indicado para o anticoncepcional que você usa. O lembrete diário é suspenso durante a pausa configurada.</small></fieldset> : field.type === 'select' ? <label key={field.key}>{field.label}<select value={form[field.key] || ''} onChange={update(field.key)}><option value="">Selecione</option>{field.options.map((option) => <option key={option} value={option}>{option}</option>)}</select></label> : field.type === 'checkbox' ? <label className="check-field" key={field.key}><input type="checkbox" checked={Boolean(form[field.key])} onChange={(event) => setForm({ ...form, [field.key]: event.target.checked })}/>{field.label}</label> : <label key={field.key}>{field.label}<input type={field.type || 'text'} value={form[field.key] || ''} onChange={update(field.key)} placeholder={field.placeholder || ''} required={field.required} min={field.min} max={field.max} step={field.step}/></label>)}</div>}
      {isAi && <div className="modal-form"><label>Data da receita<input type="date" value={form.date} onChange={update('date')} /></label><label>Observações<textarea value={form.notes} onChange={update('notes')} placeholder="Opcional" /></label></div>}
      {submissionError && <small className="form-error">{submissionError}</small>}
      {!canSubmit && <small className="form-error">{isFutureVitals ? 'Não é possível registrar sinais vitais no futuro.' : isFutureAlarmInvalid ? 'Escolha uma data e horário futuros para o alarme.' : medicationScheduleInvalid ? 'Para gerar alarmes, informe um início futuro, frequência de 1 a 24 e uma duração válida.' : femaleDateInvalid ? femaleDateError : isFemaleDailyLog && !femaleValue ? 'Registre pelo menos o fluxo, um sintoma, o humor ou uma observação.' : isAi ? 'Escolha um arquivo para iniciar a análise.' : 'Preencha os campos obrigatórios para continuar.'}</small>}
      <div className="modal-actions"><button className="cancel-btn" disabled={submitting} onClick={onClose}>Cancelar</button><button className={isConfirm ? 'danger-btn' : 'primary-btn'} disabled={!canSubmit || submitting} onClick={() => submit({ ...action, data: isConfirm ? action.data : isAi ? { ...form, fileName, file } : isFemale && form.recordType === 'menstrual-cycle' ? { ...form, ...(isCycleEnd ? { date: form.endDate } : { startDate: form.date }) } : submittedForm })}>{submitting ? 'Salvando...' : isConfirm ? 'Confirmar' : isAi ? 'Enviar para análise' : isContraceptiveSetup ? 'Salvar anticoncepcional' : 'Salvar registro'}</button></div>
      <small className="sync-note">{connection === 'guest' ? 'Salvo somente neste dispositivo. Crie uma conta para backup e sincronização.' : connection === 'online' ? 'Será sincronizado com o servidor.' : 'Salvo localmente e sincronizado quando o servidor conectar.'}</small>
    </div>
  </div>
}

function ConnectionBadge({ status }) {
  const label = status === 'guest' ? 'Modo Guest — somente neste dispositivo' : status === 'demo' ? 'Demonstração local' : status === 'online' ? 'Servidor conectado' : status === 'checking' ? 'Verificando conexão' : 'Modo offline'
  return <span className={`connection-badge ${status}`}><i></i>{label}</span>
}

function SyncBadge({ connection, pendingCount, error = '' }) {
  const pending = pendingCount > 0
  const syncing = connection === 'checking'
  const label = error || (connection === 'guest' ? 'Seus registros estão salvos somente neste dispositivo. Crie uma conta para ter backup.' : connection === 'demo' ? 'Dados fictícios salvos neste navegador' : syncing ? 'Verificando sincronização' : pending ? `${pendingCount} ${pendingCount === 1 ? 'dado pendente' : 'dados pendentes'} neste dispositivo` : 'Dados sincronizados')
  return <span className={`sync-badge ${syncing ? 'checking' : pending ? 'pending' : 'synced'}`} title={label} aria-label={label}><Icon name={connection === 'guest' ? 'file' : syncing || pending ? 'cloud' : 'check'} size={15}/>{pending && <b>{pendingCount > 99 ? '99+' : pendingCount}</b>}</span>
}

function hasGuestRecords() {
  try {
    const data = JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
    return guestCollections.some((collection) => Array.isArray(data[collection]) && data[collection].length > 0)
  } catch {
    return false
  }
}

function AuthPage({ view, onViewChange, onSubmit, onGuestLogin, loading, error }) {
  const [form, setForm] = useState({ name: '', email: '', password: '', passwordConfirmation: '' })
  const [migrateGuestData, setMigrateGuestData] = useState(false)
  const isLanding = view === 'landing'
  const isRegister = view === 'register'
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value })
  const submit = (event) => { event.preventDefault(); onSubmit({ ...form, migrateGuestData }, view) }
  if (isLanding) return <main className="auth-page"><div className="auth-brand"><Brand /></div><div className="auth-layout"><section className="auth-hero"><p className="eyebrow">CUIDADO QUE ACOMPANHA VOCÊ</p><h1>Um jeito mais simples de cuidar da sua saúde.</h1><p>Organize medicamentos, sinais vitais, consultas e documentos em um espaço feito para você e para quem depende do seu cuidado.</p><div className="auth-highlights"><span><Icon name="check" size={15}/> Seus dados organizados</span><span><Icon name="cloud" size={15}/> Funciona mesmo sem internet</span><span><Icon name="heart" size={15}/> Um perfil para cada pessoa</span></div></section><section className="auth-card"><div className="auth-mark">✦</div><h2>Bem-vindo ao Katu’I</h2><p>Entre na sua conta, crie um espaço de cuidado ou experimente sem cadastro.</p><button className="primary-btn auth-main-button" onClick={() => onViewChange('login')}>Entrar na minha conta <Icon name="arrow" size={16}/></button><button className="outline-btn auth-secondary-button" onClick={() => onViewChange('register')}>Criar minha conta</button><button className="outline-btn guest-login-button" onClick={onGuestLogin}>Continuar como Guest</button><small>No modo Guest, os registros ficam somente neste dispositivo e não há acesso à IA nem a backup na nuvem.</small></section></div></main>
  return <main className="auth-page"><div className="auth-brand"><Brand /></div><div className="auth-form-wrap"><section className="auth-card auth-form-card"><button className="auth-back" onClick={() => onViewChange('landing')}>← Voltar</button><div className="auth-mark small-mark">✦</div><h2>{isRegister ? 'Criar sua conta' : 'Entrar no Katu’I'}</h2><p>{isRegister ? 'Crie seu acesso para começar seu espaço de cuidado.' : 'Acesse seus perfis e registros de saúde.'}</p><form onSubmit={submit}>{isRegister && <label>Nome completo<input autoFocus value={form.name} onChange={update('name')} placeholder="Como podemos chamar você?" required minLength={2}/></label>}<label>E-mail<input autoFocus={!isRegister} type="email" value={form.email} onChange={update('email')} placeholder="voce@exemplo.com" required autoComplete={isRegister ? 'email' : 'username'}/></label><label>Senha<input type="password" value={form.password} onChange={update('password')} placeholder="Mínimo de 8 caracteres" required minLength={8} autoComplete={isRegister ? 'new-password' : 'current-password'}/></label>{isRegister && <label>Confirme sua senha<input type="password" value={form.passwordConfirmation} onChange={update('passwordConfirmation')} placeholder="Digite a senha novamente" required minLength={8} autoComplete="new-password"/></label>}{hasGuestRecords() && isGuestMode() && <label className="check-field guest-migration-choice"><input type="checkbox" checked={migrateGuestData} onChange={(event) => setMigrateGuestData(event.target.checked)}/>Importar meus registros Guest para esta conta. Essa ação enviará seus dados de saúde ao servidor.</label>}{error && <small className="auth-error">{error}</small>}<button className="primary-btn auth-submit" disabled={loading}>{loading ? 'Aguarde...' : isRegister ? 'Criar conta' : 'Entrar'}</button></form><button className="auth-switch" onClick={() => onViewChange(isRegister ? 'login' : 'register')}>{isRegister ? 'Já tenho uma conta' : 'Ainda não tenho uma conta'}</button></section></div></main>
}

function Onboarding({ step, setStep, form, setForm, onFinish, onSkip, darkMode, error = '', isGuest = false }) {
  const steps = ['Boas-vindas', 'Seu nome', 'Sua idade', 'Sexo biológico', 'Seu peso', 'Sua altura', 'Alergias', 'Condições especiais', 'IA opcional', 'Tudo pronto']
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value })
  const isLast = step === steps.length - 1
  const validationMessage = step === 1 && form.name.trim().length < 2 ? 'Informe pelo menos 2 caracteres.' : step === 2 && form.age && (Number(form.age) < 0 || Number(form.age) > 120) ? 'Informe uma idade entre 0 e 120 anos.' : step === 4 && form.weight && (Number(form.weight) <= 0 || Number(form.weight) > 500) ? 'Informe um peso válido.' : step === 5 && form.height && (Number(form.height) < 30 || Number(form.height) > 250) ? 'Informe uma altura entre 30 e 250 cm.' : ''
  const canContinue = !validationMessage && (step !== 1 || form.name.trim().length > 1)
  const next = () => canContinue && (isLast ? onFinish() : setStep(step + 1))
  const back = () => setStep(Math.max(0, step - 1))
  return <div className={darkMode ? 'onboarding dark-mode' : 'onboarding'}><div className="onboarding-top"><div className="onboarding-brand"><Brand /></div>{step > 0 && step < steps.length - 1 && <span className="onboarding-counter">{step} de {steps.length - 2}</span>}{!isGuest && <button className="onboarding-skip" onClick={onSkip}>Pular por agora</button>}</div><div className="onboarding-body"><div className="onboarding-progress">{steps.slice(1, -1).map((label, index) => <i key={label} className={step > index + 1 ? 'done' : step === index + 1 ? 'current' : ''}></i>)}</div>{step === 0 && <div className="onboarding-content welcome-onboarding"><div className="onboarding-mark">✦</div><p className="eyebrow">SEU ESPAÇO DE CUIDADO</p><h1>Olá! Vamos cuidar da sua saúde juntos?</h1><p>Para te ajudar melhor, precisamos conhecer um pouco sobre você. É rápido, seguro e você pode alterar tudo depois.</p><button className="primary-btn onboarding-main-btn" onClick={() => setStep(1)}>Começar <Icon name="arrow" size={17}/></button></div>}{step === 1 && <OnboardingField eyebrow="SOBRE VOCÊ" title="Como podemos te chamar?" description="Escreva seu nome para personalizar sua experiência." input={<input autoFocus value={form.name} onChange={update('name')} placeholder="Digite seu nome" />}/>} {step === 2 && <OnboardingField eyebrow="SOBRE VOCÊ" title="Qual a sua idade?" description="Essa informação ajuda a personalizar alguns cuidados." input={<div className="unit-input"><input autoFocus type="number" min="0" max="120" value={form.age} onChange={update('age')} placeholder="00"/><span>anos</span></div>}/>} {step === 3 && <div className="onboarding-content"><p className="eyebrow">SOBRE VOCÊ</p><h1>Qual seu sexo biológico?</h1><p>Isso ajuda a habilitar funções específicas, como saúde feminina.</p><div className="choice-grid">{[['feminino','Feminino','♀'],['masculino','Masculino','♂'],['intersexo','Intersexo','⚥'],['nao-informado','Prefiro não informar','—']].map(([value,label,sign]) => <button key={value} className={form.sex === value ? 'choice-card selected' : 'choice-card'} onClick={() => setForm({ ...form, sex: value })}><span>{sign}</span><strong>{label}</strong></button>)}</div></div>} {step === 4 && <OnboardingField eyebrow="MEDIDAS DE SAÚDE" title="Qual o seu peso?" description="Você poderá atualizar essa informação sempre que quiser." input={<div className="unit-input"><input autoFocus type="number" min="0" max="500" value={form.weight} onChange={update('weight')} placeholder="00"/><span>kg</span></div>}/>} {step === 5 && <OnboardingField eyebrow="MEDIDAS DE SAÚDE" title="Qual a sua altura?" description="Usaremos essa informação apenas para cálculos de saúde." input={<div className="unit-input"><input autoFocus type="number" min="30" max="250" value={form.height} onChange={update('height')} placeholder="000"/><span>cm</span></div>}/>} {step === 6 && <OnboardingField eyebrow="SEU CUIDADO" title="Você tem alguma alergia?" description="Inclua medicamentos, alimentos ou outras alergias importantes. Você também pode deixar para depois." input={<textarea autoFocus value={form.allergies} onChange={update('allergies')} placeholder="Digite aqui ou escreva ‘Nenhuma’"/>}/>} {step === 7 && <OnboardingField eyebrow="SEU CUIDADO" title="Há alguma condição especial?" description="Compartilhe algo que devemos considerar para cuidar melhor de você." input={<textarea autoFocus value={form.conditions} onChange={update('conditions')} placeholder="Digite aqui ou escreva ‘Nenhuma’"/>}/>} {step === 8 && (isGuest ? <div className="onboarding-content"><p className="eyebrow">MODO GUEST</p><h1>IA indisponível sem uma conta</h1><p>Seus registros permanecem neste dispositivo. Crie uma conta para acessar os recursos de inteligência artificial.</p></div> : <div className="onboarding-content"><p className="eyebrow">UM RECURSO OPCIONAL</p><h1>Quer usar a IA do Katu’I?</h1><p>Ela pode ajudar a organizar receitas e documentos. Você poderá ativá-la ou desativá-la quando quiser.</p><button className={form.aiEnabled ? 'choice-card wide selected' : 'choice-card wide'} onClick={() => setForm({ ...form, aiEnabled: true })}><span>✦</span><strong>Sim, quero experimentar</strong><small>Leitura inteligente de receitas</small></button><button className={!form.aiEnabled ? 'choice-card wide selected' : 'choice-card wide'} onClick={() => setForm({ ...form, aiEnabled: false })}><span>○</span><strong>Agora não</strong><small>Você pode mudar essa escolha depois</small></button></div>)} {step === 9 && <div className="onboarding-content"><div className="onboarding-success">✓</div><p className="eyebrow">TUDO PRONTO</p><h1>Seu espaço está preparado.</h1><p>Confira seus dados. Você poderá editar qualquer informação nas configurações.</p><div className="onboarding-summary"><div><span>Nome</span><strong>{form.name || 'Não informado'}</strong></div><div><span>Idade</span><strong>{form.age ? `${form.age} anos` : 'Não informado'}</strong></div><div><span>Sexo biológico</span><strong>{form.sex === 'nao-informado' ? 'Prefiro não informar' : form.sex || 'Não informado'}</strong></div><div><span>IA de receitas</span><strong>{form.aiEnabled ? 'Ativada' : 'Desativada'}</strong></div></div></div>}</div>{validationMessage && <small className="onboarding-error">{validationMessage}</small>}{error && <small className="onboarding-error">{error}</small>}<div className="onboarding-footer">{step > 0 && <button className="back-btn" onClick={back}>Voltar</button>}{step > 0 && <button className="primary-btn onboarding-next" disabled={!canContinue} onClick={next}>{isLast ? 'Entrar no Katu’I' : 'Continuar'} <Icon name="arrow" size={16}/></button>}</div></div>
}

function HealthTagsInput({ value, onChange, kind }) {
  const emptyLabel = kind === 'allergy' ? 'Nenhuma' : 'Nenhum'
  const suggestions = kind === 'allergy' ? ['Penicilina', 'Dipirona', 'AAS', 'Leite', 'Amendoim', 'Nenhuma'] : ['Diabetes', 'Hipertensão', 'Asma', 'Cardiopatia', 'Epilepsia', 'Nenhum']
  const [draft, setDraft] = useState('')
  const tags = String(value || '').split(',').map((item) => item.trim()).filter((item) => item && item !== emptyLabel)
  const setTags = (next) => onChange(next.length ? next.join(', ') : emptyLabel)
  const add = (item) => { const clean = item.trim(); if (!clean) return; if (clean === emptyLabel) return setTags([]); if (!tags.includes(clean)) setTags([...tags, clean]); setDraft('') }
  return <div className="health-tags-input"><div className="health-tags-list">{tags.map((tag) => <button type="button" key={tag} className="health-tag" onClick={() => setTags(tags.filter((item) => item !== tag))}>{tag}<span>×</span></button>)}{!tags.length && <span className="health-tags-empty">Nenhum item selecionado</span>}</div><div className="health-tags-entry"><input value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ',') { event.preventDefault(); add(draft) } }} placeholder="Digite e pressione Enter"/><button type="button" onClick={() => add(draft)}>Adicionar</button></div><div className="health-suggestions">{suggestions.map((suggestion) => <button type="button" key={suggestion} className={tags.includes(suggestion) ? 'selected' : ''} onClick={() => suggestion === emptyLabel ? setTags([]) : add(suggestion)}>{suggestion}</button>)}</div><small>Você pode adicionar mais de uma opção e tocar em uma tag para removê-la.</small></div>
}

function OnboardingField({ eyebrow, title, description, input }) {
  const healthKind = title === 'Você tem alguma alergia?' ? 'allergy' : title === 'Há alguma condição especial?' ? 'condition' : null
  const healthInput = healthKind ? <HealthTagsInput kind={healthKind} value={input?.props?.value || ''} onChange={(value) => input?.props?.onChange?.({ target: { value } })}/> : input
  return <div className="onboarding-content"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p>{description}</p><div className="onboarding-input">{healthInput}</div></div>
}

function DashboardSummary({ profile, onAction, onNavigate, selectedDate, onDateSelect, calendarExpanded, onCalendarExpandedChange, calendarRecords, calendarLoadError }) {
  const [overview, setOverview] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const calendarRef = useRef(null)
  useEffect(() => {
    let mounted = true
    setLoadError('')
    apiRequest('/overview').then((result) => {
      if (!result || typeof result !== 'object' || Array.isArray(result)) throw new Error('A API retornou um resumo inválido.')
      if (mounted) setOverview({
        medications: Array.isArray(result.medications) ? result.medications : [],
        vitalSigns: Array.isArray(result.vitalSigns) ? result.vitalSigns : [],
        appointments: Array.isArray(result.appointments) ? result.appointments : [],
        symptoms: Array.isArray(result.symptoms) ? result.symptoms : [],
      })
    }).catch((error) => {
      if (mounted) setLoadError(error.message || 'Não foi possível carregar o resumo.')
    })
    const refresh = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (['/overview', '/medications', '/vital-signs', '/appointments', '/symptoms'].some((resource) => endpoint === resource || endpoint.startsWith(`${resource}/`))) {
        setRefreshKey((value) => value + 1)
      }
    }
    window.addEventListener('katui:resource-change', refresh)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', refresh)
    }
  }, [profile?.id, refreshKey])
  const medications = overview?.medications || []
  const vitalSigns = overview?.vitalSigns || []
  const appointments = overview?.appointments || []
  const symptoms = overview?.symptoms || []
  const medicationsForDate = medications.filter((item) => recordOccursOnDate(item, selectedDate, 'medications'))
  const vitalSignsForDate = vitalSigns.filter((item) => recordOccursOnDate(item, selectedDate, 'vitalSigns'))
  const appointmentsForDate = appointments.filter((item) => recordOccursOnDate(item, selectedDate, 'appointments'))
  const symptomsForDate = symptoms.filter((item) => recordOccursOnDate(item, selectedDate, 'symptoms'))
  const appointmentForDate = appointmentsForDate
    .sort((left, right) => `${left.date} ${left.time || ''}`.localeCompare(`${right.date} ${right.time || ''}`))[0]
  const medication = medicationsForDate[0]
  const formattedDate = (value) => {
    if (!value) return ''
    const date = new Date(`${value.slice(0, 10)}T00:00:00`)
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
  }
  const count = (items) => overview ? items.length : '—'
  return <>
    {loadError && <p className="form-error" role="status">{loadError}</p>}
    <CalendarCard selectedDate={selectedDate} onDateSelect={onDateSelect} records={calendarRecords} expanded={calendarExpanded} onExpandedChange={onCalendarExpandedChange} cardRef={calendarRef} loadError={calendarLoadError} />
    <section className="metrics-grid"><article className="metric-card"><div className="metric-icon teal-bg"><Icon name="pill" size={20}/></div><div><span>Medicamentos</span><strong>{count(medicationsForDate)} <small>cadastrados</small></strong><button className="mini-link" onClick={() => onNavigate('Medicamentos')}>Ver medicamentos <Icon name="arrow" size={13}/></button></div><span className="metric-arrow"><Icon name="chevron" size={17}/></span></article><article className="metric-card"><div className="metric-icon rose-bg"><Icon name="pulse" size={20}/></div><div><span>Sinais vitais</span><strong>{overview ? vitalSignsForDate.length || 'Nenhum registro' : '—'}</strong><button className="mini-link" onClick={() => onNavigate('Sinais vitais')}>Ver registros <Icon name="arrow" size={13}/></button></div><span className="metric-arrow"><Icon name="chevron" size={17}/></span></article><article className="metric-card"><div className="metric-icon blue-bg"><Icon name="calendar" size={20}/></div><div><span>Consulta nesta data</span><strong>{appointmentForDate?.name || (overview ? 'Nenhuma agendada' : '—')}</strong><button className="mini-link" onClick={() => onNavigate('Consultas')}>{appointmentForDate ? formattedDate(appointmentForDate.date) : 'Ver consultas'} <Icon name="arrow" size={13}/></button></div><span className="metric-arrow"><Icon name="chevron" size={17}/></span></article></section>
    <section className="dashboard-grid"><div className="panel medication-panel"><div className="panel-header"><div><h3>Próximos medicamentos</h3><p>{overview ? 'Tratamentos deste perfil' : 'Carregando seus dados...'}</p></div></div>{medication ? <div className="resource-row"><div className="resource-icon"><Icon name="pill" size={18}/></div><div className="resource-main"><strong>{medication.name || 'Medicamento'}</strong><span>{medication.time ? `Próximo horário: ${medication.time}` : medication.dosage || medication.dose || 'Tratamento ativo'}</span></div></div> : overview ? <EmptyState icon="pill" title="Nenhum medicamento nesta data" message="Escolha outro dia ou adicione um medicamento." action="Adicionar medicamento" onAction={() => onNavigate('Medicamentos')}/> : null}</div><div className="panel week-panel"><div className="panel-header"><div><h3>Seu bem-estar</h3><p>Registros de sintomas nesta data</p></div></div>{overview && symptomsForDate.length ? <div className="metric-card"><div><span>Registros de sintomas</span><strong>{symptomsForDate.length}</strong><button className="mini-link" onClick={() => onAction({ kind: 'form', title: 'Registrar como estou', endpoint: '/symptoms' })}>Novo registro <Icon name="arrow" size={13}/></button></div></div> : overview ? <EmptyState icon="pulse" title="Sem registros nesta data" message="Escolha outro dia para ver os registros." action="Fazer primeiro registro" onAction={() => onAction({ kind: 'form', title: 'Registrar como estou', endpoint: '/symptoms' })}/> : null}</div></section>
  </>
}

function App() {
  const desktopShell = isDesktopShell()
  const [authView, setAuthView] = useState(() => localStorage.getItem('katui.auth-token') || localStorage.getItem('katui.guest-mode') === 'true' ? 'app' : 'landing')
  const [authLoading, setAuthLoading] = useState(false)
  const [authError, setAuthError] = useState('')
  const [onboardingError, setOnboardingError] = useState('')
  const [globalActionError, setGlobalActionError] = useState('')
  const [active, setActive] = useState('Visão geral')
  const [selectedDate, setSelectedDate] = useState(() => localStorage.getItem('katui.selected-date') || localDateKey(new Date()))
  const [dashboardCalendarExpanded, setDashboardCalendarExpanded] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationOpen, setNotificationOpen] = useState(false)
  const [notificationAlarms, setNotificationAlarms] = useState([])
  const [notificationLoading, setNotificationLoading] = useState(false)
  const [notificationError, setNotificationError] = useState('')
  const [notificationRefreshKey, setNotificationRefreshKey] = useState(0)
  const notificationRef = useRef(null)
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('katui.dark-mode') === 'true')
  const [onboardingComplete, setOnboardingComplete] = useState(() => localStorage.getItem('katui.guest-mode') === 'true' ? Boolean(readGuestProfile()?.name) : localStorage.getItem('katui.onboarding-complete') === 'true')
  const [onboardingStep, setOnboardingStep] = useState(() => Number(localStorage.getItem('katui.onboarding-step') || 0))
  const [onboardingForm, setOnboardingForm] = useState(() => {
    try {
      const savedGuest = localStorage.getItem('katui.guest-mode') === 'true' ? readGuestProfile() : null
      return JSON.parse(secureStorage.getItem('katui.onboarding-draft')) || (savedGuest ? { ...defaultOnboardingForm, name: savedGuest.name || '', age: savedGuest.age ? String(savedGuest.age) : '', sex: savedGuest.sex || 'nao-informado', weight: savedGuest.weight ? String(savedGuest.weight) : '', height: savedGuest.heightCm ? String(savedGuest.heightCm) : '', allergies: savedGuest.allergies || '', conditions: savedGuest.specialConditions || '', aiEnabled: false } : defaultOnboardingForm)
    } catch {
      return defaultOnboardingForm
    }
  })
  const [modal, setModal] = useState(null)
  const [connection, setConnection] = useState('checking')
  const [audioFeedbackEnabled, setAudioFeedbackEnabled] = useState(() => localStorage.getItem('katui.audio-feedback') === 'true')
  const [undoInfo, setUndoInfo] = useState(null)
  const [undoInProgress, setUndoInProgress] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState('')
  const undoStackRef = useRef([])
  const undoInProgressRef = useRef(false)
  const audioContextRef = useRef(null)
  const [pendingCount, setPendingCount] = useState(0)
  const [offlineStorageError, setOfflineStorageError] = useState('')
  const [profiles, setProfiles] = useState(() => {
    try { return JSON.parse(secureStorage.getItem('katui.profiles')) || [] } catch { return [] }
  })
  const [activeProfileId, setActiveProfileId] = useState(() => localStorage.getItem('katui.active-profile-id') || '')
  const [pushEnabled, setPushEnabled] = useState(() => !isDesktopShell() && localStorage.getItem(`katui.push-enabled.${localStorage.getItem('katui.active-profile-id') || ''}`) === 'true')
  const [notificationPermission, setNotificationPermission] = useState(() => typeof Notification === 'undefined' ? 'unsupported' : Notification.permission)
  const [accountLoaded, setAccountLoaded] = useState(false)
  const [calendarRecords, setCalendarRecords] = useState([])
  const [calendarLoadError, setCalendarLoadError] = useState('')
  const [guestMigrationPending, setGuestMigrationPending] = useState(() => localStorage.getItem('katui.guest-migration-consent') === 'true')
  const [notificationsEnabled, setNotificationsEnabled] = useState(() => localStorage.getItem('katui.notifications') !== 'false')
  const [weeklySummaryEnabled, setWeeklySummaryEnabled] = useState(() => localStorage.getItem('katui.weekly-summary') !== 'false')
  const [accessibility, setAccessibility] = useState(() => {
    try { return { ...defaultAccessibility, ...JSON.parse(localStorage.getItem('katui.accessibility') || '{}') } } catch { return defaultAccessibility }
  })

  useEffect(() => {
    const reportStorageError = (event) => {
      const message = event.detail?.message || 'Não foi possível salvar os dados locais protegidos.'
      setGlobalActionError(message)
      setOfflineStorageError(message)
    }
    window.addEventListener('katui:secure-storage-error', reportStorageError)
    return () => window.removeEventListener('katui:secure-storage-error', reportStorageError)
  }, [])

  useEffect(() => {
    if (authView !== 'app' || !activeProfileId) {
      setCalendarRecords([])
      setCalendarLoadError('')
      return undefined
    }
    let mounted = true
    const loadCalendarRecords = async () => {
      const results = await Promise.allSettled(calendarResources.map(async ({ resource, endpoint, label }) => {
        const records = await resourceApi.list(endpoint)
        if (!Array.isArray(records)) throw new Error(`${label}: a API retornou uma lista inválida.`)
        return records.map((record) => ({ ...record, __calendarResource: resource }))
      }))
      if (!mounted) return
      const loaded = results.flatMap((result) => result.status === 'fulfilled' ? result.value : [])
      const failedResources = results.flatMap((result, index) => result.status === 'rejected' ? [calendarResources[index].label] : [])
      setCalendarRecords(loaded)
      setCalendarLoadError(failedResources.length ? `Não foi possível carregar os indicadores de: ${failedResources.join(', ')}.` : '')
    }
    loadCalendarRecords()
    const refresh = (event) => {
      const changedEndpoint = event.detail?.endpoint || ''
      if (calendarResources.some(({ endpoint }) => changedEndpoint === endpoint || changedEndpoint.startsWith(`${endpoint}/`))) loadCalendarRecords()
    }
    window.addEventListener('katui:resource-change', refresh)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', refresh)
    }
  }, [activeProfileId, authView])

  useEffect(() => {
    const url = new URL(window.location.href)
    const requestedSection = url.searchParams.get('section')
    if (!requestedSection) return
    const availableSections = [...moduleNavigation, ...supportNavigation].map((item) => item.label)
    if (availableSections.includes(requestedSection)) setActive(requestedSection)
    url.searchParams.delete('section')
    window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`)
  }, [])

  useEffect(() => {
    if (authView !== 'app') return undefined
    if (isGuestMode()) {
      setConnection('guest')
      setPendingCount(0)
      setOfflineStorageError('')
      return undefined
    }
    if (isDemoMode()) {
      setConnection('demo')
      setPendingCount(0)
      setOfflineStorageError('')
      return undefined
    }
    let mounted = true
    const checkConnection = async () => {
      try {
        await apiRequest('/health')
        if (mounted) setConnection('online')
        setOfflineStorageError('')
        try {
          await flushOfflineQueue()
        } catch (error) {
          if (isNetworkError(error) && mounted) setConnection('offline')
          if (!isNetworkError(error) && mounted) setOfflineStorageError(error.message || 'A fila offline não pôde ser sincronizada.')
        }
      } catch (error) {
        if (mounted) setConnection(isNetworkError(error) || error.code === 'API_URL_NOT_CONFIGURED' ? 'offline' : 'online')
        if (mounted && error.status === 401) {
          localStorage.removeItem('katui.auth-token')
          setAuthView('landing')
        }
      }
      try {
        if (mounted) setPendingCount(await getOfflineQueueSize())
      } catch (error) {
        if (mounted) setOfflineStorageError(error.message || 'O armazenamento offline seguro não está disponível.')
      }
    }
    checkConnection()
    const interval = setInterval(checkConnection, 30000)
    const retryWhenOnline = () => checkConnection()
    const updateQueueCount = (event) => setPendingCount(event.detail?.count ?? 0)
    const updateQueueError = (event) => setOfflineStorageError(event.detail?.message || 'Uma ação offline precisa de atenção.')
    window.addEventListener('online', retryWhenOnline)
    window.addEventListener('katui:queue-change', updateQueueCount)
    window.addEventListener('katui:queue-error', updateQueueError)
    getOfflineQueueSize().then((count) => { if (mounted) setPendingCount(count) }).catch((error) => { if (mounted) setOfflineStorageError(error.message || 'O armazenamento offline seguro não está disponível.') })
    return () => { mounted = false; clearInterval(interval); window.removeEventListener('online', retryWhenOnline); window.removeEventListener('katui:queue-change', updateQueueCount); window.removeEventListener('katui:queue-error', updateQueueError) }
  }, [authView])

  useEffect(() => {
    if (authView !== 'app' || isGuestMode()) return undefined
    let mounted = true
    apiRequest('/me').then((account) => {
      if (!mounted) return
      setAccountLoaded(true)
      const accountProfiles = Array.isArray(account?.profiles) ? account.profiles : account?.profile ? [account.profile] : null
      if (!accountProfiles) return
      const validProfiles = accountProfiles.filter((profile) => profile?.id)
      const localPendingProfiles = profiles.filter((profile) => String(profile.id).startsWith('local-') && !validProfiles.some((item) => item.id === profile.id))
      const mergedProfiles = [...validProfiles, ...localPendingProfiles]
      setProfiles(mergedProfiles)
      secureStorage.setItem('katui.profiles', JSON.stringify(mergedProfiles))
      const preferredId = account.activeProfileId && mergedProfiles.some((profile) => profile.id === account.activeProfileId)
        ? account.activeProfileId
        : mergedProfiles.some((profile) => profile.id === activeProfileId) ? activeProfileId : mergedProfiles[0]?.id || ''
      setActiveProfileId(preferredId)
      localStorage.setItem('katui.active-profile-id', preferredId)
      setOnboardingComplete(mergedProfiles.length > 0)
      localStorage.setItem('katui.onboarding-complete', String(mergedProfiles.length > 0))
    }).catch((error) => {
      if (mounted && error.status === 401) {
        localStorage.removeItem('katui.auth-token')
        setAuthView('landing')
      } else if (mounted && !isNetworkError(error)) {
        setOfflineStorageError(error.message || 'Não foi possível carregar seus perfis do servidor.')
      }
    })
    const updateProfileId = (event) => {
      const { localId, profile } = event.detail || {}
      if (!profile?.id) return
      if (localStorage.getItem('katui.guest-migration-profile-id') === localId) localStorage.setItem('katui.guest-migration-profile-id', profile.id)
      setProfiles((currentProfiles) => {
        const updated = currentProfiles.map((item) => item.id === localId ? { ...item, ...profile } : item)
        secureStorage.setItem('katui.profiles', JSON.stringify(updated))
        return updated
      })
      setActiveProfileId((currentId) => {
        if (currentId !== localId) return currentId
        localStorage.setItem('katui.active-profile-id', profile.id)
        return profile.id
      })
    }
    window.addEventListener('katui:profile-id-change', updateProfileId)
    return () => {
      mounted = false
      window.removeEventListener('katui:profile-id-change', updateProfileId)
    }
  }, [authView])

  useEffect(() => {
    localStorage.setItem('katui.dark-mode', String(darkMode))
  }, [darkMode])

  useEffect(() => {
    localStorage.setItem('katui.notifications', String(notificationsEnabled))
    localStorage.setItem('katui.weekly-summary', String(weeklySummaryEnabled))
    localStorage.setItem('katui.accessibility', JSON.stringify(accessibility))
  }, [notificationsEnabled, weeklySummaryEnabled, accessibility])

  useEffect(() => {
    localStorage.setItem('katui.selected-date', selectedDate)
  }, [selectedDate])

  useEffect(() => {
    if (!onboardingComplete) {
      localStorage.setItem('katui.onboarding-step', String(onboardingStep))
      secureStorage.setItem('katui.onboarding-draft', JSON.stringify(onboardingForm))
    }
  }, [onboardingComplete, onboardingStep, onboardingForm])

  const openAction = (action) => { setGlobalActionError(''); setModal(action) }
  const startGuestMode = async () => {
    if (!secureStorage.getItem('katui.guest-data')) secureStorage.setItem('katui.guest-data', JSON.stringify(emptyGuestData()))
    const savedGuestProfile = readGuestProfile()
    const hasProfile = Boolean(savedGuestProfile?.name)
    secureStorage.setItem('katui.profiles', JSON.stringify(hasProfile ? [savedGuestProfile] : []))
    secureStorage.setItem('katui.profile-draft', JSON.stringify(savedGuestProfile || {}))
    try {
      await flushSecureStorage()
    } catch (error) {
      const message = error.message || 'Não foi possível preparar o armazenamento local protegido.'
      setGlobalActionError(message)
      setAuthError(message)
      return
    }
    localStorage.setItem('katui.guest-mode', 'true')
    localStorage.setItem('katui.onboarding-complete', String(hasProfile))
    localStorage.removeItem('katui.demo-mode')
    localStorage.removeItem('katui.auth-token')
    localStorage.removeItem('katui.auth-email')
    localStorage.setItem('katui.active-profile-id', 'guest-local-profile')
    setProfiles(hasProfile ? [savedGuestProfile] : [])
    setActiveProfileId('guest-local-profile')
    setOnboardingForm(savedGuestProfile ? { ...defaultOnboardingForm, name: savedGuestProfile.name || '', age: savedGuestProfile.age ? String(savedGuestProfile.age) : '', sex: savedGuestProfile.sex || 'nao-informado', weight: savedGuestProfile.weight ? String(savedGuestProfile.weight) : '', height: savedGuestProfile.heightCm ? String(savedGuestProfile.heightCm) : '', allergies: savedGuestProfile.allergies || '', conditions: savedGuestProfile.specialConditions || '', aiEnabled: false } : defaultOnboardingForm)
    setOnboardingStep(0)
    setOnboardingComplete(hasProfile)
    setActive('Visão geral')
    setModal(null)
    setAuthView('app')
    setConnection('guest')
  }
  const handleAuth = async (form, mode) => {
    if (mode === 'register' && form.password !== form.passwordConfirmation) throw new Error('As senhas precisam ser iguais.')
    const wasGuest = isGuestMode()
    if (wasGuest) localStorage.removeItem('katui.guest-mode')
    let result
    try {
      result = await apiRequest(mode === 'register' ? '/auth/register' : '/auth/login', { method: 'POST', body: JSON.stringify(mode === 'register' ? { name: form.name.trim(), email: form.email.trim(), password: form.password } : { email: form.email.trim(), password: form.password }) })
    } catch (error) {
      if (wasGuest) localStorage.setItem('katui.guest-mode', 'true')
      throw error
    }
    const token = result?.token || result?.accessToken
    if (!token) {
      if (wasGuest) localStorage.setItem('katui.guest-mode', 'true')
      throw new Error('A resposta de autenticação não trouxe um token válido.')
    }
    const accountEmail = form.email.trim().toLowerCase()
    const previousEmail = localStorage.getItem('katui.auth-email')
    if (previousEmail !== accountEmail) {
      ;['katui.profiles', 'katui.onboarding-draft', 'katui.profile-draft'].forEach((key) => secureStorage.removeItem(key))
      ;['katui.active-profile-id', 'katui.onboarding-complete', 'katui.onboarding-step'].forEach((key) => localStorage.removeItem(key))
      setProfiles([])
      setActiveProfileId('')
      setOnboardingComplete(false)
      setOnboardingStep(0)
      setOnboardingForm(defaultOnboardingForm)
    }
    localStorage.setItem('katui.auth-token', token)
    localStorage.setItem('katui.auth-email', accountEmail)
    setAccountLoaded(false)
    if (wasGuest) {
      setGuestMigrationPending(Boolean(form.migrateGuestData))
      if (form.migrateGuestData) localStorage.setItem('katui.guest-migration-consent', 'true')
      else localStorage.removeItem('katui.guest-migration-consent')
      localStorage.removeItem('katui.guest-migration-completed')
      localStorage.removeItem('katui.guest-migration-profile-id')
    }
    setAuthView('app')
    if (wasGuest && form.migrateGuestData) setGuestMigrationPending(true)
    if (mode === 'register') {
      setOnboardingComplete(false)
      setOnboardingStep(0)
      setOnboardingForm({ ...defaultOnboardingForm, name: form.name.trim() })
      localStorage.removeItem('katui.onboarding-complete')
      secureStorage.setItem('katui.onboarding-draft', JSON.stringify({ ...defaultOnboardingForm, name: form.name.trim() }))
    }
  }
  const logout = () => {
    localStorage.removeItem('katui.auth-token')
    localStorage.removeItem('katui.demo-mode')
    localStorage.removeItem('katui.guest-mode')
    setAuthView('landing')
    undoStackRef.current = []
    setUndoInfo(null)
    setFeedbackMessage('')
    setProfileOpen(false)
    setModal(null)
  }
  const activeProfile = profiles.find((profile) => profile.id === activeProfileId) || profiles[0] || null
  const guestMode = isGuestMode()
  const femaleProfile = ['feminino', 'female', 'f'].includes(String(activeProfile?.sex || '').toLowerCase())
  const visibleSupportNavigation = supportNavigation.filter((item) => item.label !== 'Saúde feminina' || femaleProfile)
  useEffect(() => {
    setPushEnabled(!desktopShell && localStorage.getItem(`katui.push-enabled.${activeProfileId}`) === 'true')
  }, [activeProfileId])
  useEffect(() => {
    if (authView !== 'app' || !activeProfileId) {
      setNotificationAlarms([])
      setNotificationError('')
      return undefined
    }
    let mounted = true
    const loadNotifications = async () => {
      setNotificationLoading(true)
      try {
        const localMode = isGuestMode() || isDemoMode()
        const alarmsRequest = !localMode && pushEnabled && isPushConfigured()
          ? listBackgroundAlarms({ token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId })
          : resourceApi.list('/alarms')
        const [alarms, medicationRecords] = await Promise.all([
          alarmsRequest,
          localMode ? resourceApi.list('/medications') : Promise.resolve([]),
        ])
        if (!Array.isArray(alarms) || !Array.isArray(medicationRecords)) throw new Error('A lista de alarmes retornada não é válida.')
        const now = Date.now()
        const candidates = [
          ...alarms.filter((alarm) => !['concluído', 'concluido', 'cancelado', 'cancelada', 'desativado'].includes(String(alarm.status || '').toLowerCase())),
          ...medicationRecords.map((medication) => getLocalMedicationAlarm(medication, now)).filter(Boolean),
        ]
        const upcoming = candidates
          .map((alarm) => ({ ...alarm, __notificationAt: getAlarmTimestamp(alarm) }))
          .filter((alarm) => alarm.__notificationAt !== null && alarm.__notificationAt >= now)
          .sort((first, second) => first.__notificationAt - second.__notificationAt)
        if (mounted) {
          setNotificationAlarms(upcoming)
          setNotificationError('')
        }
      } catch (error) {
        if (mounted) setNotificationError(error.message || 'Não foi possível carregar os alarmes.')
      } finally {
        if (mounted) setNotificationLoading(false)
      }
    }
    loadNotifications()
    const refresh = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (['/alarms', '/medications'].some((path) => endpoint === path || endpoint.startsWith(`${path}/`))) loadNotifications()
    }
    const interval = window.setInterval(loadNotifications, 60_000)
    window.addEventListener('katui:resource-change', refresh)
    return () => {
      mounted = false
      window.clearInterval(interval)
      window.removeEventListener('katui:resource-change', refresh)
    }
  }, [authView, activeProfileId, guestMode, pushEnabled, notificationRefreshKey])
  useEffect(() => {
    if (!notificationOpen) return undefined
    const closeOnOutsideClick = (event) => {
      if (!notificationRef.current?.contains(event.target)) setNotificationOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setNotificationOpen(false)
    }
    window.addEventListener('pointerdown', closeOnOutsideClick)
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      window.removeEventListener('pointerdown', closeOnOutsideClick)
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [notificationOpen])
  useEffect(() => {
    if (authView !== 'app' || (!guestMode && !desktopShell) || !notificationsEnabled || typeof Notification === 'undefined' || Notification.permission !== 'granted') return undefined
    let checking = false
    const checkLocalReminders = async () => {
      if (checking) return
      checking = true
      let data
      let notified
      try {
        data = guestMode
          ? JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
          : { alarms: await resourceApi.list('/alarms'), medications: await resourceApi.list('/medications') }
        if (!Array.isArray(data.alarms) || !Array.isArray(data.medications)) throw new Error('A API retornou uma lista inválida de lembretes.')
        notified = JSON.parse(localStorage.getItem('katui.local-alarm-notifications') || '[]')
        if (!Array.isArray(notified)) throw new Error('O histórico local de notificações está inválido.')
      } catch (error) {
        setGlobalActionError(error.message || 'Não foi possível verificar os alarmes locais.')
        checking = false
        return
      }
      const keys = new Set(notified)
      const dueReminders = getDueLocalReminders(data)
      dueReminders.forEach((reminder) => {
        if (keys.has(reminder.key)) return
        try {
          new Notification(reminder.title, { body: reminder.body, icon: `${import.meta.env.BASE_URL}katui-logo.png` })
          keys.add(reminder.key)
        } catch (error) {
          setGlobalActionError(error.message || 'O navegador não conseguiu exibir o lembrete local.')
        }
      })
      localStorage.setItem('katui.local-alarm-notifications', JSON.stringify([...keys].slice(-200)))
      checking = false
    }
    checkLocalReminders()
    const timer = window.setInterval(checkLocalReminders, 15_000)
    return () => window.clearInterval(timer)
  }, [authView, guestMode, notificationsEnabled, desktopShell, activeProfileId])
  useEffect(() => {
    const profile = profiles.find((item) => item.id === activeProfileId)
    if (!profile?.preferences) return
    const preferences = { ...defaultProfilePreferences, ...profile.preferences, accessibility: { ...defaultAccessibility, ...(profile.preferences.accessibility || {}) } }
    setDarkMode(Boolean(preferences.darkMode))
    setNotificationsEnabled(preferences.notificationsEnabled !== false)
    setWeeklySummaryEnabled(preferences.weeklySummaryEnabled !== false)
    setPushEnabled(localStorage.getItem(`katui.push-enabled.${profile.id}`) === 'true')
    setAccessibility(preferences.accessibility)
  }, [activeProfileId])
  useEffect(() => {
    if (active === 'Saúde feminina' && !femaleProfile) setActive('Visão geral')
  }, [active, femaleProfile])
  const updateProfilePreferences = (changes) => {
    if (!activeProfileId) return
    const currentPreferences = { ...defaultProfilePreferences, ...(activeProfile?.preferences || {}), accessibility: { ...defaultAccessibility, ...(activeProfile?.preferences?.accessibility || {}) } }
    const nextPreferences = { ...currentPreferences, ...changes }
    setProfiles((currentProfiles) => {
      const nextProfiles = currentProfiles.map((profile) => profile.id === activeProfileId ? { ...profile, preferences: nextPreferences } : profile)
      secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
      return nextProfiles
    })
    if (changes.darkMode !== undefined) setDarkMode(changes.darkMode)
    if (changes.notificationsEnabled !== undefined) setNotificationsEnabled(changes.notificationsEnabled)
    if (changes.weeklySummaryEnabled !== undefined) setWeeklySummaryEnabled(changes.weeklySummaryEnabled)
    if (changes.accessibility) setAccessibility(changes.accessibility)
  }
  const toggleProfilePreference = (key) => updateProfilePreferences({ [key]: !({ darkMode, notificationsEnabled, weeklySummaryEnabled }[key]) })
  const toggleBackgroundPush = async () => {
    const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
    setGlobalActionError('')
    try {
      if (pushEnabled) {
        await disableBackgroundPush(auth)
        localStorage.removeItem(`katui.push-enabled.${activeProfileId}`)
        setPushEnabled(false)
        setFeedbackMessage('Alarmes em segundo plano desativados neste dispositivo.')
      } else {
        await enableBackgroundPush(auth)
        localStorage.setItem(`katui.push-enabled.${activeProfileId}`, 'true')
        setPushEnabled(true)
        setFeedbackMessage('Notificações autorizadas. Os novos alarmes poderão tocar com o site fechado.')
        try {
          const medications = await resourceApi.list('/medications')
          if (!Array.isArray(medications)) throw new Error('A API retornou uma lista inválida de medicamentos.')
          const results = await Promise.allSettled(medications.filter((medication) => medication.generateAlarms).map((medication) => scheduleMedicationReminders(medication, auth)))
          const failed = results.filter((result) => result.status === 'rejected')
          if (failed.length) setGlobalActionError(`Notificações ativadas, mas ${failed.length} medicamento(s) ainda não têm alarmes. Confira data, frequência e duração e salve novamente.`)
        } catch (error) {
          setGlobalActionError(`Notificações ativadas, mas não foi possível sincronizar os medicamentos existentes: ${error.message}`)
        }
      }
      setNotificationPermission(typeof Notification === 'undefined' ? 'unsupported' : Notification.permission)
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível configurar as notificações.')
    }
  }
  const enableLocalNotifications = async () => {
    if (typeof Notification === 'undefined') {
      setGlobalActionError('Este navegador não oferece notificações.')
      return
    }
    try {
      const permission = Notification.permission === 'default' ? await Notification.requestPermission() : Notification.permission
      setNotificationPermission(permission)
      if (permission !== 'granted') throw new Error('Permita notificações nas configurações do navegador para receber lembretes locais.')
      localStorage.setItem('katui.notifications', 'true')
      setNotificationsEnabled(true)
      updateProfilePreferences({ notificationsEnabled: true })
      setFeedbackMessage('Notificações locais ativadas. Elas exigem que o site permaneça aberto.')
      setGlobalActionError('')
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível ativar as notificações locais.')
    }
  }
  const updateProfileAccessibility = (key, value) => updateProfilePreferences({ accessibility: { ...accessibility, [key]: value === undefined ? !accessibility[key] : value, ...(key === 'textScale' ? { largeText: Number(value) > 100 } : {}) } })
  const selectProfile = (profile) => {
    const preferences = { ...defaultProfilePreferences, ...(profile.preferences || {}), accessibility: { ...defaultAccessibility, ...(profile.preferences?.accessibility || {}) } }
    setDarkMode(Boolean(preferences.darkMode))
    setNotificationsEnabled(preferences.notificationsEnabled !== false)
    setWeeklySummaryEnabled(preferences.weeklySummaryEnabled !== false)
    setPushEnabled(localStorage.getItem(`katui.push-enabled.${profile.id}`) === 'true')
    setAccessibility(preferences.accessibility)
    setActiveProfileId(profile.id)
    localStorage.setItem('katui.active-profile-id', profile.id)
    setProfileOpen(false)
  }
  const registerLocalDependent = (data, id = `local-${Date.now()}`) => {
    if (!data?.name?.trim()) return
    const profile = { id, name: data.name.trim(), relation: data.relation?.trim() || data.detail?.trim() || 'Dependente', type: 'dependente', preferences: { ...defaultProfilePreferences }, ...data }
    const nextProfiles = [...profiles, profile]
    setProfiles(nextProfiles)
    setActiveProfileId(profile.id)
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
    localStorage.setItem('katui.active-profile-id', profile.id)
  }
  const playFeedbackTone = (kind, enabled = audioFeedbackEnabled) => {
    if (!enabled) return
    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext
    if (!AudioContextConstructor) {
      setFeedbackMessage('O navegador não oferece áudio; o feedback visual continua ativo.')
      return
    }
    try {
      const context = audioContextRef.current || new AudioContextConstructor()
      audioContextRef.current = context
      const oscillator = context.createOscillator()
      const gain = context.createGain()
      const now = context.currentTime
      oscillator.type = 'sine'
      oscillator.frequency.value = kind === 'error' ? 220 : kind === 'undo' ? 520 : 660
      gain.gain.setValueAtTime(0.0001, now)
      gain.gain.exponentialRampToValueAtTime(0.07, now + 0.015)
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.16)
      oscillator.connect(gain)
      gain.connect(context.destination)
      context.resume().then(() => {
        oscillator.start()
        oscillator.stop(context.currentTime + 0.17)
      }).catch(() => setFeedbackMessage('O som não pôde ser reproduzido. O feedback visual continua ativo.'))
    } catch {
      setFeedbackMessage('O som não pôde ser reproduzido. O feedback visual continua ativo.')
    }
  }
  const toggleAudioFeedback = () => {
    const enabled = !audioFeedbackEnabled
    setAudioFeedbackEnabled(enabled)
    localStorage.setItem('katui.audio-feedback', String(enabled))
    if (enabled) {
      setFeedbackMessage('Feedback sonoro ativado.')
      playFeedbackTone('success', true)
    } else {
      setFeedbackMessage('Feedback sonoro desativado.')
    }
  }
  const getUndoDescriptor = (action, response) => {
    const recordCollectionPattern = /^\/(medications|alarms|prescriptions|symptoms|vital-signs|appointments|exams|feminine-health|pregnancy|vaccines|documents|reports)$/
    const deleteMatch = action.method === 'DELETE' && action.endpoint.match(/^(\/(?:medications|alarms|prescriptions|symptoms|vital-signs|appointments|exams|feminine-health|pregnancy|vaccines|documents|reports))\/([^/]+)$/)
    if (deleteMatch && action.data && typeof action.data === 'object') {
      const record = { ...action.data }
      delete record.id
      delete record._id
      return {
        label: 'Registro removido.',
        originalEndpoint: `${deleteMatch[1]}/${deleteMatch[2]}`,
        action: { endpoint: deleteMatch[1], method: 'POST', data: record, profileId: action.data.profileId || activeProfileId, ...(action.data.__pushAlarm ? { pushAlarm: true } : {}), ...(deleteMatch[1] === '/medications' && action.data.generateAlarms && isPushConfigured() ? { pushMedication: true } : {}) },
      }
    }
    const method = action.method || 'POST'
    const createdRecordId = response?.id || response?.record?.id || response?.data?.id
    if (method === 'POST' && recordCollectionPattern.test(action.endpoint) && createdRecordId) {
      return {
        label: 'Registro adicionado.',
        action: { endpoint: `${action.endpoint}/${encodeURIComponent(createdRecordId)}`, method: 'DELETE', profileId: activeProfileId, ...(action.endpoint === '/alarms' && pushEnabled ? { pushAlarm: true, data: { ...action.data, ...response, id: createdRecordId, __pushAlarm: true } } : {}), ...(action.endpoint === '/medications' && action.data?.generateAlarms && pushEnabled ? { pushMedication: true, data: { ...action.data, ...response, id: createdRecordId } } : {}) },
      }
    }
    return null
  }
  const undoLastAction = async () => {
    const last = undoStackRef.current[undoStackRef.current.length - 1]
    if (!last || undoInProgressRef.current) return
    undoInProgressRef.current = true
    setUndoInProgress(true)
    try {
      const auth = { token: localStorage.getItem('katui.auth-token'), profileId: last.action.profileId }
      const resourceId = decodeURIComponent(last.action.endpoint.split('/').pop())
      if (last.action.pushAlarm && last.action.method === 'DELETE') await removeSingleReminder(resourceId, auth)
      if (last.action.pushMedication && last.action.method === 'DELETE') await removeMedicationReminders(resourceId, auth)
      let response
      try {
        response = await apiRequest(last.action.endpoint, {
          method: last.action.method,
          profileId: last.action.profileId,
          ...(last.action.method === 'DELETE' ? {} : { body: JSON.stringify(last.action.data) }),
          idempotencyKey: `undo-${globalThis.crypto?.randomUUID?.() || Date.now()}`,
        })
      } catch (error) {
        if (last.action.pushMedication && last.action.method === 'DELETE') {
          try {
            await scheduleMedicationReminders(last.action.data, auth)
          } catch (restoreError) {
            throw new Error(`O registro não foi removido e a restauração do alarme falhou: ${error.message} ${restoreError.message}`)
          }
        }
        if (last.action.pushAlarm && last.action.method === 'DELETE') {
          try {
            await scheduleRestoredAlarm(last.action.data, auth)
          } catch (restoreError) {
            throw new Error(`O registro não foi removido e a restauração do alarme falhou: ${error.message} ${restoreError.message}`)
          }
        }
        throw error
      }
      if (last.action.pushAlarm && last.action.method === 'POST') {
        await scheduleRestoredAlarm({ ...last.action.data, id: response?.id || last.action.data.sourceId }, auth)
      }
      if (last.action.pushMedication && last.action.method === 'POST') {
        const restoredId = response?.id || response?.medication?.id || response?.data?.id
        if (!restoredId) throw new Error('Medicamento restaurado, mas o servidor não retornou um ID para reativar o alarme.')
        await scheduleMedicationReminders({ ...last.action.data, id: restoredId }, auth)
      }
      let remainingStack = undoStackRef.current.slice(0, -1)
      if (last.action.method === 'POST' && last.originalEndpoint) {
        const restoredId = response?.id || response?.record?.id || response?.data?.id
        remainingStack = remainingStack.flatMap((entry) => entry.action.method === 'DELETE' && entry.action.endpoint === last.originalEndpoint
          ? restoredId
            ? [{ ...entry, action: { ...entry.action, endpoint: `${last.action.endpoint}/${encodeURIComponent(restoredId)}` } }]
            : []
          : [entry])
      }
      undoStackRef.current = remainingStack
      window.dispatchEvent(new CustomEvent('katui:resource-change', { detail: { endpoint: last.action.endpoint.split('/').slice(0, 2).join('/') } }))
      const next = undoStackRef.current[undoStackRef.current.length - 1] || null
      setUndoInfo(next)
      setFeedbackMessage(last.label === 'Registro removido.' ? 'Registro restaurado.' : 'Inclusão desfeita.')
      playFeedbackTone('undo')
      setGlobalActionError('')
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível desfazer a ação. Tente novamente.')
      playFeedbackTone('error')
    } finally {
      undoInProgressRef.current = false
      setUndoInProgress(false)
    }
  }
  const performAction = async (action) => {
    const guestMode = isGuestMode()
    if (guestMode && (action.kind === 'ai' || action.sensitive)) throw new Error('Este recurso exige uma conta e não está disponível no modo Guest.')
    if (!isApiConfigured() && !guestMode && !isDemoMode()) throw new Error('O servidor não está configurado neste build. Defina VITE_API_URL e gere o build novamente.')
    const requestId = action.requestId || globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
    const { file: selectedFile, ...jsonData } = action.data || {}
    const payload = { ...jsonData, profileId: activeProfileId, date: jsonData.date || selectedDate }
    const localProfileId = action.endpoint === '/profiles' ? `local-${Date.now()}-${Math.random().toString(16).slice(2)}` : null
    const demoMode = isDemoMode()
    const localMode = demoMode || guestMode
    const method = action.method || 'POST'
    const backgroundScheduleRequired = !desktopShell && !localMode && (
      (action.endpoint === '/medications' && method === 'POST' && payload.generateAlarms) ||
      (action.endpoint === '/alarms' && method === 'POST') ||
      (action.kind === 'prescription-confirm' && payload.medications?.some((medication) => medication.generateAlarms)) ||
      (action.data?.__pushAlarm && method === 'DELETE')
    )
    const medicationScheduleCleanupRequired = !desktopShell && !localMode && isPushConfigured() && method === 'DELETE' && /^\/medications\/[^/]+$/.test(action.endpoint)
    if (backgroundScheduleRequired && !isPushConfigured()) {
      throw new Error('Ative primeiro o serviço de alarmes em segundo plano. O site não consegue disparar alarmes fechados sem Web Push configurado.')
    }
    if (backgroundScheduleRequired && !pushEnabled) {
      throw new Error('Ative Alarmes em segundo plano em Configurações antes de salvar este lembrete.')
    }
    if (backgroundScheduleRequired && connection !== 'online') {
      throw new Error('Conecte-se à internet para registrar ou remover um alarme em segundo plano.')
    }
    if (medicationScheduleCleanupRequired && connection !== 'online') {
      throw new Error('Conecte-se à internet para excluir o medicamento e cancelar seus alarmes em segundo plano.')
    }
    if ((action.sensitive || action.kind === 'ai') && connection !== 'online' && !localMode) {
      throw new Error(action.sensitive
        ? 'Esta configuração exige conexão com o servidor.'
        : 'A leitura de receita com IA exige conexão. O arquivo não será armazenado na fila offline.')
    }
    let response
    let savedOffline = connection !== 'online' && !localMode
    if (!savedOffline) {
      try {
        if (action.kind === 'ai' && selectedFile) {
          const body = new FormData()
          body.append('file', selectedFile)
          body.append('profileId', activeProfileId)
          body.append('date', jsonData.date || selectedDate)
          if (jsonData.notes) body.append('notes', jsonData.notes)
          response = await apiRequest(action.endpoint, { method: action.method || 'POST', idempotencyKey: requestId, body })
        } else {
          if (action.data?.__pushAlarm && method === 'DELETE' && !localMode && !desktopShell) {
            const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
            await removeSingleReminder(decodeURIComponent(action.endpoint.split('/').pop()), auth)
            try {
              response = await apiRequest(action.endpoint, { method, idempotencyKey: requestId })
            } catch (error) {
              try {
                await scheduleRestoredAlarm(action.data, auth)
              } catch (restoreError) {
                throw new Error(`O alarme foi desativado, mas o registro não foi removido nem reativado: ${error.message} Restauração falhou: ${restoreError.message}`)
              }
              throw error
            }
          } else if (medicationScheduleCleanupRequired) {
            const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
            const medicationId = decodeURIComponent(action.endpoint.split('/').pop())
            await removeMedicationReminders(medicationId, auth)
            try {
              response = await apiRequest(action.endpoint, { method, idempotencyKey: requestId })
            } catch (error) {
              if (action.data?.generateAlarms) {
                try {
                  await scheduleMedicationReminders(action.data, auth)
                } catch (restoreError) {
                  throw new Error(`O medicamento não foi removido e seu alarme não pôde ser restaurado: ${error.message} ${restoreError.message}`)
                }
              }
              if (isNetworkError(error)) {
                throw new Error('Não foi possível confirmar a exclusão do medicamento. O alarme foi restaurado; tente novamente quando a conexão estiver estável.')
              }
              throw error
            }
          } else {
            response = await apiRequest(action.endpoint, {
              method,
              idempotencyKey: requestId,
              ...(method === 'DELETE' ? {} : { body: JSON.stringify(payload) }),
            })
          }
        }
      } catch (error) {
        if (!isNetworkError(error) || action.sensitive || action.kind === 'ai') throw error
        savedOffline = true
        setConnection('offline')
      }
    }
    if (savedOffline) {
      await queueOfflineAction({
        endpoint: action.endpoint,
        method: action.method || 'POST',
        kind: action.kind,
        id: requestId,
        localProfileId,
        profileId: activeProfileId,
        data: payload,
      })
      if (localProfileId) registerLocalDependent(action.data, localProfileId)
      undoStackRef.current = []
      setUndoInfo(null)
      setOfflineStorageError('')
      setFeedbackMessage('Registro salvo localmente. Ações na fila offline ainda não podem ser desfeitas.')
      playFeedbackTone('success')
      setModal({ kind: 'success', title: 'Salvo localmente', message: 'O registro foi guardado com criptografia neste dispositivo e será sincronizado quando a conexão voltar.' })
      return
    }
    if (action.kind === 'ai') {
      if (!response?.id || !Array.isArray(response.medications)) {
        throw new Error('A resposta da análise não contém uma sugestão válida para revisão.')
      }
      setModal({ kind: 'ai-review', title: 'Revise a sugestão', result: response })
      return
    }
    if (action.endpoint === '/profiles' || action.endpoint === '/profiles/me') {
      const serverProfile = response?.profile || response
      if (!serverProfile?.id) throw new Error('O servidor não retornou o ID do perfil criado.')
      if (action.endpoint === '/profiles') registerLocalDependent(action.data, serverProfile.id)
      if (action.endpoint === '/profiles/me') {
        const profile = { ...serverProfile, type: 'principal', relation: 'Perfil principal' }
        const nextProfiles = [profile, ...profiles.filter((item) => item.type !== 'principal')]
        setProfiles(nextProfiles)
        setActiveProfileId(profile.id)
        secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
        localStorage.setItem('katui.active-profile-id', profile.id)
      }
    }
    let alarmSetupWarning = ''
    const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
    if (!desktopShell && !localMode && method === 'POST' && action.endpoint === '/medications' && payload.generateAlarms) {
      try {
        await scheduleMedicationReminders({ ...response, ...payload, id: response?.id || response?.medication?.id || response?.data?.id }, auth)
      } catch (error) {
        alarmSetupWarning = `Medicamento salvo, mas os alarmes em segundo plano não foram configurados: ${error.message}`
      }
    } else if (!desktopShell && !localMode && action.kind === 'prescription-confirm' && payload.medications?.some((medication) => medication.generateAlarms)) {
      const requestedMedications = payload.medications.filter((medication) => medication.generateAlarms)
      const savedMedications = Array.isArray(response?.medications) ? response.medications : Array.isArray(response?.createdMedications) ? response.createdMedications : []
      const results = await Promise.allSettled(requestedMedications.map(async (requested) => {
        const saved = savedMedications.find((medication) => String(medication.clientId) === requested.clientId)
        if (!saved) throw new Error(`A API não retornou o registro criado para ${requested.name || 'um medicamento'}.`)
        await scheduleMedicationReminders({ ...requested, ...saved, id: saved?.id || saved?._id }, auth)
      }))
      const failed = results.filter((result) => result.status === 'rejected')
      if (failed.length) {
        alarmSetupWarning = `Receita confirmada, mas ${failed.length} alarme(s) não foram configurados. Verifique se a API retornou os IDs dos medicamentos e confira os dados da posologia.`
      }
    } else if (!desktopShell && !localMode && method === 'POST' && action.endpoint === '/alarms') {
      try {
        const alarmId = response?.id || response?.alarm?.id || response?.data?.id
        await scheduleSingleReminder(payload, alarmId, auth)
      } catch (error) {
        alarmSetupWarning = `Alarme salvo, mas não foi ativado para funcionar com o site fechado: ${error.message}`
      }
    }
    const undoDescriptor = getUndoDescriptor(action, response)
    if (undoDescriptor) {
      const entry = { ...undoDescriptor, action: { ...undoDescriptor.action, profileId: undoDescriptor.action.profileId || activeProfileId } }
      undoStackRef.current = [...undoStackRef.current, entry]
      setUndoInfo(entry)
    }
    setFeedbackMessage(undoDescriptor?.label || 'Alteração salva.')
    playFeedbackTone('success')
    window.dispatchEvent(new CustomEvent('katui:resource-change', { detail: { endpoint: action.endpoint } }))
    const successMessage = alarmSetupWarning || (method === 'DELETE'
      ? guestMode ? 'Registro removido somente deste dispositivo.' : demoMode ? 'Registro fictício removido deste navegador.' : 'O registro foi removido do servidor.'
      : guestMode ? 'Registro salvo somente neste dispositivo. Crie uma conta para ter backup e sincronização.' : demoMode ? 'Dados fictícios atualizados somente neste navegador. Nenhum servidor real foi acessado.' : 'O servidor recebeu sua solicitação e os dados já estão sincronizados.')
    if (alarmSetupWarning) setGlobalActionError(alarmSetupWarning)
    setModal({ kind: 'success', title: action.title === 'Receita confirmada' ? 'Receita confirmada' : 'Tudo certo!', message: successMessage })
  }

  const submitAction = async (action) => {
    setGlobalActionError('')
    try {
      await performAction(action)
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível concluir esta ação.')
      setFeedbackMessage('A ação não foi concluída.')
      playFeedbackTone('error')
      throw error
    }
  }

  const exportBackup = () => {
    const backup = {
      format: 'katui-personal-backup',
      version: 1,
      exportedAt: new Date().toISOString(),
      profiles,
      activeProfileId,
      preferences: { darkMode, notificationsEnabled, weeklySummaryEnabled, accessibility },
      profileDraft: secureStorage.getItem('katui.profile-draft'),
    }
    const blob = new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `katui-backup-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
  }

  const importBackup = (file) => {
    const reader = new FileReader()
    reader.onload = async () => {
      try {
        const backup = JSON.parse(String(reader.result || ''))
        if (backup.format !== 'katui-personal-backup' || backup.version !== 1 || !Array.isArray(backup.profiles)) throw new Error('INVALID_BACKUP')
        const nextPreferences = { ...defaultAccessibility, ...(backup.preferences?.accessibility || {}) }
        const nextActiveProfileId = backup.activeProfileId || backup.profiles[0]?.id || ''
        secureStorage.setItem('katui.profiles', JSON.stringify(backup.profiles))
        if (backup.profileDraft) secureStorage.setItem('katui.profile-draft', backup.profileDraft)
        try {
          await flushSecureStorage()
        } catch (error) {
          setGlobalActionError(error.message || 'Não foi possível salvar o backup neste dispositivo.')
          return
        }
        setProfiles(backup.profiles)
        setActiveProfileId(nextActiveProfileId)
        localStorage.setItem('katui.active-profile-id', nextActiveProfileId)
        setDarkMode(Boolean(backup.preferences?.darkMode))
        setNotificationsEnabled(backup.preferences?.notificationsEnabled !== false)
        setWeeklySummaryEnabled(backup.preferences?.weeklySummaryEnabled !== false)
        setAccessibility(nextPreferences)
        setModal({ kind: 'success', title: 'Backup importado', message: 'Suas preferências e perfis foram restaurados neste dispositivo.' })
      } catch {
        setModal({ kind: 'success', title: 'Backup inválido', message: 'Não foi possível importar este arquivo. Escolha um backup do Katu’I.' })
      }
    }
    reader.readAsText(file)
  }

  const clearLocalData = async () => {
    if (!window.confirm('Apagar os dados locais do Katu’I neste dispositivo? Essa ação não pode ser desfeita.')) return
    try {
      await clearSecureStorage()
      localStorage.clear()
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível apagar os dados locais protegidos.')
      return
    }
    clearOfflineQueue().catch((error) => setOfflineStorageError(error.message || 'Não foi possível limpar a fila offline.'))
    setProfiles([])
    setActiveProfileId('')
    setDarkMode(false)
    setNotificationsEnabled(true)
    setWeeklySummaryEnabled(true)
    setAccessibility(defaultAccessibility)
    setOnboardingForm(defaultOnboardingForm)
    setOnboardingStep(0)
    setOnboardingComplete(false)
    setAuthView('landing')
  }

  const syncBackup = () => submitAction({ kind: 'form', title: 'Backup pessoal', endpoint: '/backup', data: { name: 'Backup pessoal', detail: 'Sincronização manual' } })
  const savePregnancy = (data) => submitAction({ kind: 'form', title: data.enabled ? 'Gestação ativada' : 'Gestação desativada', endpoint: '/pregnancy', data: { enabled: data.enabled, startDate: data.startDate, profileId: activeProfileId } })
  const saveProfileTheme = async (themeId) => {
    const theme = profileThemeOptions.find((option) => option.id === themeId) || profileThemeOptions[0]
    await submitAction({ kind: 'form', title: 'Cor do perfil', endpoint: `/profiles/${activeProfileId}`, method: 'PATCH', data: { themeColor: theme.id, corHex: theme.color } })
    const nextProfiles = profiles.map((profile) => profile.id === activeProfileId ? { ...profile, themeColor: theme.id, corHex: theme.color } : profile)
    setProfiles(nextProfiles)
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
  }
  const saveAiSettings = async ({ provider, model, apiKey }) => {
    const ai = { provider: provider || 'system', model: model || '', configured: provider !== 'system' && Boolean(apiKey || activeProfile?.preferences?.ai?.configured), maskedKey: provider !== 'system' ? (activeProfile?.preferences?.ai?.maskedKey || '') : '' }
    await submitAction({ kind: 'form', title: 'Configuração de IA', endpoint: `/profiles/${activeProfileId}/ai-settings`, method: 'PATCH', sensitive: Boolean(apiKey), data: { provider: ai.provider, model: ai.model, ...(apiKey ? { apiKey } : {}) } })
    updateProfilePreferences({ ai })
  }

  const migrateGuestRecords = async (profileId) => {
    if (localStorage.getItem('katui.guest-migration-consent') !== 'true' || !profileId || String(profileId).startsWith('local-')) {
      setGuestMigrationPending(true)
      return
    }
    const guestData = JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
    const completed = new Set(JSON.parse(localStorage.getItem('katui.guest-migration-completed') || '[]'))
    const failures = []
    for (const collection of guestCollections) {
      const records = Array.isArray(guestData[collection]) ? guestData[collection] : []
      for (let index = 0; index < records.length; index += 1) {
        const record = records[index]
        const migrationId = `${collection}:${record.id || index}`
        if (completed.has(migrationId)) continue
        const { id, _id, ...recordData } = record
        try {
          await apiRequest(`/${collection}`, {
            method: 'POST',
            idempotencyKey: `guest-migration-${migrationId}`,
            body: JSON.stringify({ ...recordData, profileId }),
          })
          completed.add(migrationId)
          localStorage.setItem('katui.guest-migration-completed', JSON.stringify([...completed]))
        } catch (error) {
          failures.push(`${collection}: ${error.message || 'falha ao importar'}`)
        }
      }
    }
    if (failures.length) {
      setGuestMigrationPending(true)
      setGlobalActionError(`Alguns registros Guest não foram importados (${failures.join('; ')}). Os originais continuam salvos neste dispositivo; tente novamente quando a conexão estiver disponível.`)
      return
    }
    localStorage.removeItem('katui.guest-migration-consent')
    localStorage.removeItem('katui.guest-migration-completed')
    localStorage.removeItem('katui.guest-migration-profile-id')
    setGuestMigrationPending(false)
    setGlobalActionError('')
  }

  const retryGuestMigration = () => {
    if (connection !== 'online') {
      setGlobalActionError('Conecte-se à internet para continuar a importação. Os registros Guest continuam neste dispositivo.')
      return
    }
    migrateGuestRecords(localStorage.getItem('katui.guest-migration-profile-id') || activeProfileId).catch((error) => setGlobalActionError(error.message || 'Não foi possível importar os registros Guest.'))
  }

  useEffect(() => {
    if (authView !== 'app' || guestMode || !guestMigrationPending || !accountLoaded || connection !== 'online' || !activeProfileId || activeProfileId === 'guest-local-profile' || String(activeProfileId).startsWith('local-')) return
    migrateGuestRecords(localStorage.getItem('katui.guest-migration-profile-id') || activeProfileId).catch((error) => {
      setGuestMigrationPending(true)
      setGlobalActionError(error.message || 'Não foi possível importar os registros Guest. Os originais continuam neste dispositivo.')
    })
  }, [authView, guestMode, guestMigrationPending, accountLoaded, connection, activeProfileId])

  useEffect(() => {
    const handleUndoShortcut = (event) => {
      if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 'z' || event.shiftKey || !undoInfo || undoInProgress) return
      const target = event.target
      if (target instanceof HTMLElement && (target.isContentEditable || target.closest('input, textarea, select, [contenteditable="true"]'))) return
      event.preventDefault()
      undoLastAction()
    }
    window.addEventListener('keydown', handleUndoShortcut)
    return () => window.removeEventListener('keydown', handleUndoShortcut)
  }, [undoInfo, undoInProgress])

  const finishOnboarding = async () => {
    setOnboardingError('')
    const profile = { name: onboardingForm.name.trim(), age: onboardingForm.age ? Number(onboardingForm.age) : null, sex: onboardingForm.sex, weight: onboardingForm.weight ? Number(onboardingForm.weight) : null, heightCm: onboardingForm.height ? Number(onboardingForm.height) : null, allergies: onboardingForm.allergies.trim() || null, specialConditions: onboardingForm.conditions.trim() || null, aiEnabled: onboardingForm.aiEnabled, themeColor: 'turquoise', corHex: profileThemeOptions[0].color, preferences: { ...defaultProfilePreferences } }
    if (isGuestMode()) {
      const profileRecord = { id: 'guest-local-profile', relation: 'Perfil neste dispositivo', type: 'principal', ...profile }
      secureStorage.setItem('katui.guest-profile', JSON.stringify(profileRecord))
      secureStorage.setItem('katui.profile-draft', JSON.stringify(profile))
      secureStorage.setItem('katui.profiles', JSON.stringify([profileRecord]))
      secureStorage.removeItem('katui.onboarding-draft')
      try {
        await flushSecureStorage()
      } catch (error) {
        setOnboardingError(error.message || 'Não foi possível salvar o perfil neste dispositivo.')
        return
      }
      localStorage.setItem('katui.onboarding-complete', 'true')
      localStorage.setItem('katui.active-profile-id', profileRecord.id)
      localStorage.removeItem('katui.onboarding-step')
      setProfiles([profileRecord])
      setActiveProfileId(profileRecord.id)
      setOnboardingComplete(true)
      return
    }
    const localProfileId = `local-${Date.now()}-${Math.random().toString(16).slice(2)}`
    let profileId = localProfileId
    try {
      if (!isApiConfigured()) throw new Error('O servidor não está configurado neste build. Defina VITE_API_URL e gere o build novamente.')
      if (connection === 'online') {
        try {
          const result = await apiRequest('/profiles/me', { method: 'POST', idempotencyKey: localProfileId, body: JSON.stringify(profile) })
          const serverProfile = result?.profile || result
          if (!serverProfile?.id) throw new Error('O servidor não retornou o ID do perfil criado.')
          profileId = serverProfile.id
        } catch (error) {
          if (!isNetworkError(error)) throw error
          setConnection('offline')
          await queueOfflineAction({ endpoint: '/profiles/me', method: 'POST', id: localProfileId, localProfileId, profileId: activeProfileId, data: profile })
        }
      } else {
        await queueOfflineAction({ endpoint: '/profiles/me', method: 'POST', id: localProfileId, localProfileId, profileId: activeProfileId, data: profile })
      }
    } catch (error) {
      setOnboardingError(error.message || 'Não foi possível criar o perfil. Tente novamente.')
      return
    }
    secureStorage.setItem('katui.profile-draft', JSON.stringify(profile))
    const profileRecord = { id: profileId, name: profile.name || 'Meu perfil', relation: 'Perfil principal', type: 'principal', themeColor: 'turquoise', corHex: profileThemeOptions[0].color, preferences: { ...defaultProfilePreferences }, ...profile }
    const nextProfiles = [profileRecord, ...profiles.filter((item) => item.type !== 'principal')]
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
    secureStorage.removeItem('katui.onboarding-draft')
    try {
      await flushSecureStorage()
    } catch (error) {
      setOnboardingError(error.message || 'Não foi possível salvar o perfil neste dispositivo.')
      return
    }
    setProfiles(nextProfiles)
    setActiveProfileId(profileRecord.id)
    localStorage.setItem('katui.onboarding-complete', 'true')
    localStorage.setItem('katui.active-profile-id', profileRecord.id)
    localStorage.removeItem('katui.onboarding-step')
    setOnboardingComplete(true)
    if (localStorage.getItem('katui.guest-migration-consent') === 'true') {
      localStorage.setItem('katui.guest-migration-profile-id', profileId)
      setAccountLoaded(true)
    }
  }

  const skipOnboarding = () => {
    localStorage.setItem('katui.onboarding-complete', 'true')
    localStorage.removeItem('katui.onboarding-step')
    secureStorage.removeItem('katui.onboarding-draft')
    setOnboardingComplete(true)
  }

  if (authView !== 'app') return <AuthPage view={authView} onViewChange={(view) => { setAuthError(''); setAuthView(view) }} onSubmit={(form, mode) => { setAuthError(''); setAuthLoading(true); handleAuth(form, mode).catch((error) => setAuthError(error.message || 'Não foi possível concluir o acesso.')).finally(() => setAuthLoading(false)) }} onGuestLogin={startGuestMode} loading={authLoading} error={authError} />
  if (!onboardingComplete) return <Onboarding step={onboardingStep} setStep={setOnboardingStep} form={onboardingForm} setForm={setOnboardingForm} onFinish={finishOnboarding} onSkip={skipOnboarding} darkMode={darkMode} error={onboardingError} isGuest={guestMode} />

  const textScale = Number(accessibility.textScale || (accessibility.largeText ? 125 : 100))
  const accessibilityClasses = [darkMode ? 'dark-mode' : '', textScale > 100 ? `text-scale-${textScale}` : '', accessibility.dyslexiaFont ? 'dyslexia-font' : '', accessibility.highContrast ? 'high-contrast' : '', accessibility.reduceMotion ? 'reduce-motion' : ''].filter(Boolean).join(' ')
  const activeTheme = getProfileTheme(activeProfile)
  const themeStyle = { '--profile-accent': activeTheme.color, '--profile-accent-soft': activeTheme.soft, '--profile-accent-dark': activeTheme.dark, '--profile-accent-ink': activeTheme.ink, '--accessibility-zoom': textScale / 100, '--accessibility-scale': textScale / 100 }
  return <div className={`app-shell ${accessibilityClasses}`} style={themeStyle}>
    <aside className="sidebar">
      <Brand />
      <div className="sidebar-label">Menu principal</div>
      <nav>{moduleNavigation.map(item => <button key={item.label} className={`nav-item nav-${item.resource}${active === item.label ? ' active' : ''}`} onClick={() => setActive(item.label)}><Icon name={item.icon} size={19}/><span>{item.label}</span></button>)}</nav>
      <div className="sidebar-label support-label">Sua saúde</div>
      <nav>{visibleSupportNavigation.filter((item) => !guestMode || !['Perfis e dependentes', 'Backup pessoal'].includes(item.label)).map(item => <button key={item.label} className={`nav-item nav-${item.resource}${active === item.label ? ' active' : ''}`} onClick={() => setActive(item.label)}><Icon name={item.icon} size={19}/><span>{item.label}</span></button>)}</nav>
      <div className="sidebar-bottom"><button className={active === 'Ajuda' ? 'nav-item active' : 'nav-item'} onClick={() => setActive('Ajuda')}><Icon name="help" size={19}/><span>Ajuda</span></button><button className={active === 'Configurações' ? 'nav-item active' : 'nav-item'} onClick={() => setActive('Configurações')}><Icon name="settings" size={19}/><span>Configurações</span></button><button className="nav-item" onClick={logout}><Icon name="arrow" size={19}/><span>{guestMode ? 'Sair do modo Guest' : 'Sair da conta'}</span></button></div>
    </aside>
    <main className="main-content">
      <header className="topbar">
        <button className="mobile-menu"><Icon name="grid" size={20}/></button>
        <div className="breadcrumb">Meu espaço <span>/</span> <strong>{active}</strong></div>
        <div className="top-actions">
          <ConnectionBadge status={connection}/>
          <SyncBadge connection={connection} pendingCount={pendingCount} error={offlineStorageError}/>
          <div className="notification-wrap" ref={notificationRef}>
            <button
              type="button"
              className="icon-button notification"
              aria-label={`Alarmes${notificationAlarms.length ? `, ${notificationAlarms.length} próximos` : ''}`}
              aria-haspopup="dialog"
              aria-expanded={notificationOpen}
              onClick={() => {
                setNotificationOpen((open) => !open)
                setNotificationRefreshKey((current) => current + 1)
              }}
            >
              <Icon name="bell" size={20}/>
              {notificationAlarms.length > 0 && <span className="notification-count">{notificationAlarms.length > 99 ? '99+' : notificationAlarms.length}</span>}
            </button>
            {notificationOpen && <section className="notification-panel" role="dialog" aria-label="Próximos alarmes">
              <div className="notification-panel-header">
                <div><strong>Próximos alarmes</strong><small>{notificationAlarms.length ? `${notificationAlarms.length} lembrete(s) futuro(s)` : 'Lembretes deste perfil'}</small></div>
                <button type="button" aria-label="Fechar notificações" onClick={() => setNotificationOpen(false)}>×</button>
              </div>
              {!guestMode && !isDemoMode() && !pushEnabled && <button type="button" className="notification-setup-note" onClick={() => { setNotificationOpen(false); setActive('Configurações') }}>Ative os alarmes em segundo plano nas Configurações para receber avisos com o site fechado.</button>}
              {guestMode && <p className="notification-local-note">No modo Guest, os avisos só funcionam enquanto o site estiver aberto.</p>}
              {notificationLoading && <p className="notification-state">Atualizando alarmes…</p>}
              {notificationError && <p className="notification-state notification-error" role="alert">{notificationError}</p>}
              {!notificationLoading && notificationAlarms.length === 0 && !notificationError && <p className="notification-state">Nenhum alarme futuro cadastrado.</p>}
              {notificationAlarms.length > 0 && <div className="notification-list">{notificationAlarms.slice(0, 5).map((alarm) => {
                const when = new Date(alarm.__notificationAt)
                const title = alarm.name || alarm.title || 'Lembrete de saúde'
                return <button type="button" className="notification-item" key={alarm.id || `${title}-${alarm.__notificationAt}`} onClick={() => {
                  setSelectedDate(localDateKey(when))
                  setActive('Alarmes')
                  setNotificationOpen(false)
                }}>
                  <span className="notification-item-icon"><Icon name={alarm.sourceKind === 'medication' ? 'pill' : 'bell'} size={16}/></span>
                  <span className="notification-item-copy"><strong>{title}</strong><small>{when.toLocaleDateString('pt-BR', { weekday: 'short', day: 'numeric', month: 'short' })} · {when.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</small></span>
                </button>
              })}</div>}
              <button type="button" className="notification-all" onClick={() => { setActive('Alarmes'); setNotificationOpen(false) }}>Ver todos os alarmes <Icon name="arrow" size={14}/></button>
            </section>}
          </div>
          {guestMode ? <div className="guest-account-actions"><button className="guest-login-cta" onClick={() => { setAuthError(''); setAuthView('login') }}>Entrar</button><button className="guest-register-cta" onClick={() => { setAuthError(''); setAuthView('register') }}>Registre-se para fazer backup</button></div> : <div className="profile-wrap"><button className="profile-button" aria-label="Trocar perfil de cuidado" aria-haspopup="menu" aria-expanded={profileOpen} onClick={() => setProfileOpen(!profileOpen)}><div className="avatar">{activeProfile?.name?.slice(0, 2).toUpperCase() || '?'}</div><div className="profile-name"><small className="profile-switch-label">Trocar perfil</small><strong>{activeProfile?.name || 'Meu perfil'}</strong><small>{activeProfile?.relation || 'Perfil principal'}</small></div><span className="profile-chevron">⌄</span></button>{profileOpen && <div className="profile-menu" role="menu"><div className="profile-menu-header"><strong>Perfil de cuidado</strong><small>Escolha quem você está acompanhando</small></div>{profiles.length ? profiles.map((profile) => <button key={profile.id} role="menuitem" className={profile.id === activeProfile?.id ? 'profile-option selected' : 'profile-option'} onClick={() => selectProfile(profile)}><span className="profile-option-avatar" style={{ background: getProfileTheme(profile).soft, color: getProfileTheme(profile).color }}>{profile.name.slice(0, 2).toUpperCase()}</span><span><b>{profile.name}</b><small>{profile.relation}</small></span>{profile.id === activeProfile?.id && <Icon name="check" size={14}/>}</button>) : <p className="profile-empty">Conclua o tutorial para criar o perfil principal.</p>}<button className="profile-add" onClick={() => { setProfileOpen(false); openAction({ kind: 'form', title: 'Adicionar perfil dependente', endpoint: '/profiles' }) }}><Icon name="plus" size={14}/> Adicionar dependente</button><button className="profile-logout" role="menuitem" onClick={() => { setProfileOpen(false); logout() }}><Icon name="arrow" size={14}/> Sair da conta</button></div>}</div>}
        </div>
      </header>
      <div className="content-wrap">{guestMigrationPending && !guestMode && <div className="guest-migration-banner" role="status"><span>Há registros Guest aguardando importação. Os originais continuam salvos neste dispositivo.</span><button className="soft-btn" disabled={connection !== 'online'} onClick={retryGuestMigration}>{connection === 'online' ? 'Tentar importar novamente' : 'Aguardando conexão'}</button></div>}{active === 'Ajuda' ? <HelpPage /> : active === 'Visão geral' ? <>
        <section className="welcome-row"><div><p className="eyebrow">{guestMode ? 'MODO GUEST · DADOS NESTE DISPOSITIVO' : 'SEU ESPAÇO DE SAÚDE'}</p><h1>Olá{activeProfile?.name ? ` ${activeProfile.name}` : ''} <span>✦</span></h1><p className="welcome-copy">{guestMode ? 'Comece registrando suas informações. Elas ficam salvas neste navegador.' : 'Seu espaço está pronto. Comece adicionando sua primeira informação.'}</p></div><div className="quick-add-wrap"><button className="outline-btn" aria-expanded={quickAddOpen} aria-haspopup="menu" onClick={() => setQuickAddOpen(!quickAddOpen)}><Icon name="plus" size={17}/> Adicionar registro</button>{quickAddOpen && <QuickAddMenu femaleProfile={femaleProfile} isGuest={guestMode} onAction={(action) => { setQuickAddOpen(false); openAction(action) }} />}</div></section>
        <section className="hero-card"><div className="hero-copy"><span className="hero-kicker">SEU RESUMO DE HOJE</span><h2>Como você está se sentindo?</h2><p>Registrar como você está ajuda a perceber padrões e acompanhar sua saúde ao longo do tempo.</p><button className="primary-btn" onClick={() => openAction({ kind: 'form', title: 'Registrar como estou', endpoint: '/symptoms' })}>Registrar como estou <Icon name="arrow" size={16}/></button></div></section>
        <section className="section-heading"><div><h2>Seu dia em foco</h2><p>Um panorama simples para você se cuidar melhor.</p></div><button className="text-btn" onClick={() => { setDashboardCalendarExpanded(true); window.setTimeout(() => document.querySelector('.calendar-card')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 0) }}>Ver histórico <Icon name="arrow" size={15}/></button></section>
        <DashboardSummary profile={activeProfile} onAction={openAction} onNavigate={setActive} selectedDate={selectedDate} onDateSelect={setSelectedDate} calendarExpanded={dashboardCalendarExpanded} onCalendarExpandedChange={setDashboardCalendarExpanded} calendarRecords={calendarRecords} calendarLoadError={calendarLoadError}/>
      </> : active === 'Configurações' ? <SettingsPage profile={activeProfile} darkMode={darkMode} onToggleDarkMode={() => toggleProfilePreference('darkMode')} notificationsEnabled={notificationsEnabled} onToggleNotifications={() => toggleProfilePreference('notificationsEnabled')} weeklySummaryEnabled={weeklySummaryEnabled} onToggleWeeklySummary={() => toggleProfilePreference('weeklySummaryEnabled')} audioFeedbackEnabled={audioFeedbackEnabled} onToggleAudioFeedback={toggleAudioFeedback} pushEnabled={pushEnabled} pushConfigured={isPushConfigured() && !isDemoMode()} notificationPermission={notificationPermission} onTogglePush={toggleBackgroundPush} onEnableLocalNotifications={enableLocalNotifications} accessibility={accessibility} onToggleAccessibility={updateProfileAccessibility} onSaveProfileTheme={saveProfileTheme} aiSettings={activeProfile?.preferences?.ai} onSaveAiSettings={saveAiSettings} onExportBackup={exportBackup} onImportBackup={importBackup} onClearLocalData={clearLocalData} onSyncBackup={syncBackup} isGuest={guestMode} onRegister={() => setAuthView('register')} /> : <ModulePage active={active} onAction={openAction} darkMode={darkMode} onToggleDarkMode={() => toggleProfilePreference('darkMode')} profile={activeProfile} onPregnancyChange={savePregnancy} selectedDate={selectedDate} onDateSelect={setSelectedDate} calendarRecords={calendarRecords} calendarLoadError={calendarLoadError} isGuest={guestMode} usePushAlarms={!guestMode && !isDemoMode() && isPushConfigured()}/>}</div>
    </main>
    {modal && <ActionModal action={modal} connection={connection} selectedDate={selectedDate} pushEnabled={pushEnabled} onClose={() => setModal(null)} onSubmit={submitAction} />}
    {feedbackMessage && <div className={`action-feedback${globalActionError ? ' raised' : ''}`} role="status" aria-live="polite"><span>{feedbackMessage}</span>{undoInfo && <button type="button" className="undo-action" disabled={undoInProgress} onClick={undoLastAction}>{undoInProgress ? 'Desfazendo…' : 'Desfazer registro (Ctrl+Z)'}</button>}<button type="button" className="dismiss-feedback" aria-label="Fechar aviso" onClick={() => setFeedbackMessage('')}>×</button></div>}
    {globalActionError && <div className="submission-error-toast" role="alert">{globalActionError}</div>}
  </div>
}

export default App
