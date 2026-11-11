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
import { medications } from './dadosAplicacao'
import {
  Icon,
  EmptyState,
  vitalNumericValue,
  vitalValueLabel,
  recordDetailEntries,
  localDateKey,
  parseDateKey,
  recordDate,
} from './interfaceCompartilhada'

export function VitalsPage({ onAction, profile, selectedDate }) {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [filterType, setFilterType] = useState('Todos')
  const [chartType, setChartType] = useState('Pressão arterial')
  const [period, setPeriod] = useState('30')
  const [customStart, setCustomStart] = useState('')
  const [customEnd, setCustomEnd] = useState(selectedDate || localDateKey(new Date()))
  const [detailRecord, setDetailRecord] = useState(null)
  const openVital = (vitalType) =>
    onAction({ kind: 'form', title: `Registrar ${vitalType.toLowerCase()}`, endpoint: '/vital-signs', vitalType })
  useEffect(() => {
    let mounted = true
    const loadRecords = async () => {
      setLoading(true)
      setLoadError('')
      try {
        const result = await resourceApi.list('/vital-signs')
        if (!Array.isArray(result)) throw new Error('A API retornou uma lista inválida de sinais vitais.')
        if (mounted) setRecords(result)
      } catch (error) {
        if (mounted) setLoadError(error.message || 'Não foi possível carregar os sinais vitais.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    loadRecords()
    const onResourceChange = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (endpoint === '/vital-signs' || endpoint.startsWith('/vital-signs/')) setRefreshKey((key) => key + 1)
    }
    window.addEventListener('katui:resource-change', onResourceChange)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', onResourceChange)
    }
  }, [profile?.id, refreshKey])

  useEffect(() => {
    if (selectedDate) setCustomEnd(selectedDate)
  }, [selectedDate])

  const endDate = period === 'custom' ? customEnd : selectedDate || localDateKey(new Date())
  const end = parseDateKey(endDate) || new Date()
  const start =
    period === 'custom'
      ? parseDateKey(customStart)
      : new Date(end.getFullYear(), end.getMonth(), end.getDate() - Number(period) + 1)
  const startDate = start ? localDateKey(start) : ''
  const filteredRecords = records
    .filter(
      (record) =>
        (period !== 'custom' || startDate) &&
        (filterType === 'Todos' || record.type === filterType) &&
        recordDate(record) >= startDate &&
        recordDate(record) <= endDate,
    )
    .sort((left, right) =>
      `${recordDate(right)} ${right.time || ''}`.localeCompare(`${recordDate(left)} ${left.time || ''}`),
    )
  const chartRecords = filteredRecords
    .filter((record) => record.type === chartType)
    .slice()
    .reverse()
  const series =
    chartType === 'Pressão arterial'
      ? [
          { key: 'systolic', label: 'Sistólica', color: '#176B62' },
          { key: 'diastolic', label: 'Diastólica', color: '#B36C38' },
        ]
      : [{ key: 'value', label: chartType, color: '#176B62' }]
  const chartPoints = series.flatMap((item) =>
    chartRecords
      .map((record, index) => {
        const valueKey =
          item.key === 'systolic'
            ? record.systolic !== undefined
              ? 'systolic'
              : 'sistolica'
            : item.key === 'diastolic'
              ? record.diastolic !== undefined
                ? 'diastolic'
                : 'diastolica'
              : 'value'
        const value = vitalNumericValue(record, valueKey)
        const timestamp = new Date(`${recordDate(record)}T${record.time || '12:00'}`).getTime()
        return value === null || !Number.isFinite(timestamp) ? null : { ...item, value, timestamp, record, index }
      })
      .filter(Boolean),
  )
  const hasEnoughChartData = series.every(
    (item) => new Set(chartPoints.filter((point) => point.key === item.key).map((point) => point.index)).size >= 2,
  )
  const allValues = chartPoints.map((point) => point.value)
  const minValue = allValues.length ? Math.min(...allValues) : 0
  const maxValue = allValues.length ? Math.max(...allValues) : 1
  const valueSpan = maxValue === minValue ? Math.max(Math.abs(maxValue) * 0.1, 1) : maxValue - minValue
  const minTimestamp = chartPoints.length ? Math.min(...chartPoints.map((point) => point.timestamp)) : 0
  const maxTimestamp = chartPoints.length ? Math.max(...chartPoints.map((point) => point.timestamp)) : 1
  const position = (point) => ({
    x:
      chartPoints.length < 2 || minTimestamp === maxTimestamp
        ? 320
        : 42 + ((point.timestamp - minTimestamp) / (maxTimestamp - minTimestamp)) * 570,
    y: 190 - ((point.value - (minValue - valueSpan * 0.1)) / (valueSpan * 1.2)) * 160,
  })
  const unit =
    chartType === 'Pressão arterial'
      ? 'mmHg'
      : { Glicose: 'mg/dL', Batimentos: 'BPM', Saturação: '%', Temperatura: '°C', Peso: 'kg', Altura: 'cm' }[
          chartType
        ] || ''
  const openEdit = (record) => {
    const id = record.id || record._id
    if (!id) return
    onAction({
      kind: 'form',
      title: 'Editar sinal vital',
      endpoint: `/vital-signs/${encodeURIComponent(id)}`,
      method: 'PATCH',
      data: record,
      vitalType: record.type,
    })
  }
  const removeRecord = (record) => {
    const id = record.id || record._id
    if (!id) return
    onAction({
      kind: 'confirm',
      method: 'DELETE',
      title: 'Remover medição?',
      message: `A medição “${vitalValueLabel(record)}” será removida.`,
      endpoint: `/vital-signs/${encodeURIComponent(id)}`,
      data: record,
    })
  }
  const formatDate = (value) => {
    const date = parseDateKey(value)
    return date ? date.toLocaleDateString('pt-BR') : 'Data não informada'
  }
  return (
    <div className="vitals-page">
      <div className="vitals-toolbar panel">
        <label>
          Indicador da lista
          <select value={filterType} onChange={(event) => setFilterType(event.target.value)}>
            <option>Todos</option>
            {['Pressão arterial', 'Glicose', 'Batimentos', 'Saturação', 'Temperatura', 'Peso', 'Altura'].map((type) => (
              <option key={type}>{type}</option>
            ))}
          </select>
        </label>
        <label>
          Período
          <select value={period} onChange={(event) => setPeriod(event.target.value)}>
            <option value="7">7 dias</option>
            <option value="30">30 dias</option>
            <option value="90">90 dias</option>
            <option value="custom">Personalizado</option>
          </select>
        </label>
        {period === 'custom' && (
          <>
            <label>
              De
              <input
                type="date"
                value={customStart}
                max={customEnd || undefined}
                onChange={(event) => setCustomStart(event.target.value)}
              />
            </label>
            <label>
              Até
              <input
                type="date"
                value={customEnd}
                min={customStart || undefined}
                max={localDateKey(new Date())}
                onChange={(event) => setCustomEnd(event.target.value)}
              />
            </label>
          </>
        )}
        <button className="panel-link" type="button" onClick={() => setRefreshKey((key) => key + 1)} disabled={loading}>
          Atualizar <Icon name="arrow" size={14} />
        </button>
      </div>
      <section className="panel vital-chart-panel">
        <div className="panel-header">
          <div>
            <h3>Evolução das medições</h3>
            <p>Gráfico por indicador, sem faixas ou interpretações clínicas.</p>
          </div>
          <label>
            Indicador
            <select value={chartType} onChange={(event) => setChartType(event.target.value)}>
              {['Pressão arterial', 'Glicose', 'Batimentos', 'Saturação', 'Temperatura', 'Peso', 'Altura'].map(
                (type) => (
                  <option key={type}>{type}</option>
                ),
              )}
            </select>
          </label>
        </div>
        {loading ? (
          <p className="vital-state">Carregando medições…</p>
        ) : loadError ? (
          <p className="vital-state form-error" role="alert">
            {loadError}
          </p>
        ) : hasEnoughChartData ? (
          <>
            <div className="vital-chart-legend">
              {series.map((item) => (
                <span key={item.key}>
                  <i style={{ backgroundColor: item.color }} />
                  {item.label} ({unit})
                </span>
              ))}
            </div>
            <div className="vital-chart-scroll">
              <svg
                className="vital-chart"
                viewBox="0 0 640 220"
                role="img"
                aria-label={`Gráfico de ${chartType} entre ${formatDate(startDate)} e ${formatDate(endDate)}. Valores disponíveis também na tabela abaixo.`}
              >
                {[30, 70, 110, 150, 190].map((y) => (
                  <line key={y} x1="42" x2="612" y1={y} y2={y} className="vital-chart-gridline" />
                ))}
                {[30, 70, 110, 150, 190].map((y, index) => (
                  <text key={`value-${y}`} x="38" y={y + 4} textAnchor="end" className="vital-chart-axis">
                    {(maxValue + valueSpan * 0.1 - index * ((valueSpan * 1.2) / 4)).toFixed(1)}
                  </text>
                ))}
                <text x="6" y="15" className="vital-chart-axis">
                  {unit}
                </text>
                {series.map((item) => {
                  const points = chartPoints.filter((point) => point.key === item.key)
                  const positionedPoints = points.map(position)
                  return (
                    <g key={item.key}>
                      {positionedPoints.length > 1 && (
                        <polyline
                          points={positionedPoints.map((point) => `${point.x},${point.y}`).join(' ')}
                          fill="none"
                          stroke={item.color}
                          strokeWidth="2"
                        />
                      )}
                      {points.map((point, index) => (
                        <circle
                          key={`${point.timestamp}-${index}`}
                          cx={positionedPoints[index].x}
                          cy={positionedPoints[index].y}
                          r="5"
                          fill={item.color}
                        >
                          <title>{`${item.label}: ${point.value} ${unit}, ${formatDate(recordDate(point.record))} ${point.record.time || ''}, ${point.record.notes || 'sem observações'}`}</title>
                        </circle>
                      ))}
                    </g>
                  )
                })}
                <text x="42" y="215" className="vital-chart-axis">
                  {chartPoints.length ? formatDate(recordDate(chartRecords[0])) : ''}
                </text>
                <text x="612" y="215" textAnchor="end" className="vital-chart-axis">
                  {chartPoints.length ? formatDate(recordDate(chartRecords[chartRecords.length - 1])) : ''}
                </text>
              </svg>
            </div>
            <p className="vital-chart-note">
              Pontos representam somente medições salvas. Os valores completos estão disponíveis na lista acessível
              abaixo.
            </p>
          </>
        ) : (
          <EmptyState
            icon="pulse"
            title={chartPoints.length ? 'Mais medições necessárias' : 'Sem medições neste período'}
            message={
              chartPoints.length
                ? `Registre pelo menos duas medições de ${chartType} neste período para mostrar a evolução.`
                : 'Não há dados para desenhar o gráfico. Nenhuma tendência ou valor foi estimado.'
            }
            action="Registrar medição"
            onAction={() => openVital(chartType)}
          />
        )}
      </section>
      <section className="panel vital-history-panel">
        <div className="panel-header">
          <div>
            <h3>Histórico de sinais vitais</h3>
            <p>
              {loading
                ? 'Carregando seus registros…'
                : loadError ||
                  `${filteredRecords.length} medição(ões) · ${formatDate(startDate)} a ${formatDate(endDate)} · ${profile?.name || 'Perfil ativo'}`}
            </p>
          </div>
          <button className="primary-btn" type="button" onClick={() => openVital('')}>
            Registrar medição <Icon name="plus" size={15} />
          </button>
        </div>
        {loadError ? (
          <p className="vital-state form-error" role="alert">
            {loadError}
          </p>
        ) : loading ? (
          <p className="vital-state">Carregando medições…</p>
        ) : filteredRecords.length ? (
          <div className="vital-history-list">
            {filteredRecords.map((record, index) => {
              const id = record.id || record._id || `${recordDate(record)}-${index}`
              return (
                <article className="vital-history-row" key={id}>
                  <div className="vital-history-copy">
                    <strong>
                      {record.type || 'Sinal vital'} · {vitalValueLabel(record)}
                    </strong>
                    <span>
                      {formatDate(recordDate(record))}
                      {record.time ? ` · ${record.time}` : ''}
                      {record.notes ? ` · ${record.notes}` : ''}
                    </span>
                    <small>Perfil: {profile?.name || 'Perfil ativo'}</small>
                  </div>
                  {record.id || record._id ? (
                    <div className="vital-history-actions">
                      <button
                        type="button"
                        className="row-action"
                        aria-label={`Ver detalhes da medição ${record.type || ''} de ${formatDate(recordDate(record))}`}
                        onClick={() => setDetailRecord(record)}
                      >
                        Detalhes
                      </button>
                      <button type="button" className="row-action" onClick={() => openEdit(record)}>
                        Editar
                      </button>
                      <button type="button" className="row-action" onClick={() => removeRecord(record)}>
                        Remover
                      </button>
                    </div>
                  ) : null}
                </article>
              )
            })}
          </div>
        ) : (
          <EmptyState
            icon="pulse"
            title="Nenhuma medição neste período"
            message="Escolha outro período ou registre a primeira medição."
            action="Registrar medição"
            onAction={() => openVital('')}
          />
        )}
      </section>
      <div className="quick-vital-actions">
        {['Pressão arterial', 'Temperatura', 'Glicose', 'Peso'].map((type) => (
          <button key={type} className="quick-item" type="button" onClick={() => openVital(type)}>
            <span>
              <strong>{type}</strong>
              <small>Adicionar medição</small>
            </span>
            <Icon name="chevron" size={16} />
          </button>
        ))}
      </div>
      {detailRecord && (
        <div
          className="modal-backdrop"
          onMouseDown={(event) => event.target === event.currentTarget && setDetailRecord(null)}
        >
          <section
            className="modal-card record-detail-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="vital-detail-title"
          >
            <button
              type="button"
              className="modal-close"
              aria-label="Fechar detalhes"
              onClick={() => setDetailRecord(null)}
            >
              ×
            </button>
            <h2 id="vital-detail-title">{detailRecord.type || 'Sinal vital'}</h2>
            <p className="modal-subtitle">
              {vitalValueLabel(detailRecord)} · {formatDate(recordDate(detailRecord))} {detailRecord.time || ''}
            </p>
            {detailRecord.notes && <p>{detailRecord.notes}</p>}
            <dl>
              {recordDetailEntries(detailRecord).map(([key, value]) => (
                <div key={key}>
                  <dt>{key.replaceAll('_', ' ')}</dt>
                  <dd>{String(value)}</dd>
                </div>
              ))}
            </dl>
            <div className="modal-actions">
              <button type="button" className="cancel-btn" onClick={() => setDetailRecord(null)}>
                Fechar
              </button>
              <button
                type="button"
                className="primary-btn"
                onClick={() => {
                  setDetailRecord(null)
                  openEdit(detailRecord)
                }}
              >
                Editar
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

export function QuickAddMenu({ onAction, femaleProfile, isGuest, aiAvailable }) {
  const [submenu, setSubmenu] = useState(null)
  const close = () => setSubmenu(null)
  const action = (payload) => {
    close()
    onAction(payload)
  }
  return (
    <div className="quick-add-menu" role="menu">
      <button
        className="quick-add-item"
        onClick={() => action({ kind: 'form', title: 'Registrar sintoma', endpoint: '/symptoms' })}
      >
        <span className="quick-icon purple">✦</span>
        <strong>Sintoma</strong>
        <Icon name="chevron" size={15} />
      </button>
      <button
        className="quick-add-item"
        onClick={() => action({ kind: 'form', title: 'Adicionar medicamento', endpoint: '/medications' })}
      >
        <span className="quick-icon teal">＋</span>
        <strong>Remédio</strong>
        <Icon name="chevron" size={15} />
      </button>
      <div className="quick-add-group">
        <button
          className="quick-add-item"
          aria-expanded={submenu === 'vitals'}
          onClick={() => setSubmenu(submenu === 'vitals' ? null : 'vitals')}
        >
          <span className="quick-icon rose">♥</span>
          <strong>Sinais vitais</strong>
          <Icon name="chevron" size={15} />
        </button>
        {submenu === 'vitals' && (
          <div className="quick-add-submenu">
            {[
              ['Glicose', 'Glicose'],
              ['Pressão arterial', 'Pressão arterial'],
              ['Peso', 'Peso'],
              ['Temperatura', 'Temperatura'],
              ['Batimentos', 'Batimentos'],
              ['Saturação', 'Saturação'],
              ['Altura', 'Altura'],
            ].map(([label, vitalType]) => (
              <button
                key={label}
                onClick={() =>
                  action({
                    kind: 'form',
                    title: `Registrar ${label.toLowerCase()}`,
                    endpoint: '/vital-signs',
                    vitalType,
                  })
                }
              >
                {label}
              </button>
            ))}
          </div>
        )}
      </div>
      <button
        className="quick-add-item"
        onClick={() => action({ kind: 'form', title: 'Agendar consulta', endpoint: '/appointments' })}
      >
        <span className="quick-icon blue">⌁</span>
        <strong>Consulta</strong>
        <Icon name="chevron" size={15} />
      </button>
      <button
        className="quick-add-item"
        onClick={() => action({ kind: 'form', title: 'Adicionar exame', endpoint: '/exams' })}
      >
        <span className="quick-icon pink">□</span>
        <strong>Exame</strong>
        <Icon name="chevron" size={15} />
      </button>
      {femaleProfile && (
        <div className="quick-add-group">
          <button
            className="quick-add-item"
            aria-expanded={submenu === 'female'}
            onClick={() => setSubmenu(submenu === 'female' ? null : 'female')}
          >
            <span className="quick-icon magenta">♡</span>
            <strong>Saúde feminina</strong>
            <Icon name="chevron" size={15} />
          </button>
          {submenu === 'female' && (
            <div className="quick-add-submenu">
              <button onClick={() => action({ kind: 'form', title: 'Início de ciclo', endpoint: '/feminine-health' })}>
                Início de ciclo
              </button>
              <button onClick={() => action({ kind: 'form', title: 'Sintomas e fluxo', endpoint: '/feminine-health' })}>
                Sintomas e fluxo
              </button>
            </div>
          )}
        </div>
      )}
      <button
        className="quick-add-item"
        onClick={() => action({ kind: 'form', title: 'Adicionar vacina', endpoint: '/vaccines' })}
      >
        <span className="quick-icon green">✓</span>
        <strong>Vacina</strong>
        <Icon name="chevron" size={15} />
      </button>
      <button
        className="quick-add-item"
        onClick={() =>
          action(
            !isGuest && aiAvailable
              ? { kind: 'ai', title: 'Ler receita com IA', endpoint: '/ai/prescription' }
              : { kind: 'form', title: 'Adicionar receita', endpoint: '/prescriptions' },
          )
        }
      >
        <span className="quick-icon gold">✦</span>
        <strong>{!isGuest && aiAvailable ? 'Ler receita com IA' : 'Adicionar receita'}</strong>
        <Icon name="chevron" size={15} />
      </button>
    </div>
  )
}
