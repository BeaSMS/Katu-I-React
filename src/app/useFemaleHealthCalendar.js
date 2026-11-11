import { dateKeyForFemaleHealth } from '../services/femaleHealth'

export function useFemaleHealthCalendar(calendarMonth, setCalendarMonth, cycleInsights) {
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

  return { calendarDays, changeCalendarMonth, formatDate, formatPrediction }
}
