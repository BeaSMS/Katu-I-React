const DUE_WINDOW_MS = 5 * 60_000
const inactiveAlarmStatuses = new Set(['concluído', 'concluido', 'cancelado', 'cancelada', 'desativado'])

export function getDueLocalReminders(data, now = Date.now()) {
  const reminders = []
  const alarms = Array.isArray(data.alarms) ? data.alarms : []
  alarms.forEach((alarm) => {
    if (!alarm.date || !alarm.time || inactiveAlarmStatuses.has(String(alarm.status || '').toLowerCase())) return
    const dueAt = new Date(`${alarm.date}T${alarm.time}`).getTime()
    if (dueAt <= now && now - dueAt < DUE_WINDOW_MS)
      reminders.push({
        key: `alarm:${alarm.id || ''}:${dueAt}`,
        title: alarm.name || alarm.title || 'Lembrete de saúde',
        body: alarm.notes || 'Você tem um cuidado agendado no Katu’I.',
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
    if (dueAt <= now && now - dueAt < DUE_WINDOW_MS && dueAt < endAt) {
      reminders.push({
        key: `medication:${medication.id || medication.name}:${dueAt}`,
        title: 'Hora do seu medicamento',
        body: medication.name ? `Está na hora de tomar ${medication.name}.` : 'Abra o Katu’I para conferir seu lembrete.',
      })
    }
  })
  return reminders
}
