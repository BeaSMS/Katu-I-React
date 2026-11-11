import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { disableBackgroundPush, enableBackgroundPush, isPushConfigured, listBackgroundAlarms, removeMedicationReminders, removeSingleReminder, scheduleMedicationReminders, scheduleRestoredAlarm, scheduleSingleReminder } from '../../services/push'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'

export function useProfileActions(context) {
  const { setGlobalActionError, active, setProfileOpen, darkMode, setDarkMode, connection, setFeedbackMessage, profiles, setProfiles, activeProfileId, setActiveProfileId, pushEnabled, setPushEnabled, setNotificationPermission, notificationsEnabled, setNotificationsEnabled, weeklySummaryEnabled, setWeeklySummaryEnabled, accessibility, setAccessibility, activeProfile, guestMode } = context
  const updateProfilePreferences = async (changes) => {
    if (!activeProfileId) return
    const currentPreferences = { ...defaultProfilePreferences, ...(activeProfile?.preferences || {}), accessibility: { ...defaultAccessibility, ...(activeProfile?.preferences?.accessibility || {}) } }
    const nextPreferences = { ...currentPreferences, ...changes }
    const updatedProfile = { ...activeProfile, preferences: nextPreferences }
    setProfiles((currentProfiles) => {
      const nextProfiles = currentProfiles.map((profile) => profile.id === activeProfileId ? updatedProfile : profile)
      secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
      return nextProfiles
    })
    if (changes.darkMode !== undefined) setDarkMode(changes.darkMode)
    if (changes.notificationsEnabled !== undefined) setNotificationsEnabled(changes.notificationsEnabled)
    if (changes.weeklySummaryEnabled !== undefined) setWeeklySummaryEnabled(changes.weeklySummaryEnabled)
    if (changes.accessibility) setAccessibility(changes.accessibility)
    if (guestMode || isDemoMode()) return
  
    const endpoint = `/profiles/${encodeURIComponent(activeProfileId)}`
    const data = { preferences: nextPreferences }
    const requestId = globalThis.crypto?.randomUUID?.() || `profile-preferences-${activeProfileId}-${Date.now()}-${Math.random().toString(16).slice(2)}`
    const offlineAction = { endpoint, method: 'PATCH', id: requestId, profileId: activeProfileId, data }
    try {
      if (connection !== 'online') {
        await queueOfflineAction(offlineAction)
        return
      }
      await apiRequest(endpoint, { method: 'PATCH', idempotencyKey: offlineAction.id, body: JSON.stringify(data) })
    } catch (error) {
      if (isNetworkError(error)) {
        try {
          await queueOfflineAction(offlineAction)
        } catch (queueError) {
          setGlobalActionError(`As preferências foram salvas neste dispositivo, mas não puderam entrar na fila de sincronização: ${queueError.message}`)
        }
        return
      }
      setGlobalActionError(`As preferências foram salvas neste dispositivo, mas não sincronizadas com o servidor: ${error.message}`)
    }
  }
  const toggleProfilePreference = (key) => updateProfilePreferences({ [key]: !({ darkMode, notificationsEnabled, weeklySummaryEnabled }[key]) })
  const toggleBackgroundPush = async () => {
    const auth = { token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId }
    setGlobalActionError('')
    try {
      if (pushEnabled) {
        await disableBackgroundPush(auth)
        localStorage.removeItem(`katui.push-enabled.${activeProfileId}`)
        setPushEnabled(false)
        setFeedbackMessage('Alarmes em segundo plano desativados neste dispositivo.')
      } else {
        await enableBackgroundPush(auth)
        localStorage.setItem(`katui.push-enabled.${activeProfileId}`, 'true')
        setPushEnabled(true)
        setFeedbackMessage('Notificações autorizadas. Os novos alarmes poderão tocar com o site fechado.')
        try {
          const medications = await resourceApi.list('/medications')
          if (!Array.isArray(medications)) throw new Error('A API retornou uma lista inválida de medicamentos.')
          const results = await Promise.allSettled(medications.filter((medication) => medication.generateAlarms).map((medication) => scheduleMedicationReminders(medication, auth)))
          const failed = results.filter((result) => result.status === 'rejected')
          if (failed.length) setGlobalActionError(`Notificações ativadas, mas ${failed.length} medicamento(s) ainda não têm alarmes. Confira data, frequência e duração e salve novamente.`)
        } catch (error) {
          setGlobalActionError(`Notificações ativadas, mas não foi possível sincronizar os medicamentos existentes: ${error.message}`)
        }
      }
      setNotificationPermission(typeof Notification === 'undefined' ? 'unsupported' : Notification.permission)
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível configurar as notificações.')
    }
  }
  const enableLocalNotifications = async () => {
    if (typeof Notification === 'undefined') {
      setGlobalActionError('Este navegador não oferece notificações.')
      return
    }
    try {
      const permission = Notification.permission === 'default' ? await Notification.requestPermission() : Notification.permission
      setNotificationPermission(permission)
      if (permission !== 'granted') throw new Error('Permita notificações nas configurações do navegador para receber lembretes locais.')
      localStorage.setItem('katui.notifications', 'true')
      setNotificationsEnabled(true)
      updateProfilePreferences({ notificationsEnabled: true })
      setFeedbackMessage('Notificações locais ativadas. Elas exigem que o site permaneça aberto.')
      setGlobalActionError('')
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível ativar as notificações locais.')
    }
  }
  const updateProfileAccessibility = (key, value) => updateProfilePreferences({ accessibility: { ...accessibility, [key]: value === undefined ? !accessibility[key] : value, ...(key === 'textScale' ? { largeText: Number(value) > 100 } : {}) } })
  const selectProfile = (profile) => {
    const preferences = { ...defaultProfilePreferences, ...(profile.preferences || {}), accessibility: { ...defaultAccessibility, ...(profile.preferences?.accessibility || {}) } }
    setDarkMode(Boolean(preferences.darkMode))
    setNotificationsEnabled(preferences.notificationsEnabled !== false)
    setWeeklySummaryEnabled(preferences.weeklySummaryEnabled !== false)
    setPushEnabled(localStorage.getItem(`katui.push-enabled.${profile.id}`) === 'true')
    setAccessibility(preferences.accessibility)
    setActiveProfileId(profile.id)
    localStorage.setItem('katui.active-profile-id', profile.id)
    setProfileOpen(false)
  }
  const registerLocalDependent = (data, id = `local-${Date.now()}`) => {
    if (!data?.name?.trim()) return
    const profile = { id, name: data.name.trim(), relation: data.relation?.trim() || data.detail?.trim() || 'Dependente', type: 'dependente', preferences: { ...defaultProfilePreferences }, ...data }
    const nextProfiles = [...profiles, profile]
    setProfiles(nextProfiles)
    setActiveProfileId(profile.id)
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
    localStorage.setItem('katui.active-profile-id', profile.id)
  }
  return { updateProfilePreferences, toggleProfilePreference, toggleBackgroundPush, enableLocalNotifications, updateProfileAccessibility, selectProfile, registerLocalDependent }
}
