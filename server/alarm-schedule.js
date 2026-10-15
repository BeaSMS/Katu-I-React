import { randomUUID } from 'node:crypto'

export function createSchedule({ ownerId, profileId, sourceId, startAt, intervalMinutes, durationDays, cycleActiveDays, cyclePauseDays, title, displayName, localDate, localTime, timeZone, oneTime }, now = Date.now()) {
  const firstOccurrence = new Date(startAt)
  const interval = intervalMinutes == null ? null : Number(intervalMinutes)
  const duration = durationDays == null ? null : Number(durationDays)
  if (!sourceId) throw new Error('O alarme precisa estar vinculado a um registro.')
  if (!startAt || Number.isNaN(firstOccurrence.getTime()) || (oneTime && firstOccurrence.getTime() <= now)) throw new Error('O horário inicial do alarme precisa estar no futuro.')
  if (!oneTime && (!Number.isInteger(interval) || interval < 1 || interval > 1440)) throw new Error('O intervalo deve ser entre 1 e 1440 minutos.')
  if (duration !== null && (!Number.isInteger(duration) || duration < 1 || duration > 3650)) throw new Error('A duração deve ser entre 1 e 3650 dias.')
  if (oneTime && interval !== null) throw new Error('Um alarme pontual não pode ter repetição.')
  const activeDays = cycleActiveDays == null ? null : Number(cycleActiveDays)
  const pauseDays = cyclePauseDays == null ? null : Number(cyclePauseDays)
  if ((activeDays === null) !== (pauseDays === null)
    || (activeDays !== null && (!Number.isInteger(activeDays) || activeDays < 1 || activeDays > 28 || !Number.isInteger(pauseDays) || pauseDays < 0 || pauseDays > 7 || interval !== 1440 || duration !== null))) {
    throw new Error('O padrão de cartela exige lembrete diário, uso contínuo e dias ativos/pausa válidos.')
  }
  if (activeDays !== null && pauseDays === 0) throw new Error('Informe uma pausa maior que zero para um padrão de cartela.')

  const endAt = oneTime ? firstOccurrence.getTime() : duration === null ? null : firstOccurrence.getTime() + duration * 86_400_000
  let nextTime = firstOccurrence.getTime()
  if (!oneTime && nextTime <= now) {
    const intervalMs = interval * 60_000
    nextTime += (Math.floor((now - nextTime) / intervalMs) + 1) * intervalMs
  }
  if (activeDays !== null) {
    const intervalMs = interval * 60_000
    const cycleDays = activeDays + pauseDays
    while (Math.floor((nextTime - firstOccurrence.getTime()) / intervalMs) % cycleDays >= activeDays) nextTime += intervalMs
  }
  if (!oneTime && endAt !== null && nextTime >= endAt) throw new Error('O período deste alarme já terminou.')
  return {
    id: randomUUID(),
    sourceId: sourceId ? String(sourceId) : null,
    ownerId,
    profileId,
    title: title || 'Seu lembrete de saúde',
    displayName: displayName || null,
    startAt: firstOccurrence.toISOString(),
    localDate: localDate || firstOccurrence.toISOString().slice(0, 10),
    localTime: localTime || firstOccurrence.toISOString().slice(11, 16),
    timeZone: timeZone || 'UTC',
    nextAt: new Date(nextTime).toISOString(),
    endAt: endAt === null ? null : new Date(endAt).toISOString(),
    intervalMinutes: interval,
    cycleActiveDays: activeDays,
    cyclePauseDays: pauseDays,
    oneTime: Boolean(oneTime),
    enabled: true,
    createdAt: new Date(now).toISOString(),
  }
}

export function advanceSchedule(schedule, now) {
  if (schedule.oneTime) {
    schedule.enabled = false
    schedule.nextAt = null
    return
  }
  const intervalMs = schedule.intervalMinutes * 60_000
  let nextTime = new Date(schedule.nextAt).getTime() + intervalMs
  while (nextTime <= now) nextTime += intervalMs
  if (schedule.cycleActiveDays != null && schedule.cyclePauseDays != null) {
    const cycleDays = schedule.cycleActiveDays + schedule.cyclePauseDays
    while (Math.floor((nextTime - new Date(schedule.startAt).getTime()) / intervalMs) % cycleDays >= schedule.cycleActiveDays) nextTime += intervalMs
  }
  if (schedule.endAt && nextTime >= new Date(schedule.endAt).getTime()) {
    schedule.enabled = false
    schedule.nextAt = null
  } else {
    schedule.nextAt = new Date(nextTime).toISOString()
  }
}
