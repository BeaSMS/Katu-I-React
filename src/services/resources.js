// Contratos da aplicação. Cada tela usa um recurso estável para que a API Node
// possa ser implementada sem acoplar os componentes à URL do servidor.
export const resourceConfig = {
  overview: { endpoint: '/overview', label: 'Visão geral' },
  medications: { endpoint: '/medications', label: 'Medicamentos', resource: 'medications' },
  alarms: { endpoint: '/alarms', label: 'Alarmes', resource: 'alarms' },
  prescriptions: { endpoint: '/prescriptions', label: 'Receitas', resource: 'prescriptions' },
  vitalSigns: { endpoint: '/vital-signs', label: 'Sinais vitais', resource: 'vitalSigns' },
  symptoms: { endpoint: '/symptoms', label: 'Diário de sintomas', resource: 'symptoms' },
  feminineHealth: { endpoint: '/feminine-health', label: 'Saúde feminina', resource: 'feminineHealth' },
  pregnancy: { endpoint: '/pregnancy', label: 'Gestação', resource: 'pregnancy' },
  appointments: { endpoint: '/appointments', label: 'Consultas', resource: 'appointments' },
  vaccines: { endpoint: '/vaccines', label: 'Carteira de vacinação', resource: 'vaccines' },
  exams: { endpoint: '/exams', label: 'Exames', resource: 'exams' },
  documents: { endpoint: '/documents', label: 'Documentos', resource: 'documents' },
  reports: { endpoint: '/reports', label: 'Relatórios PDF', resource: 'reports' },
  profiles: { endpoint: '/profiles', label: 'Perfis e dependentes', resource: 'profiles' },
  backup: { endpoint: '/backup', label: 'Backup pessoal', resource: 'backup' },
  settings: { endpoint: '/settings', label: 'Configurações', resource: 'settings' },
  help: { endpoint: '/help', label: 'Ajuda', resource: 'help' },
}

export const moduleNavigation = [
  { label: 'Visão geral', icon: 'grid', resource: 'overview' },
  { label: 'Medicamentos', icon: 'pill', resource: 'medications' },
  { label: 'Alarmes', icon: 'bell', resource: 'alarms' },
  { label: 'Receitas', icon: 'receipt', resource: 'prescriptions' },
  { label: 'Diário de sintomas', icon: 'journal', resource: 'symptoms' },
  { label: 'Sinais vitais', icon: 'pulse', resource: 'vitalSigns' },
  { label: 'Consultas', icon: 'calendar', resource: 'appointments' },
  { label: 'Exames', icon: 'file', resource: 'exams' },
]

export const supportNavigation = [
  { label: 'Documentos', icon: 'folder', resource: 'documents' },
  { label: 'Saúde feminina', icon: 'calendar', resource: 'feminineHealth' },
  { label: 'Carteira de vacinação', icon: 'vaccination', resource: 'vaccines' },
  { label: 'Perfis e dependentes', icon: 'profile', resource: 'profiles' },
  { label: 'Backup pessoal', icon: 'file', resource: 'backup' },
]

export const settingsNavigation = [
  { label: 'Configurações', icon: 'settings', resource: 'settings' },
  { label: 'Ajuda', icon: 'help', resource: 'help' },
]

export function getResourceForLabel(label) {
  return Object.values(resourceConfig).find((resource) => resource.label === label) || null
}
