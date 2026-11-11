import { useEffect } from 'react'
import { apiRequest, isDemoMode, isGuestMode, resourceApi } from '../services/api'
import { isPushConfigured, listBackgroundAlarms } from '../services/push'
import { secureStorage } from '../services/secureStorage'
import { defaultAccessibility, defaultProfilePreferences, getAlarmTimestamp, getLocalMedicationAlarm } from './ui/dadosAplicacao'
import { getDueLocalReminders } from '../services/localReminders'

export function useProfileEffects(context) {
  const { authView, setGlobalActionError, active, setActive, mobileMenuOpen, setMobileMenuOpen, mobileMenuButtonRef, mobileNavigationRef, notificationOpen, setNotificationOpen, setNotificationAlarms, setNotificationLoading, setNotificationError, notificationRefreshKey, notificationRef, darkMode, setDarkMode, profiles, activeProfileId, pushEnabled, setPushEnabled, notificationsEnabled, setNotificationsEnabled, notificationPermission, weeklySummaryEnabled, setWeeklySummaryEnabled, accessibility, setAccessibility, guestMode, femaleProfile } = context
  useEffect(() => {
    setPushEnabled(localStorage.getItem(`katui.push-enabled.${activeProfileId}`) === 'true')
  }, [activeProfileId])
  useEffect(() => {
    if (authView !== 'app' || !activeProfileId) {
      setNotificationAlarms([])
      setNotificationError('')
      return undefined
    }
    let mounted = true
    const loadNotifications = async () => {
      setNotificationLoading(true)
      try {
        const localMode = isGuestMode() || isDemoMode()
        const alarmsRequest = !localMode && pushEnabled && isPushConfigured()
          ? listBackgroundAlarms({ token: localStorage.getItem('katui.auth-token'), profileId: activeProfileId })
          : resourceApi.list('/alarms')
        const [alarms, medicationRecords] = await Promise.all([
          alarmsRequest,
          localMode ? resourceApi.list('/medications') : Promise.resolve([]),
        ])
        if (!Array.isArray(alarms) || !Array.isArray(medicationRecords)) throw new Error('A lista de alarmes retornada não é válida.')
        const now = Date.now()
        const candidates = [
          ...alarms.filter((alarm) => !['concluído', 'concluido', 'cancelado', 'cancelada', 'desativado'].includes(String(alarm.status || '').toLowerCase())),
          ...medicationRecords.map((medication) => getLocalMedicationAlarm(medication, now)).filter(Boolean),
        ]
        const upcoming = candidates
          .map((alarm) => ({ ...alarm, __notificationAt: getAlarmTimestamp(alarm) }))
          .filter((alarm) => alarm.__notificationAt !== null && alarm.__notificationAt >= now)
          .sort((first, second) => first.__notificationAt - second.__notificationAt)
        if (mounted) {
          setNotificationAlarms(upcoming)
          setNotificationError('')
        }
      } catch (error) {
        if (mounted) setNotificationError(error.message || 'Não foi possível carregar os alarmes.')
      } finally {
        if (mounted) setNotificationLoading(false)
      }
    }
    loadNotifications()
    const refresh = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (['/alarms', '/medications'].some((path) => endpoint === path || endpoint.startsWith(`${path}/`))) loadNotifications()
    }
    const interval = window.setInterval(loadNotifications, 60_000)
    window.addEventListener('katui:resource-change', refresh)
    return () => {
      mounted = false
      window.clearInterval(interval)
      window.removeEventListener('katui:resource-change', refresh)
    }
  }, [authView, activeProfileId, guestMode, pushEnabled, notificationRefreshKey])
  useEffect(() => {
    if (!notificationOpen) return undefined
    const closeOnOutsideClick = (event) => {
      if (!notificationRef.current?.contains(event.target)) setNotificationOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setNotificationOpen(false)
    }
    window.addEventListener('pointerdown', closeOnOutsideClick)
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      window.removeEventListener('pointerdown', closeOnOutsideClick)
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [notificationOpen])
  useEffect(() => {
    if (!mobileMenuOpen) return undefined
    mobileNavigationRef.current?.querySelector('button')?.focus()
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') {
        setMobileMenuOpen(false)
        mobileMenuButtonRef.current?.focus()
      }
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [mobileMenuOpen])
  useEffect(() => {
    if (authView !== 'app' || (pushEnabled && isPushConfigured() && !isDemoMode()) || !notificationsEnabled || typeof Notification === 'undefined' || Notification.permission !== 'granted') return undefined
    let checking = false
    const checkLocalReminders = async () => {
      if (checking) return
      checking = true
      let data
      let notified
      try {
        data = guestMode
          ? JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
          : { alarms: await resourceApi.list('/alarms'), medications: await resourceApi.list('/medications') }
        if (!Array.isArray(data.alarms) || !Array.isArray(data.medications)) throw new Error('A API retornou uma lista inválida de lembretes.')
        notified = JSON.parse(localStorage.getItem('katui.local-alarm-notifications') || '[]')
        if (!Array.isArray(notified)) throw new Error('O histórico local de notificações está inválido.')
      } catch (error) {
        setGlobalActionError(error.message || 'Não foi possível verificar os alarmes locais.')
        checking = false
        return
      }
      const keys = new Set(notified)
      const dueReminders = getDueLocalReminders(data)
      dueReminders.forEach((reminder) => {
        if (keys.has(reminder.key)) return
        try {
          new Notification(reminder.title, { body: reminder.body, icon: `${import.meta.env.BASE_URL}katui-logo.png` })
          keys.add(reminder.key)
        } catch (error) {
          setGlobalActionError(error.message || 'O navegador não conseguiu exibir o lembrete local.')
        }
      })
      localStorage.setItem('katui.local-alarm-notifications', JSON.stringify([...keys].slice(-200)))
      checking = false
    }
    checkLocalReminders()
    const timer = window.setInterval(checkLocalReminders, 15_000)
    return () => window.clearInterval(timer)
  }, [authView, guestMode, notificationsEnabled, notificationPermission, activeProfileId, pushEnabled])
  useEffect(() => {
    const profile = profiles.find((item) => item.id === activeProfileId)
    if (!profile?.preferences) return
    const preferences = { ...defaultProfilePreferences, ...profile.preferences, accessibility: { ...defaultAccessibility, ...(profile.preferences.accessibility || {}) } }
    setDarkMode(Boolean(preferences.darkMode))
    setNotificationsEnabled(preferences.notificationsEnabled !== false)
    setWeeklySummaryEnabled(preferences.weeklySummaryEnabled !== false)
    setPushEnabled(localStorage.getItem(`katui.push-enabled.${profile.id}`) === 'true')
    setAccessibility(preferences.accessibility)
  }, [activeProfileId])
  useEffect(() => {
    if (active === 'Saúde feminina' && !femaleProfile) setActive('Visão geral')
  }, [active, femaleProfile])

}
