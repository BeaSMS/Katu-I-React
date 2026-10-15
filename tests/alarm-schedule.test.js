import assert from 'node:assert/strict'
import test from 'node:test'
import { buildMedicationSchedule } from '../src/services/alarmSchedule.js'
import { advanceSchedule, createSchedule } from '../server/alarm-schedule.js'

const medication = {
  id: 'med-1',
  name: 'Losartana',
  date: '2026-10-20',
  time: '08:00',
  frequencyType: 'A cada intervalo de horas',
  frequencyValue: '8',
  durationDays: '7',
}

test('calculates every-eight-hours medication schedule for seven days', () => {
  const schedule = buildMedicationSchedule(medication)
  assert.equal(schedule.medicationId, 'med-1')
  assert.equal(schedule.medicationName, 'Losartana')
  assert.equal(schedule.intervalMinutes, 480)
  assert.equal(schedule.durationDays, 7)
  assert.equal(new Date(schedule.startAt).getMinutes(), 0)
})

test('converts doses per day into evenly spaced reminders', () => {
  const schedule = buildMedicationSchedule({ ...medication, frequencyType: 'Vezes ao dia', frequencyValue: '3' })
  assert.equal(schedule.intervalMinutes, 480)
})

test('keeps continuous medication schedules open-ended', () => {
  const schedule = buildMedicationSchedule({ ...medication, continuous: true })
  assert.equal(schedule.durationDays, null)
})

test('builds a contraceptive reminder schedule with active and pause days', () => {
  const schedule = buildMedicationSchedule({
    ...medication,
    date: '2026-10-20',
    time: '08:00',
    continuous: true,
    frequencyType: 'Vezes ao dia',
    frequencyValue: '1',
    contraceptivePackType: '21',
  })
  assert.equal(schedule.intervalMinutes, 1440)
  assert.equal(schedule.cycleActiveDays, 21)
  assert.equal(schedule.cyclePauseDays, 7)
})

test('pauses contraceptive reminders during the break and resumes on the next pack', () => {
  const startAt = '2026-10-01T08:00:00.000Z'
  const start = Date.parse(startAt)
  const schedule = createSchedule({
    ownerId: 'user-1',
    profileId: 'profile-1',
    sourceId: 'contraceptive-1',
    startAt,
    intervalMinutes: 1440,
    cycleActiveDays: 21,
    cyclePauseDays: 7,
  }, start - 60_000)

  advanceSchedule(schedule, start + 21 * 86_400_000)

  assert.equal(schedule.nextAt, '2026-10-29T08:00:00.000Z')
  assert.equal(schedule.enabled, true)
})

test('rejects missing IDs, invalid dose intervals, and missing finite duration', () => {
  assert.throws(() => buildMedicationSchedule({ ...medication, id: '' }), /ID do medicamento/)
  assert.throws(() => buildMedicationSchedule({ ...medication, frequencyValue: '25' }), /entre 1 e 24/)
  assert.throws(() => buildMedicationSchedule({ ...medication, durationDays: '' }), /duração/)
})

test('advances a delayed recurring schedule without sending a burst of missed reminders', () => {
  const now = Date.parse('2026-10-20T17:30:00.000Z')
  const schedule = createSchedule({
    ownerId: 'user-1',
    profileId: 'profile-1',
    sourceId: 'med-1',
    startAt: '2026-10-20T08:00:00.000Z',
    intervalMinutes: 480,
    durationDays: 7,
  }, Date.parse('2026-10-20T07:00:00.000Z'))
  advanceSchedule(schedule, now)
  assert.equal(schedule.nextAt, '2026-10-21T00:00:00.000Z')
  assert.equal(schedule.enabled, true)
})

test('resumes an active medication schedule at its next interval and rejects expired courses', () => {
  const now = Date.parse('2026-10-20T17:30:00.000Z')
  const schedule = createSchedule({
    ownerId: 'user-1',
    profileId: 'profile-1',
    sourceId: 'med-1',
    startAt: '2026-10-20T08:00:00.000Z',
    intervalMinutes: 480,
    durationDays: 7,
  }, now)
  assert.equal(schedule.nextAt, '2026-10-21T00:00:00.000Z')
  assert.throws(() => createSchedule({
    ownerId: 'user-1',
    profileId: 'profile-1',
    sourceId: 'med-expired',
    startAt: '2026-10-19T08:00:00.000Z',
    intervalMinutes: 480,
    durationDays: 1,
  }, now), /período.*terminou/)
})

test('disables one-time alarms after delivery and finite schedules after their last dose', () => {
  const once = createSchedule({
    ownerId: 'user-1',
    profileId: 'profile-1',
    sourceId: 'alarm-1',
    startAt: '2026-10-20T08:00:00.000Z',
    oneTime: true,
  }, Date.parse('2026-10-20T07:00:00.000Z'))
  advanceSchedule(once, Date.parse('2026-10-20T08:00:00.000Z'))
  assert.equal(once.enabled, false)

  const finite = createSchedule({
    ownerId: 'user-1',
    profileId: 'profile-1',
    sourceId: 'med-2',
    startAt: '2026-10-20T08:00:00.000Z',
    intervalMinutes: 1440,
    durationDays: 1,
  }, Date.parse('2026-10-20T07:00:00.000Z'))
  advanceSchedule(finite, Date.parse('2026-10-20T08:00:00.000Z'))
  assert.equal(finite.enabled, false)
})
