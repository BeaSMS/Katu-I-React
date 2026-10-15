import assert from 'node:assert/strict'
import test from 'node:test'
import { getContraceptiveDay, getContraceptivePattern, getContraceptivePatternDescription } from '../src/services/contraceptive.js'

test('maps supported contraceptive packs to active and pause day patterns', () => {
  assert.deepEqual(getContraceptivePattern('21'), {
    packType: '21',
    activeDays: 21,
    pauseDays: 7,
    cycleDays: 28,
    label: '21 dias de uso + 7 dias de pausa',
  })
  assert.equal(getContraceptivePattern('24').pauseDays, 4)
  assert.equal(getContraceptivePattern('28').pauseDays, 0)
  assert.equal(getContraceptivePattern('continuous').cycleDays, null)
  assert.equal(getContraceptivePattern('unknown'), null)
})

test('calculates use, pause, and before-start status for a 21-day pack', () => {
  assert.deepEqual(getContraceptiveDay('2026-10-01', '2026-10-21', '21'), {
    phase: 'active',
    day: 21,
    total: 21,
  })
  assert.deepEqual(getContraceptiveDay('2026-10-01', '2026-10-22', '21'), {
    phase: 'pause',
    day: 1,
    total: 7,
  })
  assert.deepEqual(getContraceptiveDay('2026-10-01', '2026-10-29', '21'), {
    phase: 'active',
    day: 1,
    total: 21,
  })
  assert.deepEqual(getContraceptiveDay('2026-10-01', '2026-09-30', '21'), {
    phase: 'not-started',
    daysUntilStart: 1,
  })
})

test('describes a pill pack and its current day clearly', () => {
  assert.equal(
    getContraceptivePatternDescription({ date: '2026-10-01', contraceptivePackType: '24' }, '2026-10-25'),
    '24 dias de uso + 4 dias de pausa · pausa, dia 1 de 4',
  )
  assert.equal(
    getContraceptivePatternDescription({ date: '2026-10-01', contraceptivePackType: 'continuous' }, '2026-10-25'),
    'Uso contínuo, sem pausa',
  )
})
