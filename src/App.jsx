import { AppShell } from './app/AppShell'
import { useAccountActions } from './app/actions/useAccountActions'
import { useBackupActions } from './app/actions/useBackupActions'
import { useFeedbackActions } from './app/actions/useFeedbackActions'
import { useOnboardingActions } from './app/actions/useOnboardingActions'
import { useProfileActions } from './app/actions/useProfileActions'
import { useRecordActions } from './app/actions/useRecordActions'
import { useSessionActions } from './app/actions/useSessionActions'
import { useUndoActions } from './app/actions/useUndoActions'
import { useAppState } from './app/useAppState'
import { useInitialEffects } from './app/useInitialEffects'
import { useProfileEffects } from './app/useProfileEffects'
import { isGuestMode } from './services/api'
import { supportNavigation } from './services/resources'

function App() {
  const appState = useAppState()
  const activeProfile =
    appState.profiles.find((profile) => profile.id === appState.activeProfileId) || appState.profiles[0] || null
  const guestMode = isGuestMode()
  const femaleProfile = ['feminino', 'female', 'f'].includes(String(activeProfile?.sex || '').toLowerCase())
  const visibleSupportNavigation = supportNavigation.filter((item) => item.label !== 'Saúde feminina' || femaleProfile)
  const context = { ...appState, activeProfile, guestMode, femaleProfile, visibleSupportNavigation }

  useInitialEffects(context)
  useProfileEffects(context)

  const sessionActions = useSessionActions(context)
  const profileActions = useProfileActions({ ...context, ...sessionActions })
  const feedbackActions = useFeedbackActions(context)
  const undoActions = useUndoActions({ ...context, ...feedbackActions })
  const recordActions = useRecordActions({
    ...context,
    ...sessionActions,
    ...profileActions,
    ...feedbackActions,
    ...undoActions,
  })
  const backupActions = useBackupActions(context)
  const accountActions = useAccountActions({ ...context, ...recordActions })
  const onboardingActions = useOnboardingActions(context)
  const actions = {
    ...sessionActions,
    ...profileActions,
    ...feedbackActions,
    ...undoActions,
    ...recordActions,
    ...backupActions,
    ...accountActions,
    ...onboardingActions,
  }

  return <AppShell context={{ ...context, ...actions }} />
}

export default App
