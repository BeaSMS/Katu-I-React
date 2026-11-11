import { useEffect, useRef, useState } from 'react'
import {
  apiRequest,
  clearOfflineQueue,
  flushOfflineQueue,
  getOfflineQueueSize,
  isAiPrescriptionEnabled,
  isApiConfigured,
  isDemoMode,
  isGuestMode,
  isNetworkError,
  queueOfflineAction,
  resourceApi,
} from '../../services/api'
import { moduleNavigation, supportNavigation, getResourceForLabel } from '../../services/resources'
import { medications } from './dadosAplicacao'
import {
  Icon,
  EmptyState,
  localDateKey,
  parseDateKey,
  hasCalendarRecordOnDate,
  calendarWeekdays,
  calendarMonths,
} from './interfaceCompartilhada'
import { VitalsPage } from './sinaisVitais'
import { FemaleHealthPage } from './saudeFeminina'
import { ResourcePage } from './recursos'
import { SettingsPage, BackupPage } from './configuracoes'

export const calendarResources = [
  { resource: 'medications', endpoint: '/medications', label: 'Medicamentos' },
  { resource: 'alarms', endpoint: '/alarms', label: 'Alarmes' },
  { resource: 'prescriptions', endpoint: '/prescriptions', label: 'Receitas' },
  { resource: 'symptoms', endpoint: '/symptoms', label: 'Sintomas' },
  { resource: 'vitalSigns', endpoint: '/vital-signs', label: 'Sinais vitais' },
  { resource: 'appointments', endpoint: '/appointments', label: 'Consultas' },
  { resource: 'exams', endpoint: '/exams', label: 'Exames' },
]

