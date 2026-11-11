import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'

export function useOnboardingActions(context) {
  const { setOnboardingError, active, setOnboardingComplete, onboardingForm, connection, setConnection, profiles, setProfiles, activeProfileId, setActiveProfileId, setAccountLoaded } = context
  const finishOnboarding = async () => {
    setOnboardingError('')
    const profile = { name: onboardingForm.name.trim(), age: onboardingForm.age ? Number(onboardingForm.age) : null, sex: onboardingForm.sex, weight: onboardingForm.weight ? Number(onboardingForm.weight) : null, heightCm: onboardingForm.height ? Number(onboardingForm.height) : null, allergies: onboardingForm.allergies.trim() || null, specialConditions: onboardingForm.conditions.trim() || null, aiEnabled: onboardingForm.aiEnabled, themeColor: 'turquoise', corHex: profileThemeOptions[0].color, preferences: { ...defaultProfilePreferences } }
    if (isGuestMode()) {
      const profileRecord = { id: 'guest-local-profile', relation: 'Perfil neste dispositivo', type: 'principal', ...profile }
      secureStorage.setItem('katui.guest-profile', JSON.stringify(profileRecord))
      secureStorage.setItem('katui.profile-draft', JSON.stringify(profile))
      secureStorage.setItem('katui.profiles', JSON.stringify([profileRecord]))
      secureStorage.removeItem('katui.onboarding-draft')
      try {
        await flushSecureStorage()
      } catch (error) {
        setOnboardingError(error.message || 'Não foi possível salvar o perfil neste dispositivo.')
        return
      }
      localStorage.setItem('katui.onboarding-complete', 'true')
      localStorage.setItem('katui.active-profile-id', profileRecord.id)
      localStorage.removeItem('katui.onboarding-step')
      setProfiles([profileRecord])
      setActiveProfileId(profileRecord.id)
      setOnboardingComplete(true)
      return
    }
    const localProfileId = `local-${Date.now()}-${Math.random().toString(16).slice(2)}`
    let profileId = localProfileId
    try {
      if (!isApiConfigured()) throw new Error('O servidor não está configurado neste build. Defina VITE_API_URL e gere o build novamente.')
      if (connection === 'online') {
        try {
          const result = await apiRequest('/profiles/me', { method: 'POST', idempotencyKey: localProfileId, body: JSON.stringify(profile) })
          const serverProfile = result?.profile || result
          if (!serverProfile?.id) throw new Error('O servidor não retornou o ID do perfil criado.')
          profileId = serverProfile.id
        } catch (error) {
          if (!isNetworkError(error)) throw error
          setConnection('offline')
          await queueOfflineAction({ endpoint: '/profiles/me', method: 'POST', id: localProfileId, localProfileId, profileId: activeProfileId, data: profile })
        }
      } else {
        await queueOfflineAction({ endpoint: '/profiles/me', method: 'POST', id: localProfileId, localProfileId, profileId: activeProfileId, data: profile })
      }
    } catch (error) {
      setOnboardingError(error.message || 'Não foi possível criar o perfil. Tente novamente.')
      return
    }
    secureStorage.setItem('katui.profile-draft', JSON.stringify(profile))
    const profileRecord = { id: profileId, name: profile.name || 'Meu perfil', relation: 'Perfil principal', type: 'principal', themeColor: 'turquoise', corHex: profileThemeOptions[0].color, preferences: { ...defaultProfilePreferences }, ...profile }
    const nextProfiles = [profileRecord, ...profiles.filter((item) => item.type !== 'principal')]
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
    secureStorage.removeItem('katui.onboarding-draft')
    try {
      await flushSecureStorage()
    } catch (error) {
      setOnboardingError(error.message || 'Não foi possível salvar o perfil neste dispositivo.')
      return
    }
    setProfiles(nextProfiles)
    setActiveProfileId(profileRecord.id)
    localStorage.setItem('katui.onboarding-complete', 'true')
    localStorage.setItem('katui.active-profile-id', profileRecord.id)
    localStorage.removeItem('katui.onboarding-step')
    setOnboardingComplete(true)
    if (localStorage.getItem('katui.guest-migration-consent') === 'true') {
      localStorage.setItem('katui.guest-migration-profile-id', profileId)
      setAccountLoaded(true)
    }
  }
  
  const skipOnboarding = () => {
    localStorage.setItem('katui.onboarding-complete', 'true')
    localStorage.removeItem('katui.onboarding-step')
    secureStorage.removeItem('katui.onboarding-draft')
    setOnboardingComplete(true)
  }
  return { finishOnboarding, skipOnboarding }
}
