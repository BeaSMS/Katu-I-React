const medicationDetailLabels = {
  name: 'Nome do medicamento',
  dosage: 'Dosagem',
  purpose: 'Finalidade',
  date: 'Data',
  startDate: 'Data de início',
  endDate: 'Data de término',
  time: 'Horário',
  frequencyType: 'Tipo de frequência',
  frequencyValue: 'Quantidade da frequência',
  continuous: 'Uso contínuo',
  durationDays: 'Duração (dias)',
  quantity: 'Quantidade',
  minimumStock: 'Estoque mínimo',
  stock: 'Estoque',
  dosePerTake: 'Doses por tomada',
  unit: 'Unidade de medida',
  generateAlarms: 'Criar alarmes automaticamente',
  status: 'Status',
  notes: 'Observações',
}

export function getRecordDetailLabel(key, resource) {
  return resource === 'medications' ? medicationDetailLabels[key] || key.replaceAll('_', ' ') : key.replaceAll('_', ' ')
}
