import { apiRequest, clearOfflineQueue, flushOfflineQueue, getOfflineQueueSize, isAiPrescriptionEnabled, isApiConfigured, isDemoMode, isGuestMode, isNetworkError, queueOfflineAction, resourceApi } from '../../services/api'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, defaultOnboardingForm, defaultAccessibility, defaultProfilePreferences, guestCollections, emptyGuestData, profileThemeOptions, getProfileTheme, iconAssets, getDueLocalReminders, getAlarmTimestamp, getLocalMedicationAlarm } from '../ui/dadosAplicacao'

export function useBackupActions(context) {
  const { setAuthView, setGlobalActionError, active, darkMode, setDarkMode, setOnboardingComplete, setOnboardingStep, setOnboardingForm, connection, setOfflineStorageError, setBackupBusy, setBackupStatus, profiles, setProfiles, activeProfileId, setActiveProfileId, notificationsEnabled, setNotificationsEnabled, weeklySummaryEnabled, setWeeklySummaryEnabled, accessibility, setAccessibility, activeProfile } = context
  const exportBackup = async () => {
    setBackupBusy(true)
    setBackupStatus('')
    try {
      const localMode = isGuestMode()
      const recordsByProfile = {}
      let collections
      if (localMode) {
        const stored = JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
        collections = Object.fromEntries(guestCollections.map((collection) => {
          const records = stored[collection] || []
          if (!Array.isArray(records)) throw new Error(`Os dados locais de ${collection} estão inválidos.`)
          return [collection, records]
        }))
      } else {
        const profilesToExport = profiles.length ? profiles : activeProfileId ? [{ id: activeProfileId }] : []
        if (!profilesToExport.length) throw new Error('Não há perfil ativo para exportar os registros.')
        await Promise.all(profilesToExport.map(async (profile) => {
          if (!profile.id) throw new Error('Um perfil sem identificador não pode ser exportado.')
          const profileRecords = {}
          await Promise.all(guestCollections.map(async (collection) => {
            const records = await apiRequest(`/${collection}`, { profileId: profile.id })
            if (!Array.isArray(records)) throw new Error(`A API retornou uma lista inválida para ${collection}.`)
            profileRecords[collection] = records
          }))
          recordsByProfile[profile.id] = profileRecords
        }))
      }
      const backup = {
        format: 'katui-personal-backup',
        version: 2,
        exportedAt: new Date().toISOString(),
        recordMode: localMode ? 'guest-local' : 'account',
        profiles,
        activeProfileId,
        preferences: { darkMode, notificationsEnabled, weeklySummaryEnabled, accessibility },
        profileDraft: secureStorage.getItem('katui.profile-draft'),
        ...(localMode ? { collections } : { recordsByProfile }),
      }
      const blob = new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `katui-backup-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 0)
      setBackupStatus('Backup exportado com perfis, preferências e registros de saúde. Tokens de sessão não são incluídos.')
    } catch (error) {
      setBackupStatus(error.message || 'Não foi possível exportar o backup.')
    } finally {
      setBackupBusy(false)
    }
  }
  
  const importBackup = (file) => {
    if (!window.confirm('Importar este backup? Em Guest, os registros locais atuais serão substituídos. Em uma conta, os dados serão mesclados sem apagar registros existentes.')) return
    setBackupBusy(true)
    setBackupStatus('')
    const reader = new FileReader()
    reader.onerror = () => {
      setBackupStatus('Não foi possível ler o arquivo selecionado.')
      setBackupBusy(false)
    }
    reader.onload = async () => {
      try {
        const backup = JSON.parse(String(reader.result || ''))
        if (backup.format !== 'katui-personal-backup' || ![1, 2].includes(backup.version) || !Array.isArray(backup.profiles)) throw new Error('O arquivo não é um backup válido do Katu’I.')
        const guestModeNow = isGuestMode()
        const nextPreferences = { ...defaultAccessibility, ...(backup.preferences?.accessibility || {}) }
        let nextProfiles = backup.profiles
        let nextActiveProfileId = backup.activeProfileId || backup.profiles[0]?.id || ''
  
        if (guestModeNow) {
          const guestRecords = backup.collections || backup.recordsByProfile?.[backup.activeProfileId]
          if (guestRecords !== undefined) {
            if (!guestRecords || typeof guestRecords !== 'object') throw new Error('A coleção de dados do backup está inválida.')
            const restored = Object.fromEntries(guestCollections.map((collection) => {
              const records = guestRecords[collection] || []
              if (!Array.isArray(records)) throw new Error(`A lista ${collection} do backup está inválida.`)
              return [collection, records]
            }))
            secureStorage.setItem('katui.guest-data', JSON.stringify(restored))
            if (nextProfiles.length) {
              nextProfiles = nextProfiles.map((profile, index) => ({ ...profile, id: profile.id || (index === 0 ? 'guest-local-profile' : `guest-local-${index}`) }))
              nextActiveProfileId = nextProfiles.some((profile) => profile.id === nextActiveProfileId) ? nextActiveProfileId : nextProfiles[0].id
              secureStorage.setItem('katui.guest-profile', JSON.stringify(nextProfiles.find((profile) => profile.id === nextActiveProfileId)))
            }
          }
        } else if (backup.recordsByProfile || backup.collections) {
          if (connection !== 'online') throw new Error('Conecte-se ao servidor para importar registros para sua conta.')
          const recordsByProfile = backup.recordsByProfile || { [backup.activeProfileId || backup.profiles[0]?.id || 'guest-local-profile']: backup.collections }
          if (typeof recordsByProfile !== 'object' || Array.isArray(recordsByProfile)) throw new Error('Os registros agrupados por perfil estão inválidos.')
          const nextCurrentProfiles = [...profiles]
          const profileMap = new Map()
          for (const sourceProfile of backup.profiles) {
            if (!sourceProfile?.id) continue
            let target = nextCurrentProfiles.find((profile) => profile.id === sourceProfile.id)
            const isPrincipal = sourceProfile.type === 'principal' || sourceProfile.relation === 'Perfil principal'
            if (!target && isPrincipal) target = nextCurrentProfiles.find((profile) => profile.type === 'principal' || profile.relation === 'Perfil principal') || activeProfile
            if (!target && !isPrincipal) target = nextCurrentProfiles.find((profile) => profile.name === sourceProfile.name && profile.relation === sourceProfile.relation)
            if (!target && !isPrincipal) {
              const { id, ...profileData } = sourceProfile
              const created = await resourceApi.create('/profiles', profileData)
              target = created?.profile || created
              if (!target?.id) throw new Error(`O servidor não confirmou a criação do perfil ${sourceProfile.name || ''}.`)
              nextCurrentProfiles.push(target)
            }
            if (target?.id) profileMap.set(sourceProfile.id, target.id)
          }
          for (const [sourceProfileId, profileCollections] of Object.entries(recordsByProfile)) {
            const targetProfileId = profileMap.get(sourceProfileId) || activeProfileId
            if (!targetProfileId || !profileCollections || typeof profileCollections !== 'object') continue
            for (const collection of guestCollections) {
              const importedRecords = profileCollections[collection] || []
              if (!Array.isArray(importedRecords)) throw new Error(`A lista ${collection} do backup está inválida.`)
              if (!importedRecords.length) continue
              const currentRecords = await apiRequest(`/${collection}`, { profileId: targetProfileId })
              if (!Array.isArray(currentRecords)) throw new Error(`A API retornou uma lista inválida para ${collection}.`)
              for (const importedRecord of importedRecords) {
                const sourceId = importedRecord.id || importedRecord._id
                const existing = sourceId && currentRecords.find((record) => String(record.id || record._id) === String(sourceId))
                const { id, _id, profileId, __calendarResource, ...recordData } = importedRecord
                const data = { ...recordData, profileId: targetProfileId }
                if (existing) await resourceApi.update(`/${collection}`, existing.id || existing._id, data)
                else await resourceApi.create(`/${collection}`, data)
              }
            }
          }
          nextProfiles = nextCurrentProfiles
          nextActiveProfileId = profileMap.get(backup.activeProfileId) || activeProfileId || nextProfiles[0]?.id || ''
        }
  
        secureStorage.setItem('katui.profiles', JSON.stringify(nextProfiles))
        if (backup.profileDraft) secureStorage.setItem('katui.profile-draft', backup.profileDraft)
        await flushSecureStorage()
        setProfiles(nextProfiles)
        setActiveProfileId(nextActiveProfileId)
        localStorage.setItem('katui.active-profile-id', nextActiveProfileId)
        setDarkMode(Boolean(backup.preferences?.darkMode))
        setNotificationsEnabled(backup.preferences?.notificationsEnabled !== false)
        setWeeklySummaryEnabled(backup.preferences?.weeklySummaryEnabled !== false)
        setAccessibility(nextPreferences)
        if (backup.preferences) localStorage.setItem('katui.accessibility', JSON.stringify(nextPreferences))
        window.dispatchEvent(new CustomEvent('katui:resource-change', { detail: { endpoint: '/overview' } }))
        setBackupStatus(backup.version === 1 ? 'Perfis e preferências restaurados. Este backup antigo não continha registros de saúde.' : 'Backup importado. Os registros foram restaurados/mesclados com sucesso.')
      } catch (error) {
        setBackupStatus(error.message || 'Não foi possível importar o backup.')
      } finally {
        setBackupBusy(false)
      }
    }
    reader.readAsText(file)
  }
  
  const clearLocalData = async () => {
    if (!window.confirm('Apagar os dados locais do Katu’I neste dispositivo? Essa ação não pode ser desfeita.')) return
    try {
      await clearSecureStorage()
      localStorage.clear()
    } catch (error) {
      setGlobalActionError(error.message || 'Não foi possível apagar os dados locais protegidos.')
      return
    }
    clearOfflineQueue().catch((error) => setOfflineStorageError(error.message || 'Não foi possível limpar a fila offline.'))
    setProfiles([])
    setActiveProfileId('')
    setDarkMode(false)
    setNotificationsEnabled(true)
    setWeeklySummaryEnabled(true)
    setAccessibility(defaultAccessibility)
    setOnboardingForm(defaultOnboardingForm)
    setOnboardingStep(0)
    setOnboardingComplete(false)
    setAuthView('landing')
  }
  return { exportBackup, importBackup, clearLocalData }
}
