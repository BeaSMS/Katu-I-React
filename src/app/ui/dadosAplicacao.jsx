import addIcon from '../../../Icons/frontend/svg/add.svg'
import arrowIcon from '../../../Icons/frontend/svg/arrow-right.svg'
import brainIcon from '../../../Icons/frontend/svg/brain.svg'
import calendarIcon from '../../../Icons/frontend/svg/calendar.svg'
import checkmarkIcon from '../../../Icons/frontend/svg/checkmark.svg'
import chevronIcon from '../../../Icons/frontend/svg/chevron-right.svg'
import cloudyWeatherIcon from '../../../Icons/frontend/svg/cloudy-weather.svg'
import dashboardIcon from '../../../Icons/frontend/svg/dashboard.svg'
import documentIcon from '../../../Icons/frontend/svg/document.svg'
import bloodIcon from '../../../Icons/frontend/svg/blood.svg'
import bloodPressureIcon from '../../../Icons/frontend/svg/blood-pressure.svg'
import folderIcon from '../../../Icons/frontend/svg/folder.svg'
import healthJournalIcon from '../../../Icons/frontend/svg/health-journal.svg'
import heartRateIcon from '../../../Icons/frontend/svg/heart-rate.svg'
import heightIcon from '../../../Icons/frontend/svg/height.svg'
import helpIcon from '../../../Icons/frontend/svg/help.svg'
import insightsIcon from '../../../Icons/frontend/svg/insights.svg'
import lightningIcon from '../../../Icons/frontend/svg/lightning.svg'
import locationPinIcon from '../../../Icons/frontend/svg/location-pin.svg'
import medicationIcon from '../../../Icons/frontend/svg/medication.svg'
import notificationsIcon from '../../../Icons/frontend/svg/notifications.svg'
import oxygenSaturationIcon from '../../../Icons/frontend/svg/oxygen-saturation.svg'
import preferencesIcon from '../../../Icons/frontend/svg/preferences.svg'
import receiptIcon from '../../../Icons/frontend/svg/receipt.svg'
import sadMoodIcon from '../../../Icons/frontend/svg/sad-mood.svg'
import searchIcon from '../../../Icons/frontend/svg/search.svg'
import settingsIcon from '../../../Icons/frontend/svg/settings.svg'
import sunnyWeatherIcon from '../../../Icons/frontend/svg/sunny-weather.svg'
import temperatureIcon from '../../../Icons/frontend/svg/temperature.svg'
import timeIcon from '../../../Icons/frontend/svg/time.svg'
import upwardTrendIcon from '../../../Icons/frontend/svg/upward-trend.svg'
import userProfileIcon from '../../../Icons/frontend/svg/user-profile.svg'
import vaccinationIcon from '../../../Icons/frontend/svg/vaccination.svg'
import weightIcon from '../../../Icons/frontend/svg/weight.svg'

export const medications = []

export const defaultOnboardingForm = {
  name: '',
  age: '',
  sex: 'nao-informado',
  weight: '',
  height: '',
  allergies: '',
  conditions: '',
  aiEnabled: false,
}

export const defaultAccessibility = {
  textScale: 100,
  largeText: false,
  dyslexiaFont: false,
  highContrast: false,
  reduceMotion: false,
}

export const defaultProfilePreferences = {
  darkMode: false,
  notificationsEnabled: true,
  weeklySummaryEnabled: true,
  accessibility: defaultAccessibility,
  ai: { provider: 'system', model: '', configured: false, maskedKey: '' },
}

export const guestCollections = [
  'medications',
  'alarms',
  'prescriptions',
  'symptoms',
  'vital-signs',
  'appointments',
  'exams',
  'feminine-health',
  'pregnancy',
  'vaccines',
  'documents',
  'reports',
]

export const emptyGuestData = () => Object.fromEntries(guestCollections.map((collection) => [collection, []]))

export const profileThemeOptions = [
  { id: 'turquoise', label: 'Turquesa', color: '#009C9C', soft: '#E2F5F3', dark: '#66C8C5', ink: '#FFFFFF' },
  { id: 'gold', label: 'Dourado', color: '#D89A27', soft: '#FFF1CD', dark: '#F0B94F', ink: '#4A3510' },
  { id: 'sky', label: 'Azul', color: '#2F8FB0', soft: '#E5F3F8', dark: '#80C5D9', ink: '#FFFFFF' },
  { id: 'purple', label: 'Violeta', color: '#7054A5', soft: '#EEE9F7', dark: '#B8A5D7', ink: '#FFFFFF' },
  { id: 'rose', label: 'Rosa', color: '#C66B88', soft: '#F9E8EE', dark: '#E4A6B9', ink: '#FFFFFF' },
  { id: 'green', label: 'Verde', color: '#4A9A73', soft: '#E6F4ED', dark: '#91D1B2', ink: '#FFFFFF' },
]

