import { useEffect, useState } from 'react'
import { resourceApi } from '../../services/api'
import { dateKeyForFemaleHealth, getCycleDayKind, getCycleInsights } from '../../services/femaleHealth'
import { getContraceptiveDay, getContraceptivePatternDescription } from '../../services/contraceptive'
import { useFemaleHealthCalendar } from '../useFemaleHealthCalendar'
import { Icon } from './interfaceCompartilhada'
import { createPregnancyActions } from '../createPregnancyActions'

export function FemaleHealthPage({ profile, onPregnancyChange, onAction, selectedDate }) {
  const storageKey = `katui.pregnancy.${profile?.id || 'current'}`
  const [pregnancy, setPregnancy] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(storageKey)) || { enabled: false, startDate: '' }
    } catch {
      return { enabled: false, startDate: '' }
    }
  })
  const [startDate, setStartDate] = useState(pregnancy.startDate || '')
  const [pregnancyConfirming, setPregnancyConfirming] = useState(false)
  const [pregnancyError, setPregnancyError] = useState('')
  const [records, setRecords] = useState([])
  const [medicationRecords, setMedicationRecords] = useState([])
  const [calendarActionDate, setCalendarActionDate] = useState('')
  const [femaleSelectedDate, setFemaleSelectedDate] = useState(selectedDate || dateKeyForFemaleHealth(new Date()))
  const [pregnancyMenuOpen, setPregnancyMenuOpen] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [refreshKey, setRefreshKey] = useState(0)
  const [calendarMonth, setCalendarMonth] = useState(() => {
    const selected = selectedDate ? new Date(`${selectedDate}T00:00:00`) : new Date()
    return new Date(selected.getFullYear(), selected.getMonth(), 1)
  })

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(storageKey)) || { enabled: false, startDate: '' }
      setPregnancy(saved)
      setStartDate(saved.startDate || '')
      setPregnancyConfirming(false)
      setPregnancyError('')
    } catch {
      setPregnancy({ enabled: false, startDate: '' })
      setStartDate('')
    }
  }, [storageKey])

  useEffect(() => {
    if (selectedDate) setFemaleSelectedDate(selectedDate)
  }, [selectedDate])

  useEffect(() => {
    let mounted = true
    const loadRecords = async () => {
      setLoading(true)
      setLoadError('')
      try {
        const [healthRecords, medications] = await Promise.all([
          resourceApi.list('/feminine-health'),
          resourceApi.list('/medications'),
        ])
        if (!Array.isArray(healthRecords) || !Array.isArray(medications))
          throw new Error('A API retornou uma lista inválida para Saúde feminina.')
        if (mounted) {
          setRecords(healthRecords)
          setMedicationRecords(medications)
        }
      } catch (error) {
        if (mounted) setLoadError(error.message || 'Não foi possível carregar os dados de Saúde feminina.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    loadRecords()
    const onResourceChange = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (
        ['/feminine-health', '/medications'].some(
          (resource) => endpoint === resource || endpoint.startsWith(`${resource}/`),
        )
      )
        setRefreshKey((current) => current + 1)
    }
    window.addEventListener('katui:resource-change', onResourceChange)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', onResourceChange)
    }
  }, [profile?.id, refreshKey])

  const cycleInsights = getCycleInsights(records)
  const cycles = cycleInsights.cycles
  const openCycle = cycles.find((cycle) => !cycle.endDate)
  const dailyLogs = records
    .filter(
      (record) =>
        record.recordType === 'daily-symptom' ||
        (!record.recordType && !record.startDate && !record.recordType?.includes('contraceptive')),
    )
    .sort((first, second) => String(second.date || '').localeCompare(String(first.date || '')))
  const contraceptiveLogs = records.filter((record) => record.recordType === 'contraceptive-dose')
  const contraceptive = medicationRecords.find((medication) =>
    String(medication.purpose || medication.category || '')
      .toLowerCase()
      .includes('anticoncepcional'),
  )
  const today = dateKeyForFemaleHealth(new Date())
  const todayDose = contraceptiveLogs.find(
    (record) =>
      record.date === today &&
      (!contraceptive || String(record.medicationId) === String(contraceptive.id || contraceptive._id)),
  )
  const contraceptiveDay = getContraceptiveDay(contraceptive?.date, today, contraceptive?.contraceptivePackType)

  const { requestActivation, confirmActivation, disablePregnancy } = createPregnancyActions({
    storageKey,
    startDate,
    setPregnancy,
    setPregnancyConfirming,
    setPregnancyError,
    setStartDate,
    onPregnancyChange,
  })
  const beginCycle = (date = femaleSelectedDate || today) =>
    onAction({
      kind: 'form',
      title: 'Registrar início de ciclo',
      endpoint: '/feminine-health',
      data: { recordType: 'menstrual-cycle', date },
    })
  const finishCycle = (cycle, date = today) =>
    onAction({
      kind: 'form',
      title: 'Registrar fim de ciclo',
      endpoint: `/feminine-health/${encodeURIComponent(cycle.id || cycle._id)}`,
      method: 'PATCH',
      data: { ...cycle, recordType: 'menstrual-cycle', endDate: date },
    })
  const openDailyLog = (date) => {
    const existing = dailyLogs.find((record) => record.date === date)
    onAction({
      kind: 'form',
      title: 'Registrar sintomas e fluxo',
      endpoint: existing ? `/feminine-health/${encodeURIComponent(existing.id || existing._id)}` : '/feminine-health',
      method: existing ? 'PATCH' : 'POST',
      data: { ...existing, recordType: 'daily-symptom', date },
    })
  }
  const logContraceptiveDose = (taken) =>
    onAction({
      kind: 'form',
      title: 'Registrar tomada de anticoncepcional',
      endpoint: '/feminine-health',
      data: {
        recordType: 'contraceptive-dose',
        medicationId: contraceptive?.id || contraceptive?._id || '',
        name: contraceptive?.name || '',
        date: today,
        time: new Date().toTimeString().slice(0, 5),
        taken,
      },
    })
  const configureContraceptive = () => {
    const startTime = new Date(Date.now() + 10 * 60_000)
    const time = `${String(startTime.getHours()).padStart(2, '0')}:${String(startTime.getMinutes()).padStart(2, '0')}`
    onAction({
      kind: 'form',
      title: 'Configurar anticoncepcional',
      endpoint: '/medications',
      data: {
        name: '',
        date: today,
        time,
        purpose: 'Anticoncepcional',
        frequencyType: 'Vezes ao dia',
        frequencyValue: '1',
        continuous: true,
        generateAlarms: true,
        contraceptivePackType: '21',
      },
    })
  }
  const openCalendarDay = (date) => {
    setFemaleSelectedDate(date)
    setCalendarActionDate(date)
  }
  const selectCalendarAction = (callback) => {
    setCalendarActionDate('')
    callback()
  }
  const { calendarDays, changeCalendarMonth, formatDate, formatPrediction } = useFemaleHealthCalendar(
    calendarMonth,
    setCalendarMonth,
    cycleInsights,
  )
  return (
    <>
      <div className="female-health-layout">
        {loadError && (
          <p className="form-error female-health-error" role="alert">
            {loadError}
          </p>
        )}
        <section className="female-health-intro">
          <div>
            <span className="female-health-kicker">SEU CICLO, NO SEU TEMPO</span>
            <h2>Acompanhe seu ciclo</h2>
            <p>Registre datas, sintomas e humor para consultar seu histórico neste perfil.</p>
          </div>
          <button
            className="primary-btn"
            onClick={() => (openCycle ? finishCycle(openCycle, today) : beginCycle())}
            disabled={Boolean(openCycle && today < openCycle.startDate)}
          >
            <Icon name={openCycle ? 'check' : 'plus'} size={16} />
            {openCycle ? 'Terminar ciclo' : 'Registrar início do fluxo'}
          </button>
        </section>

        <section className="female-health-summary-grid">
          <article className="female-health-stat">
            <span className="female-stat-icon">
              <Icon name="calendar" size={19} />
            </span>
            <div>
              <small>Próxima menstruação estimada</small>
              <strong>{formatPrediction(cycleInsights.nextPeriodDate)}</strong>
              <span>
                {cycleInsights.averageCycleLength
                  ? `Média de ${cycleInsights.averageCycleLength} dias entre ciclos`
                  : 'Registre ao menos dois inícios de ciclo'}
              </span>
            </div>
          </article>
          <article className="female-health-stat">
            <span className="female-stat-icon fertile">
              <Icon name="spark" size={19} />
            </span>
            <div>
              <small>Janela fértil estimada</small>
              <strong>
                {cycleInsights.fertileStart
                  ? `${formatDate(dateKeyForFemaleHealth(cycleInsights.fertileStart))} – ${formatDate(dateKeyForFemaleHealth(cycleInsights.fertileEnd))}`
                  : 'Sem estimativa disponível'}
              </strong>
              <span>Estimativa baseada nos ciclos registrados</span>
            </div>
          </article>
          <article className="female-health-stat">
            <span className="female-stat-icon period">
              <Icon name="clock" size={19} />
            </span>
            <div>
              <small>Duração média do fluxo</small>
              <strong>
                {cycleInsights.completedPeriodCount
                  ? `${cycleInsights.averagePeriodLength} dias`
                  : 'Sem dados suficientes'}
              </strong>
              <span>
                {cycleInsights.completedPeriodCount
                  ? 'Média dos períodos encerrados'
                  : 'A média será calculada após encerrar um ciclo'}
              </span>
            </div>
          </article>
        </section>

        <p className="female-health-disclaimer" role="note">
          As previsões são aproximadas, podem variar e não devem ser usadas para evitar ou planejar uma gravidez nem
          substituir orientação profissional.
        </p>

        <div className="female-health-main-grid">
          <section className="panel female-cycle-calendar">
            <div className="female-panel-heading">
              <div>
                <h3>Calendário do ciclo</h3>
                <p>Selecione um dia para registrar sintomas e fluxo.</p>
              </div>
              <div className="female-calendar-controls">
                <button type="button" aria-label="Mês anterior" onClick={() => changeCalendarMonth(-1)}>
                  ‹
                </button>
                <strong>{calendarMonth.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}</strong>
                <button type="button" aria-label="Próximo mês" onClick={() => changeCalendarMonth(1)}>
                  ›
                </button>
              </div>
            </div>
            <div className="female-calendar-grid female-weekday-row">
              {['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map((day, index) => (
                <span key={`${day}-${index}`}>{day}</span>
              ))}
            </div>
            <div className="female-calendar-grid">
              {calendarDays.map((day) => {
                const kind = getCycleDayKind(day, cycleInsights)
                const isCurrentMonth = day.getMonth() === calendarMonth.getMonth()
                const dayKey = dateKeyForFemaleHealth(day)
                const isSelected = dayKey === femaleSelectedDate
                return (
                  <button
                    key={dayKey}
                    type="button"
                    className={`female-calendar-day${isCurrentMonth ? '' : ' outside-month'}${kind ? ` ${kind}` : ''}${isSelected ? ' selected' : ''}`}
                    aria-label={`${day.toLocaleDateString('pt-BR')}${kind ? `, ${kind === 'period' ? 'fluxo registrado' : kind === 'fertile' ? 'janela fértil estimada' : 'menstruação estimada'}` : ''}`}
                    aria-pressed={isSelected}
                    onClick={() => openCalendarDay(dayKey)}
                  >
                    <span>{day.getDate()}</span>
                    {kind && <i />}
                  </button>
                )
              })}
            </div>
            <div className="female-calendar-legend">
              <span>
                <i className="period" /> Fluxo registrado
              </span>
              <span>
                <i className="predicted-period" /> Período estimado
              </span>
              <span>
                <i className="fertile" /> Janela fértil estimada
              </span>
            </div>
          </section>
        </div>

        <section className="panel female-section-panel">
          <div className="female-panel-heading">
            <div>
              <h3>Anticoncepcional e lembretes</h3>
              <p>Use um lembrete para registrar o horário da sua rotina.</p>
            </div>
            <span className="female-cycle-icon">
              <Icon name="pill" size={19} />
            </span>
          </div>
          {contraceptive ? (
            <div className="female-contraceptive-row">
              <div>
                <strong>{contraceptive.name}</strong>
                <span>
                  {getContraceptivePatternDescription(contraceptive, today) ||
                    (contraceptive.time ? `Lembrete diário às ${contraceptive.time}` : 'Lembrete configurado')}
                </span>
                {contraceptive.time && (
                  <small>
                    Tomada diária às {contraceptive.time}
                    {contraceptive.generateAlarms ? ' · alarmes ativados' : ''}
                  </small>
                )}
              </div>
              <div className="female-dose-actions">
                {todayDose ? (
                  <span className={`female-dose-status${todayDose.taken ? ' taken' : ' missed'}`}>
                    {todayDose.taken ? 'Registrado hoje' : 'Marcado como não tomado'}
                  </span>
                ) : contraceptiveDay?.phase === 'pause' ? (
                  <span className="female-dose-status">Pausa da cartela · sem tomada prevista hoje</span>
                ) : (
                  <>
                    <button type="button" className="soft-btn" onClick={() => logContraceptiveDose(true)}>
                      Marcar tomada
                    </button>
                    <button type="button" className="text-btn" onClick={() => logContraceptiveDose(false)}>
                      Não tomei
                    </button>
                  </>
                )}
              </div>
            </div>
          ) : (
            <div className="female-empty-inline">
              <p>Nenhum anticoncepcional cadastrado.</p>
              <button type="button" className="soft-btn" onClick={configureContraceptive}>
                <Icon name="plus" size={14} /> Configurar anticoncepcional
              </button>
            </div>
          )}
        </section>

        <div className="female-history-grid">
          <section className="panel female-section-panel">
            <div className="female-panel-heading">
              <div>
                <h3>Histórico de ciclos</h3>
                <p>Datas registradas neste perfil</p>
              </div>
            </div>
            {loading ? (
              <p className="female-muted">Carregando ciclos…</p>
            ) : cycles.length ? (
              <div className="female-history-list">
                {cycles.slice(0, 8).map((cycle) => (
                  <article className="female-history-item" key={cycle.id || cycle._id || cycle.startDate}>
                    <span className={`female-history-mark${cycle.endDate ? '' : ' active'}`} />
                    <div>
                      <strong>
                        {formatDate(cycle.startDate)}
                        {cycle.endDate ? ` – ${formatDate(cycle.endDate)}` : ' · em andamento'}
                      </strong>
                      <small>
                        {cycle.flow ? `Fluxo: ${cycle.flow}` : 'Registro de menstruação'}
                        {cycle.physicalSymptoms ? ` · ${cycle.physicalSymptoms}` : ''}
                      </small>
                    </div>
                    {!cycle.endDate && (
                      <button type="button" className="text-btn" onClick={() => finishCycle(cycle)}>
                        Encerrar
                      </button>
                    )}
                  </article>
                ))}
              </div>
            ) : (
              <p className="female-muted">
                Ainda não há ciclos registrados. Use “Registrar início do fluxo” para começar.
              </p>
            )}
          </section>
          <section className="panel female-section-panel">
            <div className="female-panel-heading">
              <div>
                <h3>Sintomas e humor</h3>
                <p>Registros diários recentes</p>
              </div>
              <button type="button" className="text-btn" onClick={() => openDailyLog(selectedDate || today)}>
                Registrar <Icon name="plus" size={14} />
              </button>
            </div>
            {loading ? (
              <p className="female-muted">Carregando registros…</p>
            ) : dailyLogs.length ? (
              <div className="female-history-list">
                {dailyLogs.slice(0, 6).map((record) => (
                  <button
                    type="button"
                    className="female-daily-item"
                    key={record.id || record._id || record.date}
                    onClick={() => openDailyLog(record.date)}
                  >
                    <strong>{formatDate(record.date)}</strong>
                    <span>
                      {[record.flow && `Fluxo ${record.flow.toLowerCase()}`, record.physicalSymptoms, record.mood]
                        .filter(Boolean)
                        .join(' · ') || 'Registro do dia'}
                    </span>
                  </button>
                ))}
              </div>
            ) : (
              <p className="female-muted">Toque em um dia do calendário para registrar fluxo, sintomas e humor.</p>
            )}
          </section>
        </div>

        <section className="panel female-pregnancy-panel">
          <button
            type="button"
            className="female-pregnancy-toggle"
            aria-expanded={pregnancyMenuOpen}
            onClick={() => setPregnancyMenuOpen((open) => !open)}
          >
            <span className="female-cycle-icon">
              <Icon name="heart" size={19} />
            </span>
            <span>
              <strong>Acompanhamento de gestação</strong>
              <small>
                {pregnancy.enabled
                  ? `Ativo · início em ${formatDate(pregnancy.startDate)}`
                  : 'Recurso opcional e privado'}
              </small>
            </span>
            <span className="female-pregnancy-chevron" aria-hidden="true">
              {pregnancyMenuOpen ? '−' : '+'}
            </span>
          </button>
          {pregnancyMenuOpen &&
            (pregnancy.enabled ? (
              <div className="female-pregnancy-active">
                <span>Acompanhamento ativo · início informado em {formatDate(pregnancy.startDate)}</span>
                <button type="button" className="text-btn" onClick={disablePregnancy}>
                  Desativar
                </button>
              </div>
            ) : pregnancyConfirming ? (
              <div className="female-pregnancy-confirm">
                <p>Usar {formatDate(startDate)} como data de início do acompanhamento?</p>
                <div>
                  <button type="button" className="cancel-btn" onClick={() => setPregnancyConfirming(false)}>
                    Voltar
                  </button>
                  <button type="button" className="primary-btn" onClick={confirmActivation}>
                    Confirmar
                  </button>
                </div>
              </div>
            ) : (
              <div className="female-pregnancy-form">
                <label>
                  Data de início
                  <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
                </label>
                <button type="button" className="soft-btn" onClick={requestActivation}>
                  Ativar acompanhamento
                </button>
                {pregnancyError && <small className="form-error">{pregnancyError}</small>}
              </div>
            ))}
        </section>
      </div>
      {calendarActionDate &&
        (() => {
          const actionDate = calendarActionDate
          const isFutureDate = actionDate > today
          const canFinishCycle = Boolean(openCycle) && actionDate >= openCycle.startDate && !isFutureDate
          const dateLabel = formatDate(actionDate)
          return (
            <div
              className="modal-backdrop female-day-action-backdrop"
              onMouseDown={(event) => event.target === event.currentTarget && setCalendarActionDate('')}
            >
              <section
                className="modal-card female-day-action-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="female-day-action-title"
              >
                <button
                  type="button"
                  className="modal-close"
                  aria-label="Fechar"
                  onClick={() => setCalendarActionDate('')}
                >
                  ×
                </button>
                <div className="modal-icon">
                  <Icon name="calendar" size={22} />
                </div>
                <p className="eyebrow">AÇÕES DO DIA</p>
                <h2 id="female-day-action-title">{dateLabel}</h2>
                <p className="modal-subtitle">O que você gostaria de registrar?</p>
                <div className="female-day-action-options">
                  <button
                    type="button"
                    className="female-day-action-option"
                    disabled={isFutureDate}
                    onClick={() => selectCalendarAction(() => openDailyLog(actionDate))}
                  >
                    <span className="female-day-action-icon">
                      <Icon name="pulse" size={19} />
                    </span>
                    <span>
                      <strong>Adicionar sintomas e fluxo</strong>
                      <small>Registre sintomas, humor ou intensidade do fluxo</small>
                    </span>
                    <Icon name="chevron" size={16} />
                  </button>
                  {openCycle ? (
                    <button
                      type="button"
                      className="female-day-action-option finish"
                      disabled={!canFinishCycle}
                      onClick={() => selectCalendarAction(() => finishCycle(openCycle, actionDate))}
                    >
                      <span className="female-day-action-icon">
                        <Icon name="check" size={19} />
                      </span>
                      <span>
                        <strong>Terminar ciclo</strong>
                        <small>
                          {actionDate < openCycle.startDate
                            ? `Escolha uma data após ${formatDate(openCycle.startDate)}`
                            : isFutureDate
                              ? 'Não é possível encerrar em uma data futura'
                              : 'Marque este dia como o último dia do fluxo'}
                        </small>
                      </span>
                      <Icon name="chevron" size={16} />
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="female-day-action-option start"
                      disabled={isFutureDate}
                      onClick={() => selectCalendarAction(() => beginCycle(actionDate))}
                    >
                      <span className="female-day-action-icon">
                        <Icon name="plus" size={19} />
                      </span>
                      <span>
                        <strong>Marcar primeiro dia do ciclo</strong>
                        <small>Inicie um novo registro de ciclo neste dia</small>
                      </span>
                      <Icon name="chevron" size={16} />
                    </button>
                  )}
                </div>
                {isFutureDate && (
                  <small className="female-day-action-note">Não é possível registrar dados em uma data futura.</small>
                )}
              </section>
            </div>
          )
        })()}
    </>
  )
}
