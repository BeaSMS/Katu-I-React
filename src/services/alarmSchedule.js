import { getContraceptivePattern } from './contraceptive.js'

export function buildMedicationSchedule(medication) {
  if (!medication?.id && !medication?._id) throw new Error('O servidor não retornou o ID do medicamento para vincular o alarme.')
  if (!medication.date || !medication.time) throw new Error('Informe a data e o horário inicial do medicamento para calcular os alarmes.')

  const startAt = new Date(`${medication.date}T${medication.time}`)
  if (Number.isNaN(startAt.getTime())) throw new Error('A data ou o horário inicial do medicamento é inválido.')

  const frequency = Number(medication.frequencyValue)
  if (!Number.isInteger(frequency) || frequency < 1 || frequency > 24) {
    throw new Error('A frequência do medicamento deve ser um número entre 1 e 24.')
  }
  const intervalMinutes = String(medication.frequencyType || '').toLowerCase().includes('vezes')
    ? Math.round((24 * 60) / frequency)
    : frequency * 60
  if (intervalMinutes < 1 || intervalMinutes > 1440) throw new Error('O intervalo do medicamento precisa ficar entre 1 minuto e 24 horas.')

  const durationDays = medication.continuous ? null : Number(medication.durationDays)
  if (durationDays !== null && (!Number.isInteger(durationDays) || durationDays < 1 || durationDays > 3650)) {
    throw new Error('A duração do medicamento deve ser entre 1 e 3650 dias.')
  }
  const contraceptivePattern = getContraceptivePattern(medication.contraceptivePackType)
  return {
    medicationId: medication.id || medication._id,
    medicationName: medication.name,
    localDate: medication.date,
    localTime: medication.time,
    startAt: startAt.toISOString(),
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    intervalMinutes,
    durationDays,
    ...(contraceptivePattern?.pauseDays ? {
      cycleActiveDays: contraceptivePattern.activeDays,
      cyclePauseDays: contraceptivePattern.pauseDays,
    } : {}),
  }
}
