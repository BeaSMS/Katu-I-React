const contraceptivePatterns = {
  '21': { packType: '21', activeDays: 21, pauseDays: 7, cycleDays: 28, label: '21 dias de uso + 7 dias de pausa' },
  '24': { packType: '24', activeDays: 24, pauseDays: 4, cycleDays: 28, label: '24 dias de uso + 4 dias de pausa' },
  '28': { packType: '28', activeDays: 28, pauseDays: 0, cycleDays: 28, label: '28 dias, sem pausa entre cartelas' },
  continuous: { packType: 'continuous', activeDays: null, pauseDays: 0, cycleDays: null, label: 'Uso contínuo, sem pausa' },
}

export function getContraceptivePattern(packType) {
  return contraceptivePatterns[String(packType)] || null
}

export function getContraceptiveDay(startDate, currentDate, packType) {
  const pattern = getContraceptivePattern(packType)
  if (!pattern || !startDate || !currentDate || !pattern.cycleDays) return null
  const start = new Date(`${String(startDate).slice(0, 10)}T00:00:00`)
  const current = new Date(`${String(currentDate).slice(0, 10)}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(current.getTime())) return null
  const elapsedDays = Math.round((current - start) / 86_400_000)
  if (elapsedDays < 0) return { phase: 'not-started', daysUntilStart: -elapsedDays }
  const cycleDay = elapsedDays % pattern.cycleDays
  if (cycleDay < pattern.activeDays) return { phase: 'active', day: cycleDay + 1, total: pattern.activeDays }
  return { phase: 'pause', day: cycleDay - pattern.activeDays + 1, total: pattern.pauseDays }
}

export function getContraceptivePatternDescription(medication, currentDate) {
  const packType = medication?.contraceptivePackType
  const pattern = getContraceptivePattern(packType)
  if (!pattern) return null
  const day = getContraceptiveDay(medication.date, currentDate, packType)
  if (!day) return pattern.label
  if (day.phase === 'not-started') return `${pattern.label} · começa em ${day.daysUntilStart} ${day.daysUntilStart === 1 ? 'dia' : 'dias'}`
  return day.phase === 'active'
    ? `${pattern.label} · dia ${day.day} de ${day.total} da cartela`
    : `${pattern.label} · pausa, dia ${day.day} de ${day.total}`
}
