import test from 'node:test'
import assert from 'node:assert/strict'
import { getDueLocalReminders } from '../src/services/localReminders.js'

test('localiza alarmes vencidos recentemente e usa o título salvo', () => {
  const now = new Date(2026, 10, 11, 15, 2).getTime()
  const reminders = getDueLocalReminders(
    {
      alarms: [
        { id: 'checkup', name: 'Consulta de rotina', date: '2026-11-11', time: '15:00' },
        { id: 'cancelled', name: 'Alarme cancelado', date: '2026-11-11', time: '15:00', status: 'Cancelado' },
      ],
    },
    now,
  )

  assert.equal(reminders.length, 1)
  assert.equal(reminders[0].title, 'Consulta de rotina')
})

test('entrega lembretes de medicamentos durante uma janela de atraso curta', () => {
  const now = new Date(2026, 10, 11, 15, 4).getTime()
  const reminders = getDueLocalReminders(
    {
      alarms: [],
      medications: [
        {
          id: 'vitamin-d',
          name: 'Vitamina D',
          date: '2026-11-11',
          time: '15:00',
          frequencyType: 'A cada intervalo de horas',
          frequencyValue: '8',
          continuous: true,
          generateAlarms: true,
        },
      ],
    },
    now,
  )

  assert.equal(reminders.length, 1)
  assert.equal(reminders[0].body, 'Está na hora de tomar Vitamina D.')
})

test('não entrega alarmes muito atrasados nem medicamentos sem lembrete ativado', () => {
  const now = new Date(2026, 10, 11, 15, 10).getTime()
  const reminders = getDueLocalReminders(
    {
      alarms: [{ id: 'old', date: '2026-11-11', time: '15:00' }],
      medications: [
        {
          id: 'without-alarm',
          name: 'Magnésio',
          date: '2026-11-11',
          time: '15:00',
          frequencyValue: '1',
          generateAlarms: false,
        },
      ],
    },
    now,
  )

  assert.deepEqual(reminders, [])
})
