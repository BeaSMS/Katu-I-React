import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, iconAssets } from './dadosAplicacao'

export function Icon({ name, size = 20 }) {
  if (iconAssets[name])
    return (
      <span
        className="asset-icon"
        aria-hidden="true"
        style={{
          width: size,
          height: size,
          WebkitMaskImage: `url("${iconAssets[name]}")`,
          maskImage: `url("${iconAssets[name]}")`,
        }}
      />
    )
  const common = {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.8,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
  }
  const paths = {
    grid: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </>
    ),
    pill: (
      <>
        <path d="M9 3h6v3h3a2 2 0 0 1 2 2v13H4V8a2 2 0 0 1 2-2h3V3Z" />
        <path d="M9 3h6M8 12h8M12 9v6" />
      </>
    ),
    spark: (
      <>
        <path d="m12 3-1.4 5.6L5 10l5.6 1.4L12 17l1.4-5.6L19 10l-5.6-1.4Z" />
        <path d="m19 16-.7 2.3L16 19l2.3.7L19 22l.7-2.3L22 19l-2.3-.7Z" />
      </>
    ),
    heart: (
      <>
        <path d="M20.8 8.8c0 5.5-8.8 10.4-8.8 10.4S3.2 14.3 3.2 8.8A4.8 4.8 0 0 1 12 6a4.8 4.8 0 0 1 8.8 2.8Z" />
      </>
    ),
    profile: (
      <>
        <circle cx="12" cy="8" r="3.5" />
        <path d="M4.5 21a7.5 7.5 0 0 1 15 0" />
      </>
    ),
    pulse: (
      <>
        <path d="M3 12h4l2-7 4 14 2-7h6" />
        <circle cx="12" cy="12" r="9" opacity=".18" />
      </>
    ),
    calendar: (
      <>
        <rect x="3" y="4" width="18" height="17" rx="2" />
        <path d="M16 2v4M8 2v4M3 9h18M8 13h2M14 13h2M8 17h2" />
      </>
    ),
    file: (
      <>
        <path d="M6 2h8l4 4v16H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z" />
        <path d="M14 2v5h5M8 12h8M8 16h6" />
      </>
    ),
    bell: (
      <>
        <path d="M6 9a6 6 0 0 1 12 0c0 7 3 7 3 9H3c0-2 3-2 3-9Z" />
        <path d="M10 21h4" />
      </>
    ),
    search: (
      <>
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4-4" />
      </>
    ),
    plus: (
      <>
        <path d="M12 5v14M5 12h14" />
      </>
    ),
    arrow: (
      <>
        <path d="M5 12h14M13 6l6 6-6 6" />
      </>
    ),
    chevron: <path d="m9 18 6-6-6-6" />,
    check: <path d="m5 12 4 4L19 6" />,
    location: (
      <>
        <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
        <circle cx="12" cy="10" r="2.5" />
      </>
    ),
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </>
    ),
    trend: (
      <>
        <path d="M4 17 10 11l4 4 6-7" />
        <path d="M15 8h5v5" />
      </>
    ),
    sun: (
      <>
        <circle cx="12" cy="12" r="3.5" />
        <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4" />
      </>
    ),
    cloud: (
      <>
        <path d="M7 18h10a4 4 0 0 0 .5-8A5.5 5.5 0 0 0 7 8.5 4.5 4.5 0 0 0 7 18Z" />
      </>
    ),
    brain: (
      <>
        <path d="M9 4a3 3 0 0 0-3 3v1a3 3 0 0 0-1 5 3 3 0 0 0 3 5h1V4Z" />
        <path d="M15 4a3 3 0 0 1 3 3v1a3 3 0 0 1 1 5 3 3 0 0 1-3 5h-1V4ZM9 8h6M9 12h6M9 16h6" />
      </>
    ),
    sad: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M8 9h.01M16 9h.01M8.5 16a5 5 0 0 1 7 0" />
      </>
    ),
    battery: (
      <>
        <rect x="3" y="7" width="17" height="10" rx="2" />
        <path d="M21 10v4M6 10v4M10 10v4" />
      </>
    ),
    bolt: <path d="m13 2-8 12h6l-1 8 8-12h-6l1-8Z" />,
    help: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M9.7 9a2.4 2.4 0 1 1 4.1 1.7c-.9.8-1.8 1.2-1.8 2.5M12 17h.01" />
      </>
    ),
    sliders: (
      <>
        <path d="M4 7h9M17 7h3M4 17h3M11 17h9" />
        <circle cx="15" cy="7" r="2" />
        <circle cx="9" cy="17" r="2" />
      </>
    ),
    settings: (
      <>
        <circle cx="12" cy="12" r="3" />
        <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.8 1.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-2.5V20a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1-1.8-1.8.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.6-1H6v-2.5h.2a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 1.8-1.8.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.6V5h2.5v.2a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1 1.8 1.8-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v2.5h-.2a1.7 1.7 0 0 0-1.6 1Z" />
      </>
    ),
    droplet: (
      <>
        <path d="M12 3c-2.6 4-6.5 7.4-6.5 11.2a6.5 6.5 0 0 0 13 0C18.5 10.4 14.6 7 12 3Z" />
        <path d="M9 15a3 3 0 0 0 3 3" />
      </>
    ),
    thermometer: (
      <>
        <path d="M14 14.8V5a3 3 0 0 0-6 0v9.8a5 5 0 1 0 6 0Z" />
        <path d="M11 11v7" />
      </>
    ),
    scale: (
      <>
        <path d="M5 4h14l2 16H3L5 4Z" />
        <path d="m12 8 2.5 3M8 15h8" />
      </>
    ),
    ruler: (
      <>
        <path d="M4 7h16v10H4z" />
        <path d="M8 7v4M12 7v3M16 7v4" />
      </>
    ),
  }
  return <svg {...common}>{paths[name]}</svg>
}

