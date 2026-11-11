import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { disableBackgroundPush, enableBackgroundPush, isPushConfigured, listBackgroundAlarms, removeMedicationReminders, removeSingleReminder, scheduleMedicationReminders, scheduleRestoredAlarm, scheduleSingleReminder } from '../../services/push'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'

export function useUndoActions(context) {
  const { setGlobalActionError, setUndoInfo, setUndoInProgress, setFeedbackMessage, undoStackRef, undoInProgressRef, activeProfileId, pushEnabled, playFeedbackTone } = context
  const getUndoDescriptor = (action, response) => {
    const recordCollectionPattern = /^\/(medications|alarms|prescriptions|symptoms|vital-signs|appointments|exams|feminine-health|pregnancy|vaccines|documents|reports)$/
    const deleteMatch = action.method === 'DELETE' && action.endpoint.match(/^(\/(?:medications|alarms|prescriptions|symptoms|vital-signs|appointments|exams|feminine-health|pregnancy|vaccines|documents|reports))\/([^/]+)$/)
    if (deleteMatch && action.data && typeof action.data === 'object') {
      const record = { ...action.data }
      delete record.id
      delete record._id
      return {
        label: 'Registro removido.',
        originalEndpoint: `${deleteMatch[1]}/${deleteMatch[2]}`,
        action: { endpoint: deleteMatch[1], method: 'POST', data: record, profileId: action.data.profileId || activeProfileId, ...(action.data.__pushAlarm ? { pushAlarm: true } : {}), ...(deleteMatch[1] === '/medications' && action.data.generateAlarms && isPushConfigured() ? { pushMedication: true } : {}) },
      }
    }
    const method = action.method || 'POST'
    const createdRecordId = response?.id || response?.record?.id || response?.data?.id
    if (method === 'POST' && recordCollectionPattern.test(action.endpoint) && createdRecordId) {
      return {
        label: 'Registro adicionado.',
        action: { endpoint: `${action.endpoint}/${encodeURIComponent(createdRecordId)}`, method: 'DELETE', profileId: activeProfileId, ...(action.endpoint === '/alarms' && pushEnabled ? { pushAlarm: true, data: { ...action.data, ...response, id: createdRecordId, __pushAlarm: true } } : {}), ...(action.endpoint === '/medications' && action.data?.generateAlarms && pushEnabled ? { pushMedication: true, data: { ...action.data, ...response, id: createdRecordId } } : {}) },
      }
    }
    return null
  }
  const undoLastAction = async () => {
    const last = undoStackRef.current[undoStackRef.current.length - 1]
    if (!last || undoInProgressRef.current) return
    undoInProgressRef.current = true
    setUndoInProgress(true)
    try {
      const auth = { token: localStorage.getItem('katui.auth-token'), profileId: last.action.profileId }
      const resourceId = decodeURIComponent(last.action.endpoint.split('/').pop())
      if (last.action.pushAlarm && last.action.method === 'DELETE') await removeSingleReminder(resourceId, auth)
      if (last.action.pushMedication && last.action.method === 'DELETE') await removeMedicationReminders(resourceId, auth)
      let response
      try {
        response = await apiRequest(last.action.endpoint, {
          method: last.action.method,
          profileId: last.action.profileId,
          ...(last.action.method === 'DELETE' ? {} : { body: JSON.stringify(last.action.data) }),
          idempotencyKey: `undo-${globalThis.crypto?.randomUUID?.() || Date.now()}`,
        })
      } catch (error) {
        if (last.action.pushMedication && last.action.method === 'DELETE') {
          try {
            await scheduleMedicationReminders(last.action.data, auth)
          } catch (restoreError) {
            throw new Error(`O registro não foi removido e a restauração do alarme falhou: ${error.message} ${restoreError.message}`)
          }
        }
        if (last.action.pushAlarm && last.action.method === 'DELETE') {
          try {
            await scheduleRestoredAlarm(last.action.data, auth)
          } catch (restoreError) {
            throw new Error(`O registro não foi removido e a restauração do alarme falhou: ${error.message} ${restoreError.message}`)
          }
        }
        throw error
      }
      if (last.action.pushAlarm && last.action.method === 'POST') {
        await scheduleRestoredAlarm({ ...last.action.data, id: response?.id || last.action.data.sourceId }, auth)
      }
      if (last.action.pushMedication && last.action.method === 'POST') {
        const restoredId = response?.id || response?.medication?.id || response?.data?.id
        if (!restoredId) throw new Error('Medicamento restaurado, mas o servidor não retornou um ID para reativar o alarme.')
        await scheduleMedicationReminders({ ...last.action.data, id: restoredId }, auth)
      }
      let remainingStack = undoStackRef.current.slice(0, -1)
      if (last.action.method === 'POST' && last.originalEndpoint) {
        const restoredId = response?.id || response?.record?.id || response?.data?.id
        remainingStack = remainingStack.flatMap((entry) => entry.action.method === 'DELETE' && entry.action.endpoint === last.originalEndpoint
          ? restoredId
            ? [{ ...entry, action: { ...entry.action, endpoint: `${last.action.endpoint}/${encodeURIComponent(restoredId)}` } }]
            : []
          : [entry])
      }
      undoStackRef.current = remainingStack
      window.dispatchEvent(new CustomEvent('katui:resource-change', { detail: { endpoint: last.action.endpoint.split('/').slice(0, 2).join('/') } }))
      const next = undoStackRef.current[undoStackRef.current.length - 1] || null
      setUndoInfo(next)
      setFeedbackMessage(last.label === 'Registro removido.' ? 'Registro restaurado.' : 'Inclusão desfeita.')
      playFeedbackTone('undo')
      setGlobalActionError('')
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível desfazer a ação. Tente novamente.')
      playFeedbackTone('error')
    } finally {
      undoInProgressRef.current = false
      setUndoInProgress(false)
    }
  }
  return { getUndoDescriptor, undoLastAction }
}
