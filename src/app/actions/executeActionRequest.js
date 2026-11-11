import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { disableBackgroundPush, enableBackgroundPush, isPushConfigured, listBackgroundAlarms, removeMedicationReminders, removeSingleReminder, scheduleMedicationReminders, scheduleRestoredAlarm, scheduleSingleReminder } from '../../services/push'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'

export async function executeActionRequest(action, context) {
  const { selectedDate, connection, setConnection, activeProfileId, pushEnabled } = context
  const guestMode = isGuestMode()
  if (guestMode && (action.kind === 'ai' || action.sensitive)) throw new Error('Este recurso exige uma conta e não está disponível no modo Guest.')
  if (action.kind === 'ai' && !isDemoMode() && !isAiPrescriptionEnabled()) throw new Error('A leitura de receitas por IA ainda não está configurada neste site.')
  if (!isApiConfigured() && !guestMode && !isDemoMode()) throw new Error('O servidor não está configurado neste build. Defina VITE_API_URL e gere o build novamente.')
  const requestId = action.requestId || globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
  const { file: submittedFile, ...jsonData } = action.data || {}
  const selectedFile = submittedFile instanceof Blob ? submittedFile : null
  const attachmentEndpoint = ['/exams', '/documents', '/prescriptions'].some((resource) => action.endpoint === resource || action.endpoint?.startsWith(`${resource}/`))
  if (submittedFile && !selectedFile) throw new Error('O arquivo selecionado não está disponível. Selecione-o novamente.')
  if (selectedFile && action.kind !== 'ai' && !attachmentEndpoint) throw new Error('Este formulário não aceita anexos.')
  if (selectedFile && selectedFile.size > 15 * 1024 * 1024) throw new Error('O arquivo deve ter no máximo 15 MB.')
  const payload = { ...jsonData, profileId: activeProfileId, ...(jsonData.unknownDate ? { date: '' } : { date: jsonData.date || selectedDate }) }
  const localProfileId = action.endpoint === '/profiles' ? `local-${Date.now()}-${Math.random().toString(16).slice(2)}` : null
  const demoMode = isDemoMode()
  const localMode = demoMode || guestMode
  const method = action.method || 'POST'
  const backgroundScheduleRequired = !localMode && (
    (pushEnabled && isPushConfigured() && (
      (action.endpoint === '/medications' && method === 'POST' && payload.generateAlarms) ||
      (action.endpoint === '/alarms' && method === 'POST') ||
      (action.kind === 'prescription-confirm' && payload.medications?.some((medication) => medication.generateAlarms))
    )) ||
    (action.data?.__pushAlarm && method === 'DELETE')
  )
  const medicationScheduleCleanupRequired = !localMode && isPushConfigured() && method === 'DELETE' && /^\/medications\/[^/]+$/.test(action.endpoint)
  if (backgroundScheduleRequired && !isPushConfigured()) {
    throw new Error('Ative primeiro o serviço de alarmes em segundo plano. O site não consegue disparar alarmes fechados sem Web Push configurado.')
  }
  if (backgroundScheduleRequired && !pushEnabled) {
    throw new Error('Ative Alarmes em segundo plano em Configurações antes de salvar este lembrete.')
  }
  if (backgroundScheduleRequired && connection !== 'online') {
    throw new Error('Conecte-se à internet para registrar ou remover um alarme em segundo plano.')
  }
  if (medicationScheduleCleanupRequired && connection !== 'online') {
    throw new Error('Conecte-se à internet para excluir o medicamento e cancelar seus alarmes em segundo plano.')
  }
  if ((action.sensitive || action.kind === 'ai') && connection !== 'online' && !localMode) {
    throw new Error(action.sensitive
      ? 'Esta configuração exige conexão com o servidor.'
      : 'A leitura de receita com IA exige conexão. O arquivo não será armazenado na fila offline.')
  }
  let response
  let savedOffline = connection !== 'online' && !localMode
  if (!savedOffline) {
    try {
      if (action.kind === 'ai' && (selectedFile || jsonData.prescriptionId)) {
        const body = new FormData()
        if (selectedFile) body.append('file', selectedFile)
        if (jsonData.prescriptionId) body.append('prescriptionId', jsonData.prescriptionId)
        body.append('profileId', activeProfileId)
        if (jsonData.date || selectedDate) body.append('date', jsonData.date || selectedDate)
        if (jsonData.notes) body.append('notes', jsonData.notes)
        response = await apiRequest(action.endpoint, { method: action.method || 'POST', idempotencyKey: requestId, body })
      } else if (selectedFile && attachmentEndpoint) {
        const body = new FormData()
        body.append('file', selectedFile)
        Object.entries(payload).forEach(([key, value]) => {
          if (value === undefined || value === null) return
          body.append(key, typeof value === 'object' ? JSON.stringify(value) : String(value))
        })
        response = await apiRequest(action.endpoint, { method, idempotencyKey: requestId, body })
      } else {
        if (action.data?.__pushAlarm && method === 'DELETE' && !localMode) {
          const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
          await removeSingleReminder(decodeURIComponent(action.endpoint.split('/').pop()), auth)
          try {
            response = await apiRequest(action.endpoint, { method, idempotencyKey: requestId })
          } catch (error) {
            try {
              await scheduleRestoredAlarm(action.data, auth)
            } catch (restoreError) {
              throw new Error(`O alarme foi desativado, mas o registro não foi removido nem reativado: ${error.message} Restauração falhou: ${restoreError.message}`)
            }
            throw error
          }
        } else if (medicationScheduleCleanupRequired) {
          const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
          const medicationId = decodeURIComponent(action.endpoint.split('/').pop())
          await removeMedicationReminders(medicationId, auth)
          try {
            response = await apiRequest(action.endpoint, { method, idempotencyKey: requestId })
          } catch (error) {
            if (action.data?.generateAlarms) {
              try {
                await scheduleMedicationReminders(action.data, auth)
              } catch (restoreError) {
                throw new Error(`O medicamento não foi removido e seu alarme não pôde ser restaurado: ${error.message} ${restoreError.message}`)
              }
            }
            if (isNetworkError(error)) {
              throw new Error('Não foi possível confirmar a exclusão do medicamento. O alarme foi restaurado; tente novamente quando a conexão estiver estável.')
            }
            throw error
          }
        } else {
          response = await apiRequest(action.endpoint, {
            method,
            idempotencyKey: requestId,
            ...(method === 'DELETE' ? {} : { body: JSON.stringify(payload) }),
          })
        }
      }
    } catch (error) {
      if (!isNetworkError(error) || action.sensitive || action.kind === 'ai') throw error
      savedOffline = true
      setConnection('offline')
    }
  }
  
  return { guestMode, requestId, jsonData, payload, localProfileId, demoMode, localMode, method, response, savedOffline, selectedFile }
}