export function CalendarCard({
  selectedDate,
  onDateSelect,
  records = [],
  resource = '',
  title = 'Calendário',
  expanded: expandedProp,
  onExpandedChange,
  cardRef,
  loadError = '',
}) {
  const [internalExpanded, setInternalExpanded] = useState(false)
  const expanded = expandedProp ?? internalExpanded
  const setExpanded = (value) => {
    const nextExpanded = typeof value === 'function' ? value(expanded) : value
    if (onExpandedChange) onExpandedChange(nextExpanded)
    else setInternalExpanded(nextExpanded)
  }
  const selected = parseDateKey(selectedDate) || new Date()
  const monthStart = new Date(selected.getFullYear(), selected.getMonth(), 1)
  const daysInMonth = new Date(selected.getFullYear(), selected.getMonth() + 1, 0).getDate()
  const selectedMonth = calendarMonths[selected.getMonth()]
  const selectedMonthLabel = `${selectedMonth.charAt(0).toUpperCase()}${selectedMonth.slice(1)}`
  const weekStart = new Date(selected)
  weekStart.setDate(selected.getDate() - selected.getDay())
  const shiftMonth = (amount) => {
    const target = new Date(selected.getFullYear(), selected.getMonth() + amount, 1)
    target.setDate(Math.min(selected.getDate(), new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()))
    onDateSelect(localDateKey(target))
  }
  const dayButton = (date, compact = false) => {
    const key = localDateKey(date)
    const isSelected = key === selectedDate
    const types = [
      ...new Set(
        records
          .filter((record) => hasCalendarRecordOnDate([record], key, resource))
          .map((record) => record.__calendarResource || resource)
          .filter(Boolean),
      ),
    ]
    const hasAlarm = types.includes('alarms')
    const hasAgenda = types.includes('appointments') || types.includes('exams')
    const activityLabel = types.length
      ? `, registros: ${types.map((type) => calendarResources.find((item) => item.resource === type)?.label || type).join(', ')}`
      : ''
    const scheduleLabel = `${hasAlarm ? ', alarme configurado' : ''}${hasAgenda ? ', item agendado' : ''}`
    return (
      <button
        key={key}
        type="button"
        className={`calendar-day${compact ? ' compact' : ''}${isSelected ? ' selected' : ''}`}
        aria-pressed={isSelected}
        aria-label={`${calendarWeekdays[date.getDay()]}, ${date.getDate()} ${calendarMonths[date.getMonth()]}${activityLabel}${scheduleLabel}`}
        onClick={() => onDateSelect(key)}
      >
        {(hasAlarm || hasAgenda) && (
          <span className="calendar-day-badges" aria-hidden="true">
            {hasAlarm && (
              <span className="calendar-day-badge alarm" title="Alarme configurado">
                <Icon name="clock" size={10} />
              </span>
            )}
            {hasAgenda && (
              <span className="calendar-day-badge agenda" title="Consulta ou exame agendado">
                <Icon name="calendar" size={10} />
              </span>
            )}
          </span>
        )}
        <span className="calendar-day-name">{calendarWeekdays[date.getDay()]}</span>
        <strong>{date.getDate()}</strong>
        {types.length > 0 && (
          <span className="calendar-day-dots" aria-hidden="true">
            {types.map((type) => (
              <i key={type} className={`calendar-day-dot ${type}`} />
            ))}
          </span>
        )}
      </button>
    )
  }
  const monthDays = Array.from({ length: monthStart.getDay() + daysInMonth }, (_, index) => {
    const day = index - monthStart.getDay() + 1
    return day > 0 ? new Date(selected.getFullYear(), selected.getMonth(), day) : null
  })
  return (
    <section
      ref={cardRef}
      className={`calendar-card${expanded ? ' expanded' : ''}`}
      aria-label={`${title}, ${calendarMonths[selected.getMonth()]} de ${selected.getFullYear()}`}
    >
      <div className="calendar-card-header">
        <div className="calendar-month-label">
          <Icon name="calendar" size={18} />
          <span>
            {title}
            <strong>
              Vendo dados de {selected.getDate()} de {selectedMonthLabel} de {selected.getFullYear()}
            </strong>
          </span>
        </div>
        <div className="calendar-controls">
          <button type="button" aria-label="Mês anterior" onClick={() => shiftMonth(-1)}>
            <Icon name="chevron" size={16} />
          </button>
          <button type="button" aria-label="Próximo mês" onClick={() => shiftMonth(1)}>
            <Icon name="chevron" size={16} />
          </button>
        </div>
      </div>
      {!expanded ? (
        <div className="calendar-week">
          {Array.from({ length: 7 }, (_, offset) => {
            const date = new Date(weekStart)
            date.setDate(weekStart.getDate() + offset)
            return dayButton(date, true)
          })}
        </div>
      ) : (
        <>
          <div className="calendar-weekday-headings">
            {calendarWeekdays.map((day) => (
              <span key={day}>{day}</span>
            ))}
          </div>
          <div className="calendar-month-grid">
            {monthDays.map((date, index) =>
              date ? dayButton(date) : <span key={`blank-${index}`} aria-hidden="true" />,
            )}
          </div>
        </>
      )}
      <button
        className="calendar-selection"
        type="button"
        aria-expanded={expanded}
        onClick={() => setExpanded((value) => !value)}
      >
        <strong>{expanded ? 'Recolher calendário' : 'Ver mês completo'}</strong>
        <Icon name="chevron" size={16} />
      </button>
      {loadError && (
        <p className="calendar-load-error" role="status">
          {loadError}
        </p>
      )}
    </section>
  )
}