export const getProfileTheme = (profile) =>
  profileThemeOptions.find(
    (option) =>
      option.id === profile?.themeColor || option.color.toLowerCase() === String(profile?.corHex || '').toLowerCase(),
  ) || profileThemeOptions[0]

export const iconAssets = {
  grid: dashboardIcon,
  pill: medicationIcon,
  heart: bloodPressureIcon,
  profile: userProfileIcon,
  pulse: heartRateIcon,
  calendar: calendarIcon,
  file: documentIcon,
  folder: folderIcon,
  journal: healthJournalIcon,
  receipt: receiptIcon,
  insights: insightsIcon,
  bell: notificationsIcon,
  search: searchIcon,
  plus: addIcon,
  arrow: arrowIcon,
  chevron: chevronIcon,
  check: checkmarkIcon,
  location: locationPinIcon,
  clock: timeIcon,
  trend: upwardTrendIcon,
  sun: sunnyWeatherIcon,
  cloud: cloudyWeatherIcon,
  brain: brainIcon,
  sad: sadMoodIcon,
  bolt: lightningIcon,
  help: helpIcon,
  sliders: preferencesIcon,
  settings: settingsIcon,
  spark: insightsIcon,
  blood: bloodIcon,
  droplet: bloodIcon,
  vaccination: vaccinationIcon,
  thermometer: temperatureIcon,
  scale: weightIcon,
  ruler: heightIcon,
  battery: oxygenSaturationIcon,
}

export function getDueLocalReminders(data, now = Date.now()) {
  const reminders = []
  const alarms = Array.isArray(data.alarms) ? data.alarms : []
  alarms.forEach((alarm) => {
    if (!alarm.date || !alarm.time) return
    const dueAt = new Date(`${alarm.date}T${alarm.time}`).getTime()
    if (dueAt <= now && now - dueAt < 30_000)
      reminders.push({
        key: `alarm:${alarm.id || dueAt}`,
        title: 'Lembrete de saúde',
        body: 'Você tem um cuidado agendado no Katu’I.',
      })
  })
  const medications = Array.isArray(data.medications) ? data.medications : []
  medications.forEach((medication) => {
    if (!medication.generateAlarms || !medication.date || !medication.time) return
    const frequency = Number(medication.frequencyValue)
    if (!Number.isInteger(frequency) || frequency < 1 || frequency > 24) return
    const intervalMinutes = String(medication.frequencyType || '')
      .toLowerCase()
      .includes('vezes')
      ? Math.round(1440 / frequency)
      : frequency * 60
    const startAt = new Date(`${medication.date}T${medication.time}`).getTime()
    const interval = intervalMinutes * 60_000
    if (!Number.isFinite(startAt) || startAt > now) return
    const elapsed = Math.floor((now - startAt) / interval)
    const dueAt = startAt + elapsed * interval
    const endAt = medication.continuous ? Infinity : startAt + Number(medication.durationDays || 0) * 86_400_000
    if (dueAt <= now && now - dueAt < 30_000 && dueAt < endAt) {
      reminders.push({
        key: `medication:${medication.id || medication.name}:${dueAt}`,
        title: 'Hora do seu medicamento',
        body: 'Abra o Katu’I para conferir seu lembrete.',
      })
    }
  })
  return reminders
}

export function getAlarmTimestamp(alarm) {
  const directTimestamp = alarm.nextAt || alarm.startAt
  if (directTimestamp) {
    const timestamp = new Date(directTimestamp).getTime()
    if (Number.isFinite(timestamp)) return timestamp
  }
  if (!alarm.date || !alarm.time) return null
  const timestamp = new Date(`${alarm.date}T${alarm.time}`).getTime()
  return Number.isFinite(timestamp) ? timestamp : null
}

export function getLocalMedicationAlarm(medication, now = Date.now()) {
  if (!medication.generateAlarms || !medication.date || !medication.time) return null
  const frequency = Number(medication.frequencyValue)
  if (!Number.isInteger(frequency) || frequency < 1 || frequency > 24) return null
  const intervalMinutes = String(medication.frequencyType || '')
    .toLowerCase()
    .includes('vezes')
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
