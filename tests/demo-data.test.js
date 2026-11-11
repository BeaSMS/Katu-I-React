import test from 'node:test'
import assert from 'node:assert/strict'
import { createDemoData, demoCredentials, ensureDemoVitalHistory } from '../src/services/api/demoData.js'

test('demonstração cria dois perfis com registros isolados', () => {
  const demo = createDemoData()
  const [ana, lucas] = demo.profiles

  assert.equal(demo.profiles.length, 2)
  assert.equal(demo.activeProfileId, ana.id)
  assert.notDeepEqual(demo.recordsByProfile[ana.id], demo.recordsByProfile[lucas.id])
  assert.equal(demo.recordsByProfile[ana.id].medications[0].name, 'Vitamina D')
  assert.equal(demo.recordsByProfile[lucas.id].medications[0].name, 'Xarope respiratório')

  for (const profile of [ana, lucas]) {
    const records = demo.recordsByProfile[profile.id]['vital-signs']
    assert.equal(records.length, 35)
    for (const type of ['Pressão arterial', 'Glicose', 'Batimentos', 'Saturação', 'Temperatura', 'Peso', 'Altura']) {
      assert.equal(records.filter((record) => record.type === type).length, 5)
    }
  }
})

test('acrescenta medições de demonstração a dados existentes sem duplicar registros', () => {
  const demo = createDemoData()
  for (const profile of demo.profiles) {
    demo.recordsByProfile[profile.id]['vital-signs'] = [
      { id: `${profile.id}-old-vital`, type: 'Peso', date: '2026-10-10', value: 60 },
    ]
  }

  assert.equal(ensureDemoVitalHistory(demo), true)
  for (const profile of demo.profiles) {
    const records = demo.recordsByProfile[profile.id]['vital-signs']
    assert.equal(records.length, 36)
    assert.equal(records.filter((record) => record.id === `${profile.id}-old-vital`).length, 1)
  }
  assert.equal(ensureDemoVitalHistory(demo), false)
})

test('anexos demonstrativos são PDFs locais e possuem credenciais identificáveis', () => {
  const demo = createDemoData()
  const document = demo.recordsByProfile[demo.activeProfileId].documents[0]

  assert.match(demoCredentials.email, /@/)
  assert.ok(demoCredentials.password.length >= 8)
  assert.equal(document.attachment.contentType, 'application/pdf')
  assert.match(document.attachment.dataUrl, /^data:application\/pdf;base64,/)
})
