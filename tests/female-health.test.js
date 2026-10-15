import test from 'node:test'
import assert from 'node:assert/strict'
import { dateKeyForFemaleHealth, getCycleDayKind, getCycleInsights } from '../src/services/femaleHealth.js'

const addDays = (date, days) => new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
const record = (startDate, endDate = '') => ({ recordType: 'menstrual-cycle', startDate: dateKeyForFemaleHealth(startDate), endDate: endDate ? dateKeyForFemaleHealth(endDate) : '' })

test('calculates average cycle and bleeding durations and predicts the next period', () => {
  const today = new Date(2026, 8, 20)
  const cycles = [
    record(new Date(2026, 8, 1), new Date(2026, 8, 5)),
    record(new Date(2026, 7, 4), new Date(2026, 7, 8)),
    record(new Date(2026, 6, 7), new Date(2026, 6, 12)),
  ]

  const result = getCycleInsights(cycles, today)

  assert.equal(result.averageCycleLength, 28)
  assert.equal(result.averagePeriodLength, 5)
  assert.equal(result.completedPeriodCount, 3)
  assert.equal(dateKeyForFemaleHealth(result.nextPeriodDate), '2026-09-29')
  assert.equal(dateKeyForFemaleHealth(result.fertileStart), '2026-09-12')
  assert.equal(dateKeyForFemaleHealth(result.fertileEnd), '2026-09-16')
})

test('ignores cycle intervals outside the supported 15-to-60-day range', () => {
  const cycles = [
    record(new Date(2026, 8, 1)),
    record(new Date(2026, 6, 3)),
    record(new Date(2026, 1, 1)),
  ]

  const result = getCycleInsights(cycles, new Date(2026, 8, 2))

  assert.equal(result.averageCycleLength, 60)
  assert.equal(dateKeyForFemaleHealth(result.nextPeriodDate), '2026-10-31')
})

test('returns no prediction until two valid cycle starts are recorded', () => {
  const result = getCycleInsights([record(new Date(2026, 8, 1))], new Date(2026, 8, 2))

  assert.equal(result.averageCycleLength, null)
  assert.equal(result.completedPeriodCount, 0)
  assert.equal(result.nextPeriodDate, null)
  assert.equal(result.fertileStart, null)
})

test('classifies recorded flow, estimated menstruation, and fertile days', () => {
  const today = new Date(2026, 8, 1)
  const result = getCycleInsights([
    record(new Date(2026, 8, 1)),
    record(new Date(2026, 7, 4), new Date(2026, 7, 8)),
    record(new Date(2026, 6, 7), new Date(2026, 6, 11)),
  ], today)

  assert.equal(getCycleDayKind(new Date(2026, 8, 1), result, today), 'period')
  assert.equal(getCycleDayKind(new Date(2026, 8, 29), result, today), 'predicted-period')
  assert.equal(getCycleDayKind(new Date(2026, 8, 14), result, today), 'fertile')
})

test('only treats an open cycle as current flow up to today', () => {
  const today = new Date(2026, 8, 10)
  const result = getCycleInsights([record(new Date(2026, 8, 8))], today)

  assert.equal(getCycleDayKind(new Date(2026, 8, 10), result, today), 'period')
  assert.equal(getCycleDayKind(addDays(today, 1), result, today), '')
})
