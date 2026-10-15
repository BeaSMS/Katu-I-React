// Contratos da aplicação. Cada tela usa um recurso estável para que a API Node
// possa ser implementada sem acoplar os componentes à URL do servidor.
export const resourceConfig = {
  overview: { endpoint: '/overview', label: 'Visão geral' },
  medications: { endpoint: '/medications', label: 'Medicamentos' },
  alarms: { endpoint: '/alarms', label: 'Alarmes' },
  prescriptions: { endpoint: '/prescriptions', label: 'Receitas' },
  vitalSigns: { endpoint: '/vital-signs', label: 'Sinais vitais' },
  symptoms: { endpoint: '/symptoms', label: 'Diário de sintomas' },
  feminineHealth: { endpoint: '/feminine-health', label: 'Saúde feminina' },
  pregnancy: { endpoint: '/pregnancy', label: 'Gestação' },
  appointments: { endpoint: '/appointments', label: 'Consultas' },
  vaccines: { endpoint: '/vaccines', label: 'Carteira de vacinação' },
  exams: { endpoint: '/exams', label: 'Exames' },
  documents: { endpoint: '/documents', label: 'Documentos' },
  reports: { endpoint: '/reports', label: 'Relatórios PDF' },
  profiles: { endpoint: '/profiles', label: 'Perfis e dependentes' },
  backup: { endpoint: '/backup', label: 'Backup pessoal' },
}

export const moduleNavigation = [
  { label: 'Visão geral', icon: 'grid', resource: 'overview' },
  { label: 'Medicamentos', icon: 'pill', resource: 'medications' },
  { label: 'Alarmes', icon: 'bell', resource: 'alarms' },
  { label: 'Receitas', icon: 'file', resource: 'prescriptions' },
  { label: 'Diário de sintomas', icon: 'pulse', resource: 'symptoms' },
  { label: 'Sinais vitais', icon: 'pulse', resource: 'vitalSigns' },
  { label: 'Consultas', icon: 'calendar', resource: 'appointments' },
  { label: 'Exames', icon: 'file', resource: 'exams' },
]

export const supportNavigation = [
  { label: 'Documentos', icon: 'file', resource: 'documents' },
  { label: 'Saúde feminina', icon: 'calendar', resource: 'feminineHealth' },
  { label: 'Carteira de vacinação', icon: 'file', resource: 'vaccines' },
  { label: 'Perfis e dependentes', icon: 'profile', resource: 'profiles' },
  { label: 'Backup pessoal', icon: 'file', resource: 'backup' },
]

export const settingsNavigation = [
  { label: 'Configurações', icon: 'sliders', resource: 'backup' },
]

export function getResourceForLabel(label) {
  return Object.values(resourceConfig).find((resource) => resource.label === label) || null
}
