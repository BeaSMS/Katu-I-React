const DAY_MS = 86_400_000

function dateAtLocalMidnight(value) {
  if (!value) return null
  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`)
  return Number.isNaN(date.getTime()) ? null : date
}

function dateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function getCycleInsights(records, today = new Date()) {
  const cycles = (Array.isArray(records) ? records : [])
    .filter((record) => record.recordType === 'menstrual-cycle' || record.startDate)
    .map((record) => ({
      ...record,
      startDate: String(record.startDate || record.date || '').slice(0, 10),
      endDate: record.endDate ? String(record.endDate).slice(0, 10) : '',
    }))
    .filter((cycle) => dateAtLocalMidnight(cycle.startDate))
    .sort((first, second) => second.startDate.localeCompare(first.startDate))

  const intervals = cycles.slice(0, -1).map((cycle, index) => {
    const currentStart = dateAtLocalMidnight(cycle.startDate)
    const previousStart = dateAtLocalMidnight(cycles[index + 1].startDate)
    return Math.round((currentStart - previousStart) / DAY_MS)
  }).filter((days) => days >= 15 && days <= 60)
  const averageCycleLength = intervals.length
    ? Math.round(intervals.reduce((sum, days) => sum + days, 0) / intervals.length)
    : null
  const periodLengths = cycles.filter((cycle) => cycle.endDate).map((cycle) => {
    const start = dateAtLocalMidnight(cycle.startDate)
    const end = dateAtLocalMidnight(cycle.endDate)
    return end && end >= start ? Math.round((end - start) / DAY_MS) + 1 : null
  }).filter((days) => days >= 1 && days <= 14)
  const averagePeriodLength = periodLengths.length
    ? Math.round(periodLengths.reduce((sum, days) => sum + days, 0) / periodLengths.length)
    : 5

  if (!averageCycleLength || !cycles.length) {
    return { cycles, averageCycleLength: null, averagePeriodLength, completedPeriodCount: periodLengths.length, nextPeriodDate: null, fertileStart: null, fertileEnd: null, ovulationDate: null }
  }
  const lastStart = dateAtLocalMidnight(cycles[0].startDate)
  const nextPeriodDate = new Date(lastStart.getTime() + averageCycleLength * DAY_MS)
  const ovulationDate = new Date(nextPeriodDate.getTime() - 14 * DAY_MS)
  const fertileStart = new Date(ovulationDate.getTime() - 3 * DAY_MS)
  const fertileEnd = new Date(ovulationDate.getTime() + DAY_MS)
  return { cycles, averageCycleLength, averagePeriodLength, completedPeriodCount: periodLengths.length, nextPeriodDate, fertileStart, fertileEnd, ovulationDate, today: dateKey(today) }
}

export function getCycleDayKind(date, insights, today = new Date()) {
  const key = dateKey(date)
  const currentKey = dateKey(today)
  const actualPeriod = insights.cycles.some((cycle) => {
    if (key < cycle.startDate) return false
    const end = cycle.endDate || (key <= currentKey ? currentKey : cycle.startDate)
    return key <= end
  })
  if (actualPeriod) return 'period'
  const predictedStart = insights.nextPeriodDate
  if (predictedStart) {
    const estimatedEnd = new Date(predictedStart.getTime() + (insights.averagePeriodLength - 1) * DAY_MS)
    if (date >= predictedStart && date <= estimatedEnd) return 'predicted-period'
  }
  if (insights.fertileStart && date >= insights.fertileStart && date <= insights.fertileEnd) return 'fertile'
  return ''
}

export function dateKeyForFemaleHealth(date) {
  return dateKey(date)
}
