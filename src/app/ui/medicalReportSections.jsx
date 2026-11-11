import { getMedicalReportDate, getMedicalReportDetails, getMedicalReportTitle } from '../../services/medicalReport'
import {
  getMedicalReportMedicationSummary,
  groupMedicalReportSymptoms,
  groupMedicalReportVitals,
} from '../../services/medicalReportSummaries'
import { getAttachmentHref } from './interfaceCompartilhada'

function displayDate(value) {
  if (!value) return 'Data não informada'
  const date = new Date(`${value.slice(0, 10)}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
}

export function getExamRecordKey(record, index) {
  return String(record.id || record._id || `${record.name || record.title || 'exame'}-${index}`)
}

export function ReportEntries({ entries }) {
  return entries.length ? (
    <dl className="medical-report-details">
      {entries.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  ) : null
}

function MedicationRows({ entries }) {
  return entries.map(({ record, startDate, endDate }) => (
    <article className="medical-report-record" key={record.id || record._id || record.name}>
      <div className="medical-report-record-heading">
        <strong>{getMedicalReportTitle('medications', record)}</strong>
        <time>
          {startDate ? displayDate(startDate) : 'Início não informado'}
          {endDate ? ` – ${displayDate(endDate)}` : ''}
        </time>
      </div>
      <ReportEntries entries={getMedicalReportDetails('medications', record)} />
    </article>
  ))
}

export function MedicationReportSection({ records }) {
  const summary = getMedicalReportMedicationSummary(records)
  return (
    <section className="medical-report-section">
      <h3>Resumo de medicamentos</h3>
      <p className="medical-report-note">
        Registros com vigência nos últimos 30 dias ({displayDate(summary.firstDay)} a hoje):
      </p>
      {summary.recent.length ? (
        <MedicationRows entries={summary.recent} />
      ) : (
        <p className="medical-report-empty">Nenhum registro de tratamento nesse período.</p>
      )}
      <h4>Uso contínuo ativo hoje</h4>
      {summary.continuous.length ? (
        <MedicationRows entries={summary.continuous} />
      ) : (
        <p className="medical-report-empty">Nenhum medicamento contínuo ativo registrado.</p>
      )}
      <p className="medical-report-note">
        O aplicativo não registra a tomada de cada dose de medicamentos comuns. Esta lista mostra tratamentos
        cadastrados e não confirma que as doses foram ingeridas.
      </p>
    </section>
  )
}

export function SymptomsReportSection({ records }) {
  const groups = groupMedicalReportSymptoms(records)
  return (
    <section className="medical-report-section">
      <h3>Sintomas registrados</h3>
      <p className="medical-report-note">Sintomas mais frequentes primeiro, com os dias em que foram registrados.</p>
      {groups.length ? (
        groups.map((group) => (
          <article className="medical-report-record" key={group.symptom}>
            <div className="medical-report-record-heading">
              <strong>{group.symptom}</strong>
              <span>{group.count} registro(s)</span>
            </div>
            <ul className="medical-report-occurrences">
              {group.occurrences.map((occurrence, index) => (
                <li key={`${occurrence.date}-${occurrence.time}-${index}`}>
                  {displayDate(occurrence.date)}
                  {occurrence.time ? ` · ${occurrence.time}` : ''}
                  {occurrence.intensity ? ` · Intensidade registrada: ${occurrence.intensity}` : ''}
                </li>
              ))}
            </ul>
          </article>
        ))
      ) : (
        <p className="medical-report-empty">Nenhum sintoma registrado no período selecionado.</p>
      )}
    </section>
  )
}

function numericValue(value) {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(String(value).replace(',', '.'))
  return Number.isFinite(parsed) ? parsed : null
}

function chartSeries(type, records) {
  if (type === 'Pressão arterial') {
    return [
      { key: 'systolic', label: 'Sistólica', color: '#176B62', value: (record) => record.systolic ?? record.sistolica },
      {
        key: 'diastolic',
        label: 'Diastólica',
        color: '#B36C38',
        value: (record) => record.diastolic ?? record.diastolica,
      },
    ].map((series) => ({
      ...series,
      unit: 'mmHg',
      points: records
        .map((record, index) => ({ record, index, value: numericValue(series.value(record)) }))
        .filter((point) => point.value !== null),
    }))
  }
  const units = { Glicose: 'mg/dL', Batimentos: 'BPM', Saturação: '%', Temperatura: '°C', Peso: 'kg', Altura: 'cm' }
  return [
    {
      key: type,
      label: type,
      color: '#176B62',
      unit: units[type] || records.find((record) => record.unit)?.unit || '',
      points: records
        .map((record, index) => ({ record, index, value: numericValue(record.value) }))
        .filter((point) => point.value !== null),
    },
  ]
}

function MedicalVitalChart({ type, records }) {
  const series = chartSeries(type, records)
  const points = series.flatMap((item) => item.points)
  if (!points.length) return <p className="medical-report-empty">Sem valores numéricos para desenhar o gráfico.</p>
  const values = points.map((point) => point.value)
  const minimum = Math.min(...values)
  const maximum = Math.max(...values)
  const span = maximum === minimum ? Math.max(Math.abs(maximum) * 0.1, 1) : maximum - minimum
  const pointPosition = (point) => ({
    x: records.length < 2 ? 320 : 42 + (point.index / (records.length - 1)) * 570,
    y: 180 - ((point.value - (minimum - span * 0.1)) / (span * 1.2)) * 145,
  })
  return (
    <div className="medical-report-chart-wrap">
      <div className="medical-report-chart-legend">
        {series.map((item) => (
          <span key={item.key}>
            <i style={{ backgroundColor: item.color }} />
            {item.label}
            {item.unit ? ` (${item.unit})` : ''}
          </span>
        ))}
      </div>
      <svg
        className="medical-report-chart"
        viewBox="0 0 640 220"
        role="img"
        aria-label={`Gráfico de ${type} com os registros selecionados`}
      >
        {[35, 75, 115, 155, 195].map((y) => (
          <line key={y} x1="42" x2="612" y1={y} y2={y} className="vital-chart-gridline" />
        ))}
        {series.map((item) => {
          const positions = item.points.map(pointPosition)
          return (
            <g key={item.key}>
              {positions.length > 1 && (
                <polyline
                  points={positions.map((point) => `${point.x},${point.y}`).join(' ')}
                  fill="none"
                  stroke={item.color}
                  strokeWidth="2"
                />
              )}
              {item.points.map((point, index) => (
                <circle
                  key={`${point.index}-${index}`}
                  cx={positions[index].x}
                  cy={positions[index].y}
                  r="4"
                  fill={item.color}
                >
                  <title>{`${item.label}: ${point.value} · ${displayDate(getMedicalReportDate(point.record))}`}</title>
                </circle>
              ))}
            </g>
          )
        })}
        <text x="42" y="215" className="vital-chart-axis">
          {displayDate(getMedicalReportDate(records[0]))}
        </text>
        <text x="612" y="215" textAnchor="end" className="vital-chart-axis">
          {displayDate(getMedicalReportDate(records[records.length - 1]))}
        </text>
      </svg>
      <p className="medical-report-note">
        O gráfico representa somente os valores registrados, sem estimar tendências.
      </p>
    </div>
  )
}

export function VitalSignsReportSection({ records }) {
  const groups = groupMedicalReportVitals(records)
  return (
    <section className="medical-report-section">
      <h3>Sinais vitais por tipo</h3>
      {groups.length ? (
        groups.map((group) => (
          <section className="medical-report-vital-group" key={group.type}>
            <h4>{group.type}</h4>
            <MedicalVitalChart type={group.type} records={group.records} />
            {group.records.map((record, index) => (
              <article className="medical-report-record" key={record.id || record._id || `${group.type}-${index}`}>
                <div className="medical-report-record-heading">
                  <strong>{getMedicalReportTitle('vitalSigns', record)}</strong>
                  <time>
                    {displayDate(getMedicalReportDate(record))}
                    {record.time ? ` · ${record.time}` : ''}
                  </time>
                </div>
                <ReportEntries entries={getMedicalReportDetails('vitalSigns', record)} />
              </article>
            ))}
          </section>
        ))
      ) : (
        <p className="medical-report-empty">Nenhum sinal vital registrado no período selecionado.</p>
      )}
    </section>
  )
}

export function ExamExportPanel({ records, selectedIds, onToggle, onExport, exporting }) {
  return (
    <section className="medical-report-exam-export panel" aria-label="Selecionar exames para PDF conjunto">
      <h3>Exames para PDF conjunto</h3>
      <p>Selecione os anexos em PDF, JPG ou PNG para reuni-los em um único arquivo.</p>
      {records.length ? (
        <div className="medical-report-exam-list">
          {records.map((record, index) => {
            const key = getExamRecordKey(record, index)
            const hasFile = Boolean(getAttachmentHref(record))
            return (
              <label className="medical-report-option" key={key}>
                <input
                  type="checkbox"
                  checked={selectedIds.includes(key)}
                  disabled={!hasFile}
                  onChange={() => onToggle(key)}
                />
                <span>
                  {record.name || record.title || 'Exame'} · {displayDate(getMedicalReportDate(record))}
                  {hasFile ? '' : ' · sem arquivo anexado'}
                </span>
              </label>
            )
          })}
        </div>
      ) : (
        <p className="medical-report-empty">Nenhum exame encontrado.</p>
      )}
      <button type="button" className="outline-btn" disabled={exporting || selectedIds.length === 0} onClick={onExport}>
        {exporting ? 'Preparando PDF…' : 'Baixar exames selecionados em PDF'}
      </button>
    </section>
  )
}
