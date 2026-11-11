import { useRef, useState } from 'react'
import { isGuestMode } from '../services/api'
import { secureStorage } from '../services/secureStorage'
import { defaultAccessibility, defaultOnboardingForm, defaultProfilePreferences } from './ui/dadosAplicacao'
import { localDateKey, readGuestProfile } from './ui/interfaceCompartilhada'

export function useAppState() {
  const [authView, setAuthView] = useState(() => localStorage.getItem('katui.auth-token') || localStorage.getItem('katui.guest-mode') === 'true' ? 'app' : 'landing')
  const [authLoading, setAuthLoading] = useState(false)
  const [authError, setAuthError] = useState('')
  const [onboardingError, setOnboardingError] = useState('')
  const [globalActionError, setGlobalActionError] = useState('')
  const [active, setActive] = useState('Visão geral')
  const [selectedDate, setSelectedDate] = useState(() => localStorage.getItem('katui.selected-date') || localDateKey(new Date()))
  const [dashboardCalendarExpanded, setDashboardCalendarExpanded] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const mobileMenuButtonRef = useRef(null)
  const mobileNavigationRef = useRef(null)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationOpen, setNotificationOpen] = useState(false)
  const [notificationAlarms, setNotificationAlarms] = useState([])
  const [notificationLoading, setNotificationLoading] = useState(false)
  const [notificationError, setNotificationError] = useState('')
  const [notificationRefreshKey, setNotificationRefreshKey] = useState(0)
  const notificationRef = useRef(null)
  const [quickAddOpen, setQuickAddOpen] = useState(false)
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('katui.dark-mode') === 'true')
  const [onboardingComplete, setOnboardingComplete] = useState(() => localStorage.getItem('katui.guest-mode') === 'true' ? Boolean(readGuestProfile()?.name) : localStorage.getItem('katui.onboarding-complete') === 'true')
  const [onboardingStep, setOnboardingStep] = useState(() => Number(localStorage.getItem('katui.onboarding-step') || 0))
  const [onboardingForm, setOnboardingForm] = useState(() => {
    try {
      const savedGuest = localStorage.getItem('katui.guest-mode') === 'true' ? readGuestProfile() : null
      return JSON.parse(secureStorage.getItem('katui.onboarding-draft')) || (savedGuest ? { ...defaultOnboardingForm, name: savedGuest.name || '', age: savedGuest.age ? String(savedGuest.age) : '', sex: savedGuest.sex || 'nao-informado', weight: savedGuest.weight ? String(savedGuest.weight) : '', height: savedGuest.heightCm ? String(savedGuest.heightCm) : '', allergies: savedGuest.allergies || '', conditions: savedGuest.specialConditions || '', aiEnabled: false } : defaultOnboardingForm)
    } catch {
      return defaultOnboardingForm
    }
  })
  const [modal, setModal] = useState(null)
  const [connection, setConnection] = useState('checking')
  const [audioFeedbackEnabled, setAudioFeedbackEnabled] = useState(() => localStorage.getItem('katui.audio-feedback') === 'true')
  const [undoInfo, setUndoInfo] = useState(null)
  const [undoInProgress, setUndoInProgress] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState('')
  const undoStackRef = useRef([])
  const undoInProgressRef = useRef(false)
  const audioContextRef = useRef(null)
  const [pendingCount, setPendingCount] = useState(0)
  const [offlineStorageError, setOfflineStorageError] = useState('')
  const [backupBusy, setBackupBusy] = useState(false)
  const [backupStatus, setBackupStatus] = useState('')
  const [profiles, setProfiles] = useState(() => {
    try { return JSON.parse(secureStorage.getItem('katui.profiles')) || [] } catch { return [] }
  })
  const [activeProfileId, setActiveProfileId] = useState(() => localStorage.getItem('katui.active-profile-id') || '')
  const [pushEnabled, setPushEnabled] = useState(() => localStorage.getItem(`katui.push-enabled.${localStorage.getItem('katui.active-profile-id') || ''}`) === 'true')
  const [notificationPermission, setNotificationPermission] = useState(() => typeof Notification === 'undefined' ? 'unsupported' : Notification.permission)
  const [accountLoaded, setAccountLoaded] = useState(false)
  const [calendarRecords, setCalendarRecords] = useState([])
  const [calendarLoadError, setCalendarLoadError] = useState('')
  const [guestMigrationPending, setGuestMigrationPending] = useState(() => localStorage.getItem('katui.guest-migration-consent') === 'true')
  const [notificationsEnabled, setNotificationsEnabled] = useState(() => localStorage.getItem('katui.notifications') !== 'false')
  const [weeklySummaryEnabled, setWeeklySummaryEnabled] = useState(() => localStorage.getItem('katui.weekly-summary') !== 'false')
  const [accessibility, setAccessibility] = useState(() => {
    try { return { ...defaultAccessibility, ...JSON.parse(localStorage.getItem('katui.accessibility') || '{}') } } catch { return defaultAccessibility }
  })

  return { authView, setAuthView, authLoading, setAuthLoading, authError, setAuthError, onboardingError, setOnboardingError, globalActionError, setGlobalActionError, active, setActive, selectedDate, setSelectedDate, dashboardCalendarExpanded, setDashboardCalendarExpanded, mobileMenuOpen, setMobileMenuOpen, mobileMenuButtonRef, mobileNavigationRef, profileOpen, setProfileOpen, notificationOpen, setNotificationOpen, notificationAlarms, setNotificationAlarms, notificationLoading, setNotificationLoading, notificationError, setNotificationError, notificationRefreshKey, setNotificationRefreshKey, notificationRef, quickAddOpen, setQuickAddOpen, darkMode, setDarkMode, onboardingComplete, setOnboardingComplete, onboardingStep, setOnboardingStep, onboardingForm, setOnboardingForm, modal, setModal, connection, setConnection, audioFeedbackEnabled, setAudioFeedbackEnabled, undoInfo, setUndoInfo, undoInProgress, setUndoInProgress, feedbackMessage, setFeedbackMessage, undoStackRef, undoInProgressRef, audioContextRef, pendingCount, setPendingCount, offlineStorageError, setOfflineStorageError, backupBusy, setBackupBusy, backupStatus, setBackupStatus, profiles, setProfiles, activeProfileId, setActiveProfileId, pushEnabled, setPushEnabled, notificationPermission, setNotificationPermission, accountLoaded, setAccountLoaded, calendarRecords, setCalendarRecords, calendarLoadError, setCalendarLoadError, guestMigrationPending, setGuestMigrationPending, notificationsEnabled, setNotificationsEnabled, weeklySummaryEnabled, setWeeklySummaryEnabled, accessibility, setAccessibility }
}
