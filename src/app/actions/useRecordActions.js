import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { disableBackgroundPush, enableBackgroundPush, isPushConfigured, listBackgroundAlarms, removeMedicationReminders, removeSingleReminder, scheduleMedicationReminders, scheduleRestoredAlarm, scheduleSingleReminder } from '../../services/push'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'
import { executeActionRequest } from './executeActionRequest'

export function useRecordActions(context) {
  const { selectedDate, connection, setConnection, profiles, activeProfileId, pushEnabled, guestMode, setGlobalActionError, active, setModal, setUndoInfo, setFeedbackMessage, undoStackRef, setOfflineStorageError, setProfiles, setActiveProfileId, registerLocalDependent, playFeedbackTone, getUndoDescriptor } = context
  const performAction = async (action) => {
    const request = await executeActionRequest(action, context)
    const { guestMode, requestId, jsonData, payload, localProfileId, demoMode, localMode, method, response, savedOffline, selectedFile } = request
    if (savedOffline) {
      await queueOfflineAction({
        endpoint: action.endpoint,
        method: action.method || 'POST',
        kind: action.kind,
        id: requestId,
        localProfileId,
        profileId: activeProfileId,
        data: { ...payload, ...(selectedFile ? { file: selectedFile } : {}) },
      })
      if (localProfileId) registerLocalDependent(action.data, localProfileId)
      undoStackRef.current = []
      setUndoInfo(null)
      setOfflineStorageError('')
      setFeedbackMessage('Registro salvo localmente. Ações na fila offline ainda não podem ser desfeitas.')
      playFeedbackTone('success')
      setModal({ kind: 'success', title: 'Salvo localmente', message: 'O registro foi guardado com criptografia neste dispositivo e será sincronizado quando a conexão voltar.' })
      return
    }
    if (action.kind === 'ai') {
      if (!response?.id || !Array.isArray(response.medications)) {
        throw new Error('A resposta da análise não contém uma sugestão válida para revisão.')
      }
      setModal({ kind: 'ai-review', title: 'Revise a sugestão', result: { ...response, ...(jsonData.prescriptionId ? { prescriptionId: jsonData.prescriptionId } : {}) } })
      return
    }
    if (action.endpoint === '/profiles' || action.endpoint === '/profiles/me') {
      const serverProfile = response?.profile || response
      if (!serverProfile?.id) throw new Error('O servidor não retornou o ID do perfil criado.')
      if (action.endpoint === '/profiles') registerLocalDependent(action.data, serverProfile.id)
      if (action.endpoint === '/profiles/me') {
        const profile = { ...serverProfile, type: 'principal', relation: 'Perfil principal' }
        const nextProfiles = [profile, ...profiles.filter((item) => item.type !== 'principal')]
        setProfiles(nextProfiles)
        setActiveProfileId(profile.id)
        secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
        localStorage.setItem('katui.active-profile-id', profile.id)
      }
    }
    const editedProfileMatch = method === 'PATCH' ? action.endpoint.match(/^\/profiles\/([^/]+)$/) : null
    if (editedProfileMatch) {
      const profileId = decodeURIComponent(editedProfileMatch[1])
      const updatedProfile = response?.profile || response || {}
      const nextProfiles = profiles.map((profile) => profile.id === profileId ? { ...profile, ...updatedProfile, ...action.data } : profile)
      setProfiles(nextProfiles)
      secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
    }
    let alarmSetupWarning = ''
    const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
    if (!localMode && method === 'POST' && action.endpoint === '/medications' && payload.generateAlarms) {
      try {
        await scheduleMedicationReminders({ ...response, ...payload, id: response?.id || response?.medication?.id || response?.data?.id }, auth)
      } catch (error) {
        alarmSetupWarning = `Medicamento salvo, mas os alarmes em segundo plano não foram configurados: ${error.message}`
      }
    } else if (!localMode && action.kind === 'prescription-confirm' && payload.medications?.some((medication) => medication.generateAlarms)) {
      const requestedMedications = payload.medications.filter((medication) => medication.generateAlarms)
      const savedMedications = Array.isArray(response?.medications) ? response.medications : Array.isArray(response?.createdMedications) ? response.createdMedications : []
      const results = await Promise.allSettled(requestedMedications.map(async (requested) => {
        const saved = savedMedications.find((medication) => String(medication.clientId) === requested.clientId)
        if (!saved) throw new Error(`A API não retornou o registro criado para ${requested.name || 'um medicamento'}.`)
        await scheduleMedicationReminders({ ...requested, ...saved, id: saved?.id || saved?._id }, auth)
      }))
      const failed = results.filter((result) => result.status === 'rejected')
      if (failed.length) {
        alarmSetupWarning = `Receita confirmada, mas ${failed.length} alarme(s) não foram configurados. Verifique se a API retornou os IDs dos medicamentos e confira os dados da posologia.`
      }
    } else if (!localMode && method === 'POST' && action.endpoint === '/alarms') {
      try {
        const alarmId = response?.id || response?.alarm?.id || response?.data?.id
        await scheduleSingleReminder(payload, alarmId, auth)
      } catch (error) {
        alarmSetupWarning = `Alarme salvo, mas não foi ativado para funcionar com o site fechado: ${error.message}`
      }
    }
    const undoDescriptor = getUndoDescriptor(action, response)
    if (undoDescriptor) {
      const entry = { ...undoDescriptor, action: { ...undoDescriptor.action, profileId: undoDescriptor.action.profileId || activeProfileId } }
      undoStackRef.current = [...undoStackRef.current, entry]
      setUndoInfo(entry)
    }
    setFeedbackMessage(undoDescriptor?.label || 'Alteração salva.')
    playFeedbackTone('success')
    window.dispatchEvent(new CustomEvent('katui:resource-change', { detail: { endpoint: action.endpoint } }))
    const successMessage = alarmSetupWarning || (method === 'DELETE'
      ? guestMode ? 'Registro removido somente deste dispositivo.' : demoMode ? 'Registro fictício removido deste navegador.' : 'O registro foi removido do servidor.'
      : guestMode ? 'Registro salvo somente neste dispositivo. Crie uma conta para ter backup e sincronização.' : demoMode ? 'Dados fictícios atualizados somente neste navegador. Nenhum servidor real foi acessado.' : 'O servidor recebeu sua solicitação e os dados já estão sincronizados.')
    if (alarmSetupWarning) setGlobalActionError(alarmSetupWarning)
    const savedPrescriptionId = action.endpoint === '/prescriptions'
      ? response?.id || response?._id || response?.prescription?.id || response?.prescription?._id || response?.data?.id || response?.data?._id
      : null
    const canOfferPrescriptionReview = savedPrescriptionId && selectedFile && !guestMode && (demoMode || isAiPrescriptionEnabled())
    setModal({
      kind: 'success',
      title: action.title === 'Receita confirmada' ? 'Receita confirmada' : 'Tudo certo!',
      message: successMessage,
      ...(canOfferPrescriptionReview ? {
        followUp: {
          kind: 'ai',
          title: 'Ler receita com IA',
          endpoint: '/ai/prescription',
          data: { prescriptionId: savedPrescriptionId, date: payload.date || '', notes: payload.notes || '' },
        },
      } : {}),
    })
  }

  const submitAction = async (action) => {
    setGlobalActionError('')
    try {
      await performAction(action)
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível concluir esta ação.')
      setFeedbackMessage('A ação não foi concluída.')
      playFeedbackTone('error')
      throw error
    }
  }
  return { performAction, submitAction }
}
