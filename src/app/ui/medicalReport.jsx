import { useState } from 'react'
import { fetchAttachmentBlob, resourceApi } from '../../services/api'
import { createCombinedExamPdf } from '../../services/examPdf'
import {
  filterMedicalReportRecords,
  getMedicalReportDate,
  getMedicalReportDetails,
  getMedicalReportProfileDetails,
  getMedicalReportTitle,
  medicalReportSections,
} from '../../services/medicalReport'
import { getAttachmentHref } from './interfaceCompartilhada'
import {
  ExamExportPanel,
  getExamRecordKey,
  MedicationReportSection,
  ReportEntries,
  SymptomsReportSection,
  VitalSignsReportSection,
} from './medicalReportSections'

const initialSections = ['profile', 'medications', 'vitalSigns']

function displayDate(value) {
  if (!value) return ''
  const date = new Date(`${value.slice(0, 10)}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
}

export function MedicalReportBuilder({ profile }) {
  const [selectedSections, setSelectedSections] = useState(initialSections)
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [reportData, setReportData] = useState(null)
  const [generatedAt, setGeneratedAt] = useState('')
  const [selectedExamIds, setSelectedExamIds] = useState([])
  const [loading, setLoading] = useState(false)
  const [exportingExams, setExportingExams] = useState(false)
  const [error, setError] = useState('')

  const toggleSection = (sectionId) => {
    setSelectedSections((current) =>
      current.includes(sectionId) ? current.filter((value) => value !== sectionId) : [...current, sectionId],
    )
    setReportData(null)
  }

  const generateReport = async () => {
    setError('')
    setReportData(null)
    try {
      filterMedicalReportRecords([], dateFrom, dateTo)
      setLoading(true)
      const collections = medicalReportSections.filter(
        (section) => selectedSections.includes(section.id) && section.endpoints.length,
      )
      const results = await Promise.all(
        collections.map(async (section) => {
          const lists = await Promise.all(section.endpoints.map((endpoint) => resourceApi.list(endpoint)))
          if (lists.some((records) => !Array.isArray(records))) {
            throw new Error(`A lista de ${section.label.toLowerCase()} retornada pela API é inválida.`)
          }
          return [section.id, lists.flat()]
        }),
      )
      const generatedData = Object.fromEntries(results)
      setReportData(generatedData)
      const examRecords = filterMedicalReportRecords(
        Array.isArray(generatedData.exams) ? generatedData.exams : [],
        dateFrom,
        dateTo,
      )
      setSelectedExamIds(
        selectedSections.includes('exams')
          ? examRecords
              .map((record, index) => ({ record, index }))
              .filter(({ record }) => getAttachmentHref(record))
              .map(({ record, index }) => getExamRecordKey(record, index))
          : [],
      )
      setGeneratedAt(new Date().toISOString())
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível preparar o relatório.')
    } finally {
      setLoading(false)
    }
  }

  const toggleExam = (examId) => {
    setSelectedExamIds((current) =>
      current.includes(examId) ? current.filter((value) => value !== examId) : [...current, examId],
    )
  }

  const exportSelectedExams = async () => {
    setError('')
    setExportingExams(true)
    try {
      const selectedExams = (reportData?.exams || []).filter((record, index) =>
        selectedExamIds.includes(getExamRecordKey(record, index)),
      )
      const bytes = await createCombinedExamPdf(selectedExams, (exam) => fetchAttachmentBlob(getAttachmentHref(exam)))
      const url = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }))
      const link = document.createElement('a')
      link.href = url
      link.download = `exames-selecionados-${new Date().toISOString().slice(0, 10)}.pdf`
      link.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (exportError) {
      setError(exportError.message || 'Não foi possível gerar o PDF conjunto dos exames.')
    } finally {
      setExportingExams(false)
    }
  }

  const formatPeriod = () => {
    if (!dateFrom && !dateTo) return 'Todo o histórico disponível'
    return `${dateFrom ? displayDate(dateFrom) : 'Início'} a ${dateTo ? displayDate(dateTo) : 'Hoje'}`
  }
  const examRecords = reportData ? filterMedicalReportRecords(reportData.exams || [], dateFrom, dateTo) : []

  return (
    <section className="medical-report-builder panel" aria-labelledby="medical-report-title">
      <div className="medical-report-controls">
        <div className="panel-header">
          <div>
            <h2 id="medical-report-title">Relatório para consulta médica</h2>
            <p>Monte um resumo dos registros para imprimir ou salvar como PDF e compartilhar com seu médico.</p>
          </div>
        </div>
        <fieldset className="medical-report-options">
          <legend>Informações incluídas</legend>
          <div className="medical-report-options-grid">
            {medicalReportSections.map((section) => (
              <label key={section.id} className="medical-report-option">
                <input
                  type="checkbox"
                  checked={selectedSections.includes(section.id)}
                  onChange={() => toggleSection(section.id)}
                />
                <span>{section.label}</span>
              </label>
            ))}
          </div>
        </fieldset>
        <div className="medical-report-period">
          <label>
            Data inicial
            <input
              type="date"
              value={dateFrom}
              onChange={(event) => {
                setDateFrom(event.target.value)
                setReportData(null)
              }}
            />
          </label>
          <label>
            Data final
            <input
              type="date"
              value={dateTo}
              onChange={(event) => {
                setDateTo(event.target.value)
                setReportData(null)
              }}
            />
          </label>
          <small>Deixe as datas em branco para incluir todo o histórico disponível.</small>
        </div>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button
          type="button"
          className="primary-btn medical-report-generate"
          disabled={loading || selectedSections.length === 0}
          onClick={generateReport}
        >
          {loading ? 'Preparando relatório…' : 'Preparar relatório'}
        </button>
      </div>
      {reportData && selectedSections.includes('exams') && (
        <ExamExportPanel
          records={examRecords}
          selectedIds={selectedExamIds}
          onToggle={toggleExam}
          onExport={exportSelectedExams}
          exporting={exportingExams}
        />
      )}
      {reportData && (
        <article className="medical-report-preview" aria-label="Prévia do relatório médico">
          <header>
            <p className="eyebrow">RESUMO PARA COMPARTILHAR COM O PROFISSIONAL DE SAÚDE</p>
            <h2>Relatório de saúde</h2>
            <p>
              {profile?.name || 'Perfil de saúde'} · Período: {formatPeriod()}
            </p>
            <small>Gerado em {displayDate(generatedAt)}</small>
          </header>
          {selectedSections.includes('profile') && (
            <section className="medical-report-section">
              <h3>Perfil</h3>
              <ReportEntries entries={getMedicalReportProfileDetails(profile)} />
            </section>
          )}
          {medicalReportSections
            .filter((section) => section.endpoints.length && selectedSections.includes(section.id))
            .map((section) => {
              const records = [...filterMedicalReportRecords(reportData[section.id] || [], dateFrom, dateTo)].sort(
                (first, second) => getMedicalReportDate(second).localeCompare(getMedicalReportDate(first)),
              )
              if (section.id === 'medications') {
                return <MedicationReportSection key={section.id} records={reportData.medications || []} />
              }
              if (section.id === 'vitalSigns') {
                return <VitalSignsReportSection key={section.id} records={records} />
              }
              if (section.id === 'symptoms') {
                return <SymptomsReportSection key={section.id} records={records} />
              }
              return (
                <section key={section.id} className="medical-report-section">
                  <h3>{section.label}</h3>
                  {records.length ? (
                    records.map((record, index) => (
                      <article
                        className="medical-report-record"
                        key={record.id || record._id || `${section.id}-${index}`}
                      >
                        <div className="medical-report-record-heading">
                          <strong>{getMedicalReportTitle(section.id, record)}</strong>
                          <time>{displayDate(getMedicalReportDate(record)) || 'Data não informada'}</time>
                        </div>
                        <ReportEntries entries={getMedicalReportDetails(section.id, record)} />
                      </article>
                    ))
                  ) : (
                    <p className="medical-report-empty">Nenhum registro no período selecionado.</p>
                  )}
                </section>
              )
            })}
          <footer>
            Informações organizadas a partir dos registros selecionados. Este relatório não faz diagnóstico nem
            substitui avaliação profissional.
          </footer>
          <div className="medical-report-actions">
            <button type="button" className="outline-btn" onClick={() => window.print()}>
              Imprimir ou salvar em PDF
            </button>
            <span>Na impressão, escolha “Salvar como PDF” para compartilhar o arquivo.</span>
          </div>
        </article>
      )}
    </section>
  )
}
