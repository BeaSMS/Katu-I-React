import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { createDemoData, demoCredentials } from '../../services/api/demoData'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'
import { Icon, Brand, EmptyState, vitalNumericValue, vitalValueLabel, recordDetailEntries, getAttachmentHref, localDateKey, readGuestProfile, parseDateKey, recordDate, recordOccursOnDate, hasCalendarRecordOnDate, calendarWeekdays, calendarMonths } from '../ui/interfaceCompartilhada'

export function useSessionActions(context) {
  const { setAuthView, setAuthError, setGlobalActionError, active, setActive, setProfileOpen, setOnboardingComplete, setOnboardingStep, setOnboardingForm, setModal, setConnection, setUndoInfo, setFeedbackMessage, undoStackRef, profiles, setProfiles, setActiveProfileId, setAccountLoaded, setGuestMigrationPending } = context
  const openAction = (action) => { setGlobalActionError(''); setModal(action) }
  const startGuestMode = async () => {
    if (!secureStorage.getItem('katui.guest-data')) secureStorage.setItem('katui.guest-data', JSON.stringify(emptyGuestData()))
    const savedGuestProfile = readGuestProfile()
    const hasProfile = Boolean(savedGuestProfile?.name)
    secureStorage.setItem('katui.profiles', JSON.stringify(hasProfile ? [savedGuestProfile] : []))
    secureStorage.setItem('katui.profile-draft', JSON.stringify(savedGuestProfile || {}))
    try {
      await flushSecureStorage()
    } catch (error) {
      const message = error.message || 'Não foi possível preparar o armazenamento local protegido.'
      setGlobalActionError(message)
      setAuthError(message)
      return
    }
    localStorage.setItem('katui.guest-mode', 'true')
    localStorage.setItem('katui.onboarding-complete', String(hasProfile))
    localStorage.removeItem('katui.demo-mode')
    localStorage.removeItem('katui.auth-token')
    localStorage.removeItem('katui.auth-email')
    localStorage.setItem('katui.active-profile-id', 'guest-local-profile')
    setProfiles(hasProfile ? [savedGuestProfile] : [])
    setActiveProfileId('guest-local-profile')
    setOnboardingForm(savedGuestProfile ? { ...defaultOnboardingForm, name: savedGuestProfile.name || '', age: savedGuestProfile.age ? String(savedGuestProfile.age) : '', sex: savedGuestProfile.sex || 'nao-informado', weight: savedGuestProfile.weight ? String(savedGuestProfile.weight) : '', height: savedGuestProfile.heightCm ? String(savedGuestProfile.heightCm) : '', allergies: savedGuestProfile.allergies || '', conditions: savedGuestProfile.specialConditions || '', aiEnabled: false } : defaultOnboardingForm)
    setOnboardingStep(0)
    setOnboardingComplete(hasProfile)
    setActive('Visão geral')
    setModal(null)
    setAuthView('app')
    setConnection('guest')
  }
  const handleAuth = async (form, mode) => {
    if (mode === 'register' && form.password !== form.passwordConfirmation) throw new Error('As senhas precisam ser iguais.')
    if (mode === 'login' && form.email.trim().toLowerCase() === demoCredentials.email) {
      if (form.password !== demoCredentials.password) throw new Error('A senha da demonstração está incorreta.')
      const demoData = createDemoData()
      secureStorage.setItem('katui.demo-data', JSON.stringify(demoData))
      secureStorage.setItem('katui.profiles', JSON.stringify(demoData.profiles))
      try {
        await flushSecureStorage()
      } catch (error) {
        const message = error.message || 'Não foi possível preparar os dados da demonstração.'
        setGlobalActionError(message)
        throw new Error(message)
      }
      localStorage.removeItem('katui.guest-mode')
      localStorage.removeItem('katui.auth-token')
      localStorage.removeItem('katui.guest-migration-consent')
      localStorage.removeItem('katui.guest-migration-completed')
      localStorage.removeItem('katui.guest-migration-profile-id')
      localStorage.setItem('katui.demo-mode', 'true')
      localStorage.setItem('katui.auth-email', demoCredentials.email)
      localStorage.setItem('katui.active-profile-id', demoData.activeProfileId)
      localStorage.setItem('katui.onboarding-complete', 'true')
      localStorage.removeItem('katui.onboarding-step')
      setProfiles(demoData.profiles)
      setActiveProfileId(demoData.activeProfileId)
      setAccountLoaded(false)
      setGuestMigrationPending(false)
      setOnboardingComplete(true)
      setOnboardingStep(0)
      setOnboardingForm(defaultOnboardingForm)
      setActive('Visão geral')
      setConnection('demo')
      setAuthView('app')
      return
    }
    const wasGuest = isGuestMode()
    if (wasGuest) localStorage.removeItem('katui.guest-mode')
    let result
    try {
      result = await apiRequest(mode === 'register' ? '/auth/register' : '/auth/login', { method: 'POST', body: JSON.stringify(mode === 'register' ? { name: form.name.trim(), email: form.email.trim(), password: form.password } : { email: form.email.trim(), password: form.password }) })
    } catch (error) {
      if (wasGuest) localStorage.setItem('katui.guest-mode', 'true')
      throw error
    }
    const token = result?.token || result?.accessToken
    if (!token) {
      if (wasGuest) localStorage.setItem('katui.guest-mode', 'true')
      throw new Error('A resposta de autenticação não trouxe um token válido.')
    }
    const accountEmail = form.email.trim().toLowerCase()
    const previousEmail = localStorage.getItem('katui.auth-email')
    if (previousEmail !== accountEmail) {
      ;['katui.profiles', 'katui.onboarding-draft', 'katui.profile-draft'].forEach((key) => secureStorage.removeItem(key))
      ;['katui.active-profile-id', 'katui.onboarding-complete', 'katui.onboarding-step'].forEach((key) => localStorage.removeItem(key))
      setProfiles([])
      setActiveProfileId('')
      setOnboardingComplete(false)
      setOnboardingStep(0)
      setOnboardingForm(defaultOnboardingForm)
    }
    localStorage.setItem('katui.auth-token', token)
    localStorage.setItem('katui.auth-email', accountEmail)
    setAccountLoaded(false)
    if (wasGuest) {
      setGuestMigrationPending(Boolean(form.migrateGuestData))
      if (form.migrateGuestData) localStorage.setItem('katui.guest-migration-consent', 'true')
      else localStorage.removeItem('katui.guest-migration-consent')
      localStorage.removeItem('katui.guest-migration-completed')
      localStorage.removeItem('katui.guest-migration-profile-id')
    }
    setAuthView('app')
    if (wasGuest && form.migrateGuestData) setGuestMigrationPending(true)
    if (mode === 'register') {
      setOnboardingComplete(false)
      setOnboardingStep(0)
      setOnboardingForm({ ...defaultOnboardingForm, name: form.name.trim() })
      localStorage.removeItem('katui.onboarding-complete')
      secureStorage.setItem('katui.onboarding-draft', JSON.stringify({ ...defaultOnboardingForm, name: form.name.trim() }))
    }
  }
  const logout = () => {
    localStorage.removeItem('katui.auth-token')
    localStorage.removeItem('katui.demo-mode')
    localStorage.removeItem('katui.guest-mode')
    localStorage.removeItem('katui.auth-email')
    setAuthView('landing')
    undoStackRef.current = []
    setUndoInfo(null)
    setFeedbackMessage('')
    setProfileOpen(false)
    setModal(null)
  }
  return { openAction, startGuestMode, handleAuth, logout }
}
