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
import {
  disableBackgroundPush,
  enableBackgroundPush,
  isPushConfigured,
  listBackgroundAlarms,
  removeMedicationReminders,
  removeSingleReminder,
  scheduleMedicationReminders,
  scheduleRestoredAlarm,
  scheduleSingleReminder,
} from '../../services/push'
import { medications } from './dadosAplicacao'

export function PrescriptionReview({ action, onClose, onSubmit, pushEnabled }) {
  const medications = Array.isArray(action.result?.medications) ? action.result.medications : []
  const canConfigureAlarms = pushEnabled || isDemoMode()
  const currentDate = new Date()
  const localToday = `${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}-${String(currentDate.getDate()).padStart(2, '0')}`
  const [selected, setSelected] = useState(() => medications.map((_, index) => index))
  const [plans, setPlans] = useState(() =>
    medications.map(() => ({
      date: localToday,
      time: currentDate.toTimeString().slice(0, 5),
      frequencyType: 'A cada intervalo de horas',
      frequencyValue: '',
      durationDays: '',
      continuous: false,
      generateAlarms: false,
    })),
  )
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const updatePlan = (index, key, value) =>
    setPlans((current) => current.map((plan, planIndex) => (planIndex === index ? { ...plan, [key]: value } : plan)))
  const confirm = async () => {
    setSubmitting(true)
    setError('')
    try {
      const selectedMedications = selected.map((index) => {
        const plan = plans[index]
        if (plan.generateAlarms) {
          const frequency = Number(plan.frequencyValue)
          const duration = Number(plan.durationDays)
          if (
            !plan.date ||
            !plan.time ||
            !Number.isInteger(frequency) ||
            frequency < 1 ||
            frequency > 24 ||
            (!plan.continuous && (!Number.isInteger(duration) || duration < 1 || duration > 3650))
          ) {
            throw new Error(
              `Informe uma frequência, data, horário e duração válidos para ${medications[index].name || 'o medicamento selecionado'}.`,
            )
          }
        }
        return {
          ...medications[index],
          ...plan,
          clientId: String(index),
          ...(plan.generateAlarms ? {} : { generateAlarms: false }),
        }
      })
      const prescriptionId = action.result.prescriptionId || action.result.id
      await onSubmit({
        kind: 'prescription-confirm',
        title: 'Receita confirmada',
        endpoint: `/prescriptions/${encodeURIComponent(prescriptionId)}/confirm`,
        data: { analysisId: action.result.id, medications: selectedMedications },
      })
    } catch (submitError) {
      setError(submitError.message || 'Não foi possível confirmar a receita.')
    } finally {
      setSubmitting(false)
    }
  }
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className="modal-card prescription-review">
        <button className="modal-close" onClick={onClose}>
          ×
        </button>
        <div className="modal-icon">✦</div>
        <h2>Revise a sugestão</h2>
        <p className="modal-subtitle">
          Confira os dados reconhecidos. Nenhum medicamento será salvo até você confirmar.
        </p>
        {action.result?.warnings?.length > 0 && (
          <div className="prescription-warnings">
            <strong>Atenção</strong>
            {action.result.warnings.map((warning, index) => (
              <p key={index}>{typeof warning === 'string' ? warning : warning.message || JSON.stringify(warning)}</p>
            ))}
          </div>
        )}
        <div className="prescription-medications">
          {medications.length ? (
            medications.map((medication, index) => (
              <div className="prescription-medication-card" key={`${medication.name || 'medication'}-${index}`}>
                <label className="prescription-medication">
                  <input
                    type="checkbox"
                    checked={selected.includes(index)}
                    onChange={() =>
                      setSelected((items) =>
                        items.includes(index) ? items.filter((item) => item !== index) : [...items, index],
                      )
                    }
                  />
                  <span>
                    <strong>{medication.name || 'Medicamento sem nome'}</strong>
                    <small>
                      {[medication.dose, medication.schedule].filter(Boolean).join(' · ') ||
                        'Confira os detalhes com a receita original.'}
                    </small>
                  </span>
                </label>
                {selected.includes(index) && canConfigureAlarms && (
                  <div className="prescription-alarm-plan">
                    <label>
                      <input
                        type="checkbox"
                        checked={plans[index].generateAlarms}
                        onChange={(event) => updatePlan(index, 'generateAlarms', event.target.checked)}
                      />
                      Configurar alarmes para este medicamento
                    </label>
                    {plans[index].generateAlarms && (
                      <div className="prescription-alarm-fields">
                        <label>
                          Data inicial
                          <input
                            type="date"
                            value={plans[index].date}
                            onChange={(event) => updatePlan(index, 'date', event.target.value)}
                          />
                        </label>
                        <label>
                          Horário inicial
                          <input
                            type="time"
                            value={plans[index].time}
                            onChange={(event) => updatePlan(index, 'time', event.target.value)}
                          />
                        </label>
                        <label>
                          Frequência
                          <select
                            value={plans[index].frequencyType}
                            onChange={(event) => updatePlan(index, 'frequencyType', event.target.value)}
                          >
                            <option>A cada intervalo de horas</option>
                            <option>Vezes ao dia</option>
                          </select>
                        </label>
                        <label>
                          Quantidade (1 a 24)
                          <input
                            type="number"
                            min="1"
                            max="24"
                            step="1"
                            value={plans[index].frequencyValue}
                            onChange={(event) => updatePlan(index, 'frequencyValue', event.target.value)}
                          />
                        </label>
                        <label className="prescription-continuous">
                          <input
                            type="checkbox"
                            checked={plans[index].continuous}
                            onChange={(event) => updatePlan(index, 'continuous', event.target.checked)}
                          />
                          Uso contínuo
                        </label>
                        {!plans[index].continuous && (
                          <label>
                            Duração (dias)
                            <input
                              type="number"
                              min="1"
                              max="3650"
                              step="1"
                              value={plans[index].durationDays}
                              onChange={(event) => updatePlan(index, 'durationDays', event.target.value)}
                            />
                          </label>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))
          ) : (
            <p className="prescription-empty">
              A análise não sugeriu medicamentos. Você ainda pode confirmar a receita para salvá-la.
            </p>
          )}
          {medications.length > 0 && !canConfigureAlarms && (
            <p className="prescription-empty">
              Para configurar alarmes, ative as notificações em segundo plano nas Configurações.
            </p>
          )}
        </div>
        {error && <small className="form-error">{error}</small>}
        <div className="modal-actions">
          <button className="cancel-btn" disabled={submitting} onClick={onClose}>
            Cancelar
          </button>
          <button
            className="primary-btn"
            disabled={submitting || (medications.length > 0 && selected.length === 0)}
            onClick={confirm}
          >
            {submitting ? 'Confirmando...' : 'Confirmar seleção'}
          </button>
        </div>
      </div>
    </div>
  )
}