export function ModulePage({
  active,
  onAction,
  darkMode,
  onToggleDarkMode,
  profile,
  onPregnancyChange,
  selectedDate,
  onDateSelect,
  calendarRecords,
  calendarLoadError,
  isGuest,
  usePushAlarms,
  onExportBackup,
  onImportBackup,
  onSyncBackup,
  backupBusy,
  backupStatus,
}) {
  const aiAvailable = !isGuest && (isDemoMode() || isAiPrescriptionEnabled())
  const pageData = {
    Medicamentos: {
      eyebrow: 'CUIDADO CONTÍNUO',
      title: 'Medicamentos e estoque',
      description: 'Organize seus tratamentos e acompanhe o que precisa ser reposto.',
      icon: 'pill',
      action: 'Adicionar medicamento',
    },
    'Sinais vitais': {
      eyebrow: 'ACOMPANHAMENTO',
      title: 'Sinais vitais',
      description: 'Veja a evolução dos seus registros e acompanhe seus indicadores.',
      icon: 'pulse',
      action: 'Novo registro',
    },
    Consultas: {
      eyebrow: 'SUA AGENDA',
      title: 'Consultas',
      description: 'Tenha seus próximos compromissos de saúde sempre à mão.',
      icon: 'calendar',
      action: 'Agendar consulta',
    },
    Documentos: {
      eyebrow: 'SEUS ARQUIVOS',
      title: 'Documentos de saúde',
      description: 'Receitas, exames e relatórios organizados em um só lugar.',
      icon: 'folder',
      action: 'Adicionar documento',
    },
    Alarmes: {
      eyebrow: 'LEMBRETES INTELIGENTES',
      title: 'Alarmes',
      description: 'Configure lembretes para medicamentos, consultas e cuidados importantes.',
      icon: 'bell',
      action: 'Criar alarme',
    },
    Receitas: {
      eyebrow: 'ORGANIZAÇÃO INTELIGENTE',
      title: 'Receitas',
      description: aiAvailable
        ? 'Organize suas prescrições e, quando quiser, use a leitura inteligente para revisar os dados.'
        : isGuest
          ? 'Organize suas prescrições manualmente neste dispositivo.'
          : 'Organize suas prescrições manualmente com sua conta.',
      icon: 'receipt',
      action: aiAvailable ? 'Ler receita com IA' : 'Adicionar receita',
    },
    Exames: {
      eyebrow: 'SEUS RESULTADOS',
      title: 'Exames',
      description: 'Guarde resultados e acompanhe a evolução dos seus exames de saúde.',
      icon: 'file',
      action: 'Adicionar exame',
    },
    'Perfis e dependentes': {
      eyebrow: 'CUIDADO EM FAMÍLIA',
      title: 'Perfis e dependentes',
      description: 'Gerencie filhos, idosos e outras pessoas que dependem do seu cuidado.',
      icon: 'profile',
      action: 'Adicionar perfil',
    },
    'Backup pessoal': {
      eyebrow: 'SEUS DADOS',
      title: 'Backup pessoal',
      description: 'Exporte, restaure e acompanhe a segurança dos seus dados de saúde.',
      icon: 'file',
      action: 'Criar backup',
    },
    'Diário de sintomas': {
      eyebrow: 'COMO VOCÊ ESTÁ?',
      title: 'Diário de sintomas',
      description: 'Registre seus sintomas e ajude a identificar padrões ao longo do tempo.',
      icon: 'journal',
      action: 'Registrar sintoma',
    },
    'Saúde feminina': {
      eyebrow: 'CUIDADO ÍNTIMO',
      title: 'Saúde feminina',
      description: 'Acompanhe seu ciclo, sintomas e lembretes com privacidade.',
      icon: 'calendar',
      action: null,
    },
    'Carteira de vacinação': {
      eyebrow: 'PROTEÇÃO EM DIA',
      title: 'Carteira de vacinação',
      description: 'Confira suas vacinas e os próximos reforços recomendados.',
      icon: 'vaccination',
      action: 'Adicionar vacina',
    },
    Configurações: {
      eyebrow: 'SEU ESPAÇO',
      title: 'Configurações',
      description: 'Ajuste sua experiência no Katu’I e suas preferências de acessibilidade.',
      icon: 'settings',
      action: null,
    },
  }
  const data = pageData[active] || pageData['Medicamentos']
  if (active === 'Configurações') return <SettingsPage darkMode={darkMode} onToggleDarkMode={onToggleDarkMode} />
  if (active === 'Backup pessoal')
    return (
      <>
        <section className="page-title-row">
          <div>
            <p className="eyebrow">{data.eyebrow}</p>
            <h1>{data.title}</h1>
            <p className="page-description">{data.description}</p>
          </div>
        </section>
        <BackupPage
          onExportBackup={onExportBackup}
          onImportBackup={onImportBackup}
          onSyncBackup={onSyncBackup}
          backupBusy={backupBusy}
          backupStatus={backupStatus}
        />
      </>
    )
  return (
    <>
      <section className="page-title-row">
        <div>
          <p className="eyebrow">{data.eyebrow}</p>
          <h1>{data.title}</h1>
          <p className="page-description">{data.description}</p>
        </div>
        {data.action && (
          <button
            className="primary-btn"
            onClick={() =>
              onAction(
                active === 'Receitas' && aiAvailable
                  ? { kind: 'ai', title: data.action, endpoint: '/ai/prescription' }
                  : active === 'Receitas'
                    ? { kind: 'form', title: 'Adicionar receita', endpoint: '/prescriptions' }
                    : active === 'Sinais vitais'
                      ? { kind: 'form', title: data.action, endpoint: '/vital-signs', vitalType: '' }
                      : {
                          kind: 'form',
                          title: data.action,
                          endpoint:
                            getResourceForLabel(active)?.endpoint || `/${active.toLowerCase().replaceAll(' ', '-')}`,
                        },
              )
            }
          >
            <Icon name={active === 'Receitas' && aiAvailable ? 'spark' : 'plus'} size={16} /> {data.action}
          </button>
        )}
      </section>
      {active === 'Saúde feminina' ? (
        <FemaleHealthPage
          profile={profile}
          onPregnancyChange={onPregnancyChange}
          onAction={onAction}
          selectedDate={selectedDate}
        />
      ) : active === 'Sinais vitais' ? (
        <>
          <CalendarCard
            selectedDate={selectedDate}
            onDateSelect={onDateSelect}
            records={calendarRecords.filter((record) => record.__calendarResource === 'vitalSigns')}
            resource="vitalSigns"
            title="Dias com medições"
            loadError={calendarLoadError}
          />
          <VitalsPage onAction={onAction} profile={profile} selectedDate={selectedDate} />
        </>
      ) : (
        <ResourcePage
          active={active}
          icon={data.icon}
          endpoint={getResourceForLabel(active)?.endpoint}
          resource={getResourceForLabel(active)?.resource || ''}
          selectedDate={selectedDate}
          onDateSelect={onDateSelect}
          calendarRecords={calendarRecords}
          calendarLoadError={calendarLoadError}
          onAction={onAction}
          profileId={profile?.id}
          profile={profile}
          usePushAlarms={active === 'Alarmes' && usePushAlarms}
        />
      )}
    </>
  )
}

