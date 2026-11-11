import test from 'node:test'
import assert from 'node:assert/strict'
import { getRecordDetailLabel } from '../src/app/ui/recordDetailLabels.js'

test('exibe rótulos de medicamentos em português', () => {
  assert.equal(getRecordDetailLabel('frequencyType', 'medications'), 'Tipo de frequência')
  assert.equal(getRecordDetailLabel('date', 'medications'), 'Data')
  assert.equal(getRecordDetailLabel('startDate', 'medications'), 'Data de início')
  assert.equal(getRecordDetailLabel('durationDays', 'medications'), 'Duração (dias)')
  assert.equal(getRecordDetailLabel('generateAlarms', 'medications'), 'Criar alarmes automaticamente')
})

test('mantém o rótulo original para campos desconhecidos e outros recursos', () => {
  assert.equal(getRecordDetailLabel('custom_field', 'medications'), 'custom field')
  assert.equal(getRecordDetailLabel('frequencyType', 'appointments'), 'frequencyType')
})