export function Brand() {
  return (
    <div className="brand">
      <img className="brand-logo" src={`${import.meta.env.BASE_URL}katui-logo.png`} alt="Katu'I" />
      <small>cuidado que acompanha</small>
    </div>
  )
}

export function EmptyState({
  icon = 'file',
  title = 'Ainda não há registros',
  message = 'Quando você adicionar informações, elas aparecerão aqui.',
  action,
  onAction,
}) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">
        <Icon name={icon} size={23} />
      </div>
      <strong>{title}</strong>
      <p>{message}</p>
      {action && (
        <button className="soft-btn" onClick={onAction}>
          <Icon name="plus" size={14} /> {action}
        </button>
      )}
    </div>
  )
}

export function vitalNumericValue(record, key) {
  const rawValue = String(record?.[key] ?? '').trim()
  if (!rawValue) return null
  const value = Number(rawValue.replace(',', '.'))
  return Number.isFinite(value) ? value : null
}

export function vitalValueLabel(record) {
  const units = {
    Glicose: 'mg/dL',
    Batimentos: 'BPM',
    Saturação: '%',
    Temperatura: '°C',
    Peso: 'kg',
    Altura: 'cm',
  }
  if (record.type === 'Pressão arterial') {
    const systolic = record.systolic ?? record.sistolica ?? ''
    const diastolic = record.diastolic ?? record.diastolica ?? ''
    return systolic !== '' && diastolic !== '' ? `${systolic}/${diastolic} mmHg` : 'Valores não informados'
  }
  const unit = units[record.type] || record.unit || ''
  return `${record.value ?? 'Valor não informado'}${unit ? ` ${unit}` : ''}`
}

export function recordDetailEntries(record) {
  const excluded = new Set([
    'id',
    '_id',
    '__calendarResource',
    'profileId',
    'createdAt',
    'updatedAt',
    'attachment',
    'file',
  ])
  return Object.entries(record || {}).filter(
    ([key, value]) => !excluded.has(key) && value !== '' && value !== null && value !== undefined,
  )
}

export function getAttachmentHref(record) {
  const attachment = record?.attachment
  const href =
    attachment?.dataUrl || attachment?.downloadUrl || attachment?.url || record?.attachmentUrl || record?.fileUrl
  if (typeof href !== 'string') return ''
  return /^(https?:\/\/|\/|blob:|data:application\/pdf;|data:image\/(?:jpeg|png);)/i.test(href) ? href : ''
}

export function localDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function readGuestProfile() {
  try {
    const profile = JSON.parse(secureStorage.getItem('katui.guest-profile') || 'null')
    return profile && typeof profile === 'object' ? profile : null
  } catch {
    return null
  }
}

export function parseDateKey(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : null
}

export function recordDate(record) {
  const value =
    record?.date ||
    record?.dateTime ||
    record?.datetime ||
    record?.recordedAt ||
    record?.recorded_at ||
    record?.createdAt ||
    record?.created_at ||
    record?.dataHoraRegistro ||
    record?.dataHora ||
    record?.dataRegistro ||
    record?.horario ||
    record?.startDate ||
    record?.dataInicio
  return String(value || '').slice(0, 10)
}

export function recordOccursOnDate(record, date, resource) {
  if (!record || !date) return false
  if (resource === 'profiles') return true
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

export function hasCalendarRecordOnDate(records, date, resource) {
  return records.some((record) => {
    const recordResource = record.__calendarResource || resource
    if (recordResource === 'medications') {
      const start = String(record.startDate || record.dataInicio || record.date || recordDate(record) || '').slice(
        0,
        10,
      )
      const end = String(record.endDate || record.dataFim || '').slice(0, 10)
      return start || end ? (!start || date >= start) && (!end || date <= end) : date === localDateKey(new Date())
    }
    return recordDate(record) === date
  })
}

export const calendarWeekdays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

export const calendarMonths = [
  'janeiro',
  'fevereiro',
  'março',
  'abril',
  'maio',
  'junho',
  'julho',
  'agosto',
  'setembro',
  'outubro',
  'novembro',
  'dezembro',
]