export function MedicationPage({ onAction }) {
  return (
    <div className="module-stack">
      <div className="summary-strip">
        <div>
          <span>Tratamentos ativos</span>
          <strong>0 medicamentos</strong>
        </div>
        <div>
          <span>Próximo lembrete</span>
          <strong>Nenhum agendado</strong>
        </div>
        <div>
          <span>Estoque geral</span>
          <strong className="good-text">Sem dados</strong>
        </div>
      </div>
      <div className="module-grid">
        <div className="panel wide-panel">
          <div className="panel-header">
            <div>
              <h3>Meus medicamentos</h3>
              <p>Seus medicamentos aparecerão aqui após o primeiro cadastro.</p>
            </div>
          </div>
          <EmptyState
            icon="pill"
            title="Nenhum medicamento cadastrado"
            message="Adicione seus medicamentos para controlar doses, horários e estoque."
            action="Adicionar medicamento"
            onAction={() => onAction({ kind: 'form', title: 'Adicionar medicamento', endpoint: '/medications' })}
          />
        </div>
        <div className="panel stock-panel">
          <div className="panel-header">
            <div>
              <h3>Estoque</h3>
              <p>Acompanhamento de reposição</p>
            </div>
          </div>
          <EmptyState
            icon="pill"
            title="Estoque vazio"
            message="O estoque será calculado quando você cadastrar um medicamento."
          />
        </div>
      </div>
    </div>
  )
}
