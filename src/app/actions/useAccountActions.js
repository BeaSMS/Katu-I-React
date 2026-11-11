import { useEffect, useRef, useState } from 'react'
import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'

export function useAccountActions(context) {
  const { authView, setGlobalActionError, connection, undoInfo, undoInProgress, profiles, setProfiles, activeProfileId, accountLoaded, guestMigrationPending, setGuestMigrationPending, activeProfile, guestMode, submitAction } = context
  const syncBackup = () => submitAction({ kind: 'form', title: 'Backup pessoal', endpoint: '/backup', data: { name: 'Backup pessoal', detail: 'Sincronização manual' } })
  const savePregnancy = (data) => submitAction({ kind: 'form', title: data.enabled ? 'Gestação ativada' : 'Gestação desativada', endpoint: '/pregnancy', data: { enabled: data.enabled, startDate: data.startDate, profileId: activeProfileId } })
  const saveProfileTheme = async (themeId) => {
    const theme = profileThemeOptions.find((option) => option.id === themeId) || profileThemeOptions[0]
    await submitAction({ kind: 'form', title: 'Cor do perfil', endpoint: `/profiles/${activeProfileId}`, method: 'PATCH', data: { themeColor: theme.id, corHex: theme.color } })
    const nextProfiles = profiles.map((profile) => profile.id === activeProfileId ? { ...profile, themeColor: theme.id, corHex: theme.color } : profile)
    setProfiles(nextProfiles)
    secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
  }
  const saveAiSettings = async ({ provider, model, apiKey }) => {
    const ai = { provider: provider || 'system', model: model || '', configured: provider !== 'system' && Boolean(apiKey || activeProfile?.preferences?.ai?.configured), maskedKey: provider !== 'system' ? (activeProfile?.preferences?.ai?.maskedKey || '') : '' }
    await submitAction({ kind: 'form', title: 'Configuração de IA', endpoint: `/profiles/${activeProfileId}/ai-configuracoes`, method: 'PATCH', sensitive: Boolean(apiKey), data: { provider: ai.provider, model: ai.model, ...(apiKey ? { apiKey } : {}) } })
    updateProfilePreferences({ ai })
  }
  
  const migrateGuestRecords = async (profileId) => {
    if (localStorage.getItem('katui.guest-migration-consent') !== 'true' || !profileId || String(profileId).startsWith('local-')) {
      setGuestMigrationPending(true)
      return
    }
    const guestData = JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
    const completed = new Set(JSON.parse(localStorage.getItem('katui.guest-migration-completed') || '[]'))
    const failures = []
    for (const collection of guestCollections) {
      const records = Array.isArray(guestData[collection]) ? guestData[collection] : []
      for (let index = 0; index < records.length; index += 1) {
        const record = records[index]
        const migrationId = `${collection}:${record.id || index}`
        if (completed.has(migrationId)) continue
        const { id, _id, ...recordData } = record
        try {
          await apiRequest(`/${collection}`, {
            method: 'POST',
            idempotencyKey: `guest-migration-${migrationId}`,
            body: JSON.stringify({ ...recordData, profileId }),
          })
          completed.add(migrationId)
          localStorage.setItem('katui.guest-migration-completed', JSON.stringify([...completed]))
        } catch (error) {
          failures.push(`${collection}: ${error.message || 'falha ao importar'}`)
        }
      }
    }
    if (failures.length) {
      setGuestMigrationPending(true)
      setGlobalActionError(`Alguns registros Guest não foram importados (${failures.join('; ')}). Os originais continuam salvos neste dispositivo; tente novamente quando a conexão estiver disponível.`)
      return
    }
    localStorage.removeItem('katui.guest-migration-consent')
    localStorage.removeItem('katui.guest-migration-completed')
    localStorage.removeItem('katui.guest-migration-profile-id')
    setGuestMigrationPending(false)
    setGlobalActionError('')
  }
  
  const retryGuestMigration = () => {
    if (connection !== 'online') {
      setGlobalActionError('Conecte-se à internet para continuar a importação. Os registros Guest continuam neste dispositivo.')
      return
    }
    migrateGuestRecords(localStorage.getItem('katui.guest-migration-profile-id') || activeProfileId).catch((error) => setGlobalActionError(error.message || 'Não foi possível importar os registros Guest.'))
  }
  
  useEffect(() => {
    if (authView !== 'app' || guestMode || !guestMigrationPending || !accountLoaded || connection !== 'online' || !activeProfileId || activeProfileId === 'guest-local-profile' || String(activeProfileId).startsWith('local-')) return
    migrateGuestRecords(localStorage.getItem('katui.guest-migration-profile-id') || activeProfileId).catch((error) => {
      setGuestMigrationPending(true)
      setGlobalActionError(error.message || 'Não foi possível importar os registros Guest. Os originais continuam neste dispositivo.')
    })
  }, [authView, guestMode, guestMigrationPending, accountLoaded, connection, activeProfileId])
  
  useEffect(() => {
    const handleUndoShortcut = (event) => {
      if (!(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 'z' || event.shiftKey || !undoInfo || undoInProgress) return
      const target = event.target
      if (target instanceof HTMLElement && (target.isContentEditable || target.closest('input, textarea, select, [contenteditable="true"]'))) return
      event.preventDefault()
      undoLastAction()
    }
    window.addEventListener('keydown', handleUndoShortcut)
    return () => window.removeEventListener('keydown', handleUndoShortcut)
  }, [undoInfo, undoInProgress])
  return { syncBackup, savePregnancy, saveProfileTheme, saveAiSettings, migrateGuestRecords, retryGuestMigration }
}
