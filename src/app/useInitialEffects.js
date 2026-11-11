import { useEffect } from 'react'
import { apiRequest, flushOfflineQueue, getOfflineQueueSize, isDemoMode, isGuestMode, isNetworkError, resourceApi } from '../services/api'
import { secureStorage } from '../services/secureStorage'
import { moduleNavigation, supportNavigation } from '../services/resources'
import { calendarResources } from './ui/modulosCalendario'

export function useInitialEffects(context) {
  const { authView, setAuthView, setGlobalActionError, active, setActive, selectedDate, darkMode, onboardingComplete, setOnboardingComplete, onboardingStep, onboardingForm, setConnection, setPendingCount, setOfflineStorageError, profiles, setProfiles, activeProfileId, setActiveProfileId, setAccountLoaded, setCalendarRecords, setCalendarLoadError, notificationsEnabled, weeklySummaryEnabled, accessibility } = context
  useEffect(() => {
    const reportStorageError = (event) => {
      const message = event.detail?.message || 'Não foi possível salvar os dados locais protegidos.'
      setGlobalActionError(message)
      setOfflineStorageError(message)
    }
    window.addEventListener('katui:secure-storage-error', reportStorageError)
    return () => window.removeEventListener('katui:secure-storage-error', reportStorageError)
  }, [])

  useEffect(() => {
    if (authView !== 'app' || !activeProfileId) {
      setCalendarRecords([])
      setCalendarLoadError('')
      return undefined
    }
    let mounted = true
    const loadCalendarRecords = async () => {
      const results = await Promise.allSettled(calendarResources.map(async ({ resource, endpoint, label }) => {
        const records = await resourceApi.list(endpoint)
        if (!Array.isArray(records)) throw new Error(`${label}: a API retornou uma lista inválida.`)
        return records.map((record) => ({ ...record, __calendarResource: resource }))
      }))
      if (!mounted) return
      const loaded = results.flatMap((result) => result.status === 'fulfilled' ? result.value : [])
      const failedResources = results.flatMap((result, index) => result.status === 'rejected' ? [calendarResources[index].label] : [])
      setCalendarRecords(loaded)
      setCalendarLoadError(failedResources.length ? `Não foi possível carregar os indicadores de: ${failedResources.join(', ')}.` : '')
    }
    loadCalendarRecords()
    const refresh = (event) => {
      const changedEndpoint = event.detail?.endpoint || ''
      if (calendarResources.some(({ endpoint }) => changedEndpoint === endpoint || changedEndpoint.startsWith(`${endpoint}/`))) loadCalendarRecords()
    }
    window.addEventListener('katui:resource-change', refresh)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', refresh)
    }
  }, [activeProfileId, authView])

  useEffect(() => {
    const url = new URL(window.location.href)
    const requestedSection = url.searchParams.get('section')
    if (!requestedSection) return
    const availableSections = [...moduleNavigation, ...supportNavigation].map((item) => item.label)
    if (availableSections.includes(requestedSection)) setActive(requestedSection)
    url.searchParams.delete('section')
    window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`)
  }, [])

  useEffect(() => {
    if (authView !== 'app') return undefined
    if (isGuestMode()) {
      setConnection('guest')
      setPendingCount(0)
      setOfflineStorageError('')
      return undefined
    }
    if (isDemoMode()) {
      setConnection('demo')
      setPendingCount(0)
      setOfflineStorageError('')
      return undefined
    }
    let mounted = true
    const checkConnection = async () => {
      try {
        await apiRequest('/health')
        if (mounted) setConnection('online')
        setOfflineStorageError('')
        try {
          await flushOfflineQueue()
        } catch (error) {
          if (isNetworkError(error) && mounted) setConnection('offline')
          if (!isNetworkError(error) && mounted) setOfflineStorageError(error.message || 'A fila offline não pôde ser sincronizada.')
        }
      } catch (error) {
        if (mounted) setConnection(isNetworkError(error) || error.code === 'API_URL_NOT_CONFIGURED' ? 'offline' : 'online')
        if (mounted && error.status === 401) {
          localStorage.removeItem('katui.auth-token')
          setAuthView('landing')
        }
      }
      try {
        if (mounted) setPendingCount(await getOfflineQueueSize())
      } catch (error) {
        if (mounted) setOfflineStorageError(error.message || 'O armazenamento offline seguro não está disponível.')
      }
    }
    checkConnection()
    const interval = setInterval(checkConnection, 30000)
    const retryWhenOnline = () => checkConnection()
    const updateQueueCount = (event) => setPendingCount(event.detail?.count ?? 0)
    const updateQueueError = (event) => setOfflineStorageError(event.detail?.message || 'Uma ação offline precisa de atenção.')
    window.addEventListener('online', retryWhenOnline)
    window.addEventListener('katui:queue-change', updateQueueCount)
    window.addEventListener('katui:queue-error', updateQueueError)
    getOfflineQueueSize().then((count) => { if (mounted) setPendingCount(count) }).catch((error) => { if (mounted) setOfflineStorageError(error.message || 'O armazenamento offline seguro não está disponível.') })
    return () => { mounted = false; clearInterval(interval); window.removeEventListener('online', retryWhenOnline); window.removeEventListener('katui:queue-change', updateQueueCount); window.removeEventListener('katui:queue-error', updateQueueError) }
  }, [authView])

  useEffect(() => {
    if (authView !== 'app' || isGuestMode()) return undefined
    let mounted = true
    apiRequest('/me').then((account) => {
      if (!mounted) return
      setAccountLoaded(true)
      const accountProfiles = Array.isArray(account?.profiles) ? account.profiles : account?.profile ? [account.profile] : null
      if (!accountProfiles) return
      const validProfiles = accountProfiles.filter((profile) => profile?.id)
      const localPendingProfiles = profiles.filter((profile) => String(profile.id).startsWith('local-') && !validProfiles.some((item) => item.id === profile.id))
      const mergedProfiles = [...validProfiles, ...localPendingProfiles]
      setProfiles(mergedProfiles)
      secureStorage.setItem('katui.profiles', JSON.stringify(mergedProfiles))
      const preferredId = account.activeProfileId && mergedProfiles.some((profile) => profile.id === account.activeProfileId)
        ? account.activeProfileId
        : mergedProfiles.some((profile) => profile.id === activeProfileId) ? activeProfileId : mergedProfiles[0]?.id || ''
      setActiveProfileId(preferredId)
      localStorage.setItem('katui.active-profile-id', preferredId)
      setOnboardingComplete(mergedProfiles.length > 0)
      localStorage.setItem('katui.onboarding-complete', String(mergedProfiles.length > 0))
    }).catch((error) => {
      if (mounted && error.status === 401) {
        localStorage.removeItem('katui.auth-token')
        setAuthView('landing')
      } else if (mounted && !isNetworkError(error)) {
        setOfflineStorageError(error.message || 'Não foi possível carregar seus perfis do servidor.')
      }
    })
    const updateProfileId = (event) => {
      const { localId, profile } = event.detail || {}
      if (!profile?.id) return
      if (localStorage.getItem('katui.guest-migration-profile-id') === localId) localStorage.setItem('katui.guest-migration-profile-id', profile.id)
      setProfiles((currentProfiles) => {
        const updated = currentProfiles.map((item) => item.id === localId ? { ...item, ...profile } : item)
        secureStorage.setItem('katui.profiles', JSON.stringify(updated))
        return updated
      })
      setActiveProfileId((currentId) => {
        if (currentId !== localId) return currentId
        localStorage.setItem('katui.active-profile-id', profile.id)
        return profile.id
      })
    }
    window.addEventListener('katui:profile-id-change', updateProfileId)
    return () => {
      mounted = false
      window.removeEventListener('katui:profile-id-change', updateProfileId)
    }
  }, [authView])

  useEffect(() => {
    localStorage.setItem('katui.dark-mode', String(darkMode))
  }, [darkMode])

  useEffect(() => {
    localStorage.setItem('katui.notifications', String(notificationsEnabled))
    localStorage.setItem('katui.weekly-summary', String(weeklySummaryEnabled))
    localStorage.setItem('katui.accessibility', JSON.stringify(accessibility))
  }, [notificationsEnabled, weeklySummaryEnabled, accessibility])

  useEffect(() => {
    localStorage.setItem('katui.selected-date', selectedDate)
  }, [selectedDate])

  useEffect(() => {
    if (!onboardingComplete) {
      localStorage.setItem('katui.onboarding-step', String(onboardingStep))
      secureStorage.setItem('katui.onboarding-draft', JSON.stringify(onboardingForm))
    }
  }, [onboardingComplete, onboardingStep, onboardingForm])


}
