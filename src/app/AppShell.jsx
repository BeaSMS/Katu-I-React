import {
  apiRequest,
  clearOfflineQueue,
  flushOfflineQueue,
  getOfflineQueueSize,
  isAiPrescriptionEnabled,
  isApiConfigured,
  isDemoMode,
  isGuestMode,
  isNetworkError,
  queueOfflineAction,
  resourceApi,
} from '../services/api'
import {
  disableBackgroundPush,
  enableBackgroundPush,
  isPushConfigured,
  listBackgroundAlarms,
  removeMedicationReminders,
  removeSingleReminder,
  scheduleMedicationReminders,
  scheduleRestoredAlarm,
  scheduleSingleReminder,
} from '../services/push'
import { moduleNavigation, supportNavigation, getResourceForLabel } from '../services/resources'
import {
  medications,
  defaultOnboardingForm,
  defaultAccessibility,
  defaultProfilePreferences,
  guestCollections,
  emptyGuestData,
  profileThemeOptions,
  getProfileTheme,
  iconAssets,
  getAlarmTimestamp,
  getLocalMedicationAlarm,
} from './ui/dadosAplicacao'
import {
  Icon,
  Brand,
  EmptyState,
  vitalNumericValue,
  vitalValueLabel,
  recordDetailEntries,
  getAttachmentHref,
  localDateKey,
  readGuestProfile,
  parseDateKey,
  recordDate,
  recordOccursOnDate,
  hasCalendarRecordOnDate,
  calendarWeekdays,
  calendarMonths,
} from './ui/interfaceCompartilhada'
import { calendarResources, CalendarCard, ModulePage, MedicationPage } from './ui/modulosCalendario'
import { VitalsPage, QuickAddMenu } from './ui/sinaisVitais'
import { ResourcePage, EmptyModulePage, AiSettings, HelpPage } from './ui/recursos'
import {
  SettingsPage,
  BackupPage,
  DependentModal,
  DependentHealthStep,
  ProfileColorModal,
} from './ui/configuracoes'
import { ActionModal } from './ui/modalAcao'
import {
  ConnectionBadge,
  SyncBadge,
  hasGuestRecords,
  AuthPage,
  Onboarding,
  HealthTagsInput,
  OnboardingField,
  DashboardSummary,
} from './ui/autenticacaoEPainel'

export function AppShell({ context }) {
  const {
    authView,
    setAuthView,
    authLoading,
    setAuthLoading,
    authError,
    setAuthError,
    onboardingError,
    globalActionError,
    active,
    setActive,
    selectedDate,
    setSelectedDate,
    dashboardCalendarExpanded,
    setDashboardCalendarExpanded,
    mobileMenuOpen,
    setMobileMenuOpen,
    mobileMenuButtonRef,
    mobileNavigationRef,
    profileOpen,
    setProfileOpen,
    notificationOpen,
    setNotificationOpen,
    notificationAlarms,
    notificationLoading,
    notificationError,
    setNotificationRefreshKey,
    notificationRef,
    quickAddOpen,
    setQuickAddOpen,
    darkMode,
    onboardingComplete,
    onboardingStep,
    setOnboardingStep,
    onboardingForm,
    setOnboardingForm,
    modal,
    setModal,
    connection,
    audioFeedbackEnabled,
    undoInfo,
    undoInProgress,
    feedbackMessage,
    setFeedbackMessage,
    pendingCount,
    offlineStorageError,
    backupBusy,
    backupStatus,
    profiles,
    pushEnabled,
    notificationPermission,
    calendarRecords,
    calendarLoadError,
    guestMigrationPending,
    notificationsEnabled,
    weeklySummaryEnabled,
    accessibility,
    activeProfile,
    guestMode,
    femaleProfile,
    visibleSupportNavigation,
    openAction,
    startGuestMode,
    handleAuth,
    logout,
    toggleProfilePreference,
    toggleBackgroundPush,
    enableLocalNotifications,
    updateProfileAccessibility,
    selectProfile,
    toggleAudioFeedback,
    undoLastAction,
    submitAction,
    exportBackup,
    importBackup,
    clearLocalData,
    syncBackup,
    savePregnancy,
    saveProfileTheme,
    saveAiSettings,
    retryGuestMigration,
    finishOnboarding,
    skipOnboarding,
  } = context
  if (authView !== 'app')
    return (
      <AuthPage
        view={authView}
        onViewChange={(view) => {
          setAuthError('')
          setAuthView(view)
        }}
        onSubmit={(form, mode) => {
          setAuthError('')
          setAuthLoading(true)
          handleAuth(form, mode)
            .catch((error) => setAuthError(error.message || 'Não foi possível concluir o acesso.'))
            .finally(() => setAuthLoading(false))
        }}
        onGuestLogin={startGuestMode}
        loading={authLoading}
        error={authError}
      />
    )
  if (!onboardingComplete)
    return (
      <Onboarding
        step={onboardingStep}
        setStep={setOnboardingStep}
        form={onboardingForm}
        setForm={setOnboardingForm}
        onFinish={finishOnboarding}
        onSkip={skipOnboarding}
        darkMode={darkMode}
        error={onboardingError}
        isGuest={guestMode}
      />
    )

  const textScale = Number(accessibility.textScale || (accessibility.largeText ? 125 : 100))
  const accessibilityClasses = [
    darkMode ? 'dark-mode' : '',
    textScale > 100 ? `text-scale-${textScale}` : '',
    accessibility.dyslexiaFont ? 'dyslexia-font' : '',
    accessibility.highContrast ? 'high-contrast' : '',
    accessibility.reduceMotion ? 'reduce-motion' : '',
  ]
    .filter(Boolean)
    .join(' ')
  const activeTheme = getProfileTheme(activeProfile)
  const themeStyle = {
    '--profile-accent': activeTheme.color,
    '--profile-accent-soft': activeTheme.soft,
    '--profile-accent-dark': activeTheme.dark,
    '--profile-accent-ink': activeTheme.ink,
    '--accessibility-zoom': textScale / 100,
    '--accessibility-scale': textScale / 100,
  }
  return (
    <div className={`app-shell ${accessibilityClasses}`} style={themeStyle}>
      <aside className="sidebar">
        <Brand />
        <div className="sidebar-label">Menu principal</div>
        <nav>
          {moduleNavigation.map((item) => (
            <button
              key={item.label}
              className={`nav-item nav-${item.resource}${active === item.label ? ' active' : ''}`}
              onClick={() => setActive(item.label)}
            >
              <Icon name={item.icon} size={19} />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-label support-label">Sua saúde</div>
        <nav>
          {visibleSupportNavigation
            .filter((item) => !guestMode || !['Perfis e dependentes', 'Backup pessoal'].includes(item.label))
            .map((item) => (
              <button
                key={item.label}
                className={`nav-item nav-${item.resource}${active === item.label ? ' active' : ''}`}
                onClick={() => setActive(item.label)}
              >
                <Icon name={item.icon} size={19} />
                <span>{item.label}</span>
              </button>
            ))}
        </nav>
        <div className="sidebar-bottom">
          <button className={active === 'Ajuda' ? 'nav-item active' : 'nav-item'} onClick={() => setActive('Ajuda')}>
            <Icon name="help" size={19} />
            <span>Ajuda</span>
          </button>
          <button
            className={active === 'Configurações' ? 'nav-item active' : 'nav-item'}
            onClick={() => setActive('Configurações')}
          >
            <Icon name="settings" size={19} />
            <span>Configurações</span>
          </button>
          <button className="nav-item" onClick={logout}>
            <Icon name="arrow" size={19} />
            <span>{guestMode ? 'Sair do modo Guest' : isDemoMode() ? 'Sair da demonstração' : 'Sair da conta'}</span>
          </button>
        </div>
      </aside>
      {mobileMenuOpen && (
        <div
          className="mobile-navigation-overlay"
          onMouseDown={(event) => event.target === event.currentTarget && setMobileMenuOpen(false)}
        >
          <button
            type="button"
            className="mobile-navigation-backdrop"
            aria-label="Fechar menu"
            onClick={() => setMobileMenuOpen(false)}
          />
          <nav
            ref={mobileNavigationRef}
            id="mobile-navigation"
            className="mobile-navigation-panel"
            aria-label="Navegação principal"
          >
            <button
              type="button"
              className="mobile-navigation-close"
              aria-label="Fechar menu"
              onClick={() => setMobileMenuOpen(false)}
            >
              ×
            </button>
            <Brand />
            <div className="sidebar-label">Menu principal</div>
            {moduleNavigation.map((item) => (
              <button
                key={item.label}
                className={`nav-item${active === item.label ? ' active' : ''}`}
                aria-current={active === item.label ? 'page' : undefined}
                onClick={() => {
                  setActive(item.label)
                  setMobileMenuOpen(false)
                }}
              >
                <Icon name={item.icon} size={19} />
                <span>{item.label}</span>
              </button>
            ))}
            <div className="sidebar-label support-label">Sua saúde</div>
            {visibleSupportNavigation
              .filter((item) => !guestMode || !['Perfis e dependentes', 'Backup pessoal'].includes(item.label))
              .map((item) => (
                <button
                  key={item.label}
                  className={`nav-item${active === item.label ? ' active' : ''}`}
                  aria-current={active === item.label ? 'page' : undefined}
                  onClick={() => {
                    setActive(item.label)
                    setMobileMenuOpen(false)
                  }}
                >
                  <Icon name={item.icon} size={19} />
                  <span>{item.label}</span>
                </button>
              ))}
            <div className="mobile-navigation-footer">
              <button
                className="nav-item"
                aria-current={active === 'Ajuda' ? 'page' : undefined}
                onClick={() => {
                  setActive('Ajuda')
                  setMobileMenuOpen(false)
                }}
              >
                <Icon name="help" size={19} />
                <span>Ajuda</span>
              </button>
              <button
                className="nav-item"
                aria-current={active === 'Configurações' ? 'page' : undefined}
                onClick={() => {
                  setActive('Configurações')
                  setMobileMenuOpen(false)
                }}
              >
                <Icon name="settings" size={19} />
                <span>Configurações</span>
              </button>
              <button
                className="nav-item"
                onClick={() => {
                  setMobileMenuOpen(false)
                  logout()
                }}
              >
                <Icon name="arrow" size={19} />
                <span>{guestMode ? 'Sair do modo Guest' : isDemoMode() ? 'Sair da demonstração' : 'Sair da conta'}</span>
              </button>
            </div>
          </nav>
        </div>
      )}
      <main className="main-content">
        <header className="topbar">
          <button
            ref={mobileMenuButtonRef}
            type="button"
            className="mobile-menu"
            aria-label="Abrir menu de navegação"
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-navigation"
            onClick={() => setMobileMenuOpen((open) => !open)}
          >
            <Icon name="grid" size={20} />
          </button>
          <div className="breadcrumb">
            Meu espaço <span>/</span> <strong>{active}</strong>
          </div>
          <div className="top-actions">
            <ConnectionBadge status={connection} />
            <SyncBadge connection={connection} pendingCount={pendingCount} error={offlineStorageError} />
            <div className="notification-wrap" ref={notificationRef}>
              <button
                type="button"
                className="icon-button notification"
                aria-label={`Alarmes${notificationAlarms.length ? `, ${notificationAlarms.length} próximos` : ''}`}
                aria-haspopup="dialog"
                aria-expanded={notificationOpen}
                onClick={() => {
                  setNotificationOpen((open) => !open)
                  setNotificationRefreshKey((current) => current + 1)
                }}
              >
                <Icon name="bell" size={20} />
                {notificationAlarms.length > 0 && (
                  <span className="notification-count">
                    {notificationAlarms.length > 99 ? '99+' : notificationAlarms.length}
                  </span>
                )}
              </button>
              {notificationOpen && (
                <section className="notification-panel" role="dialog" aria-label="Próximos alarmes">
                  <div className="notification-panel-header">
                    <div>
                      <strong>Próximos alarmes</strong>
                      <small>
                        {notificationAlarms.length
                          ? `${notificationAlarms.length} lembrete(s) futuro(s)`
                          : 'Lembretes deste perfil'}
                      </small>
                    </div>
                    <button type="button" aria-label="Fechar notificações" onClick={() => setNotificationOpen(false)}>
                      ×
                    </button>
                  </div>
                  {!guestMode && !isDemoMode() && !pushEnabled && (
                    <button
                      type="button"
                      className="notification-setup-note"
                      onClick={() => {
                        setNotificationOpen(false)
                        setActive('Configurações')
                      }}
                    >
                      Ative os alarmes em segundo plano nas Configurações para receber avisos com o site fechado.
                    </button>
                  )}
                  {(guestMode || isDemoMode()) && (
                    <p className="notification-local-note">
                      {isDemoMode()
                        ? 'Na demonstração, ative notificações locais em Configurações. Os avisos só funcionam com o app aberto.'
                        : 'No modo Guest, os avisos só funcionam enquanto o site estiver aberto.'}
                    </p>
                  )}
                  {notificationLoading && <p className="notification-state">Atualizando alarmes…</p>}
                  {notificationError && (
                    <p className="notification-state notification-error" role="alert">
                      {notificationError}
                    </p>
                  )}
                  {!notificationLoading && notificationAlarms.length === 0 && !notificationError && (
                    <p className="notification-state">Nenhum alarme futuro cadastrado.</p>
                  )}
                  {notificationAlarms.length > 0 && (
                    <div className="notification-list">
                      {notificationAlarms.slice(0, 5).map((alarm) => {
                        const when = new Date(alarm.__notificationAt)
                        const title = alarm.name || alarm.title || 'Lembrete de saúde'
                        return (
                          <button
                            type="button"
                            className="notification-item"
                            key={alarm.id || `${title}-${alarm.__notificationAt}`}
                            onClick={() => {
                              setSelectedDate(localDateKey(when))
                              setActive('Alarmes')
                              setNotificationOpen(false)
                            }}
                          >
                            <span className="notification-item-icon">
                              <Icon name={alarm.sourceKind === 'medication' ? 'pill' : 'bell'} size={16} />
                            </span>
                            <span className="notification-item-copy">
                              <strong>{title}</strong>
                              <small>
                                {when.toLocaleDateString('pt-BR', { weekday: 'short', day: 'numeric', month: 'short' })}{' '}
                                · {when.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
                              </small>
                            </span>
                          </button>
                        )
                      })}
                    </div>
                  )}
                  <button
                    type="button"
                    className="notification-all"
                    onClick={() => {
                      setActive('Alarmes')
                      setNotificationOpen(false)
                    }}
                  >
                    Ver todos os alarmes <Icon name="arrow" size={14} />
                  </button>
                </section>
              )}
            </div>
            {guestMode ? (
              <div className="guest-account-actions">
                <button
                  className="guest-login-cta"
                  onClick={() => {
                    setAuthError('')
                    setAuthView('login')
                  }}
                >
                  Entrar
                </button>
                <button
                  className="guest-register-cta"
                  onClick={() => {
                    setAuthError('')
                    setAuthView('register')
                  }}
                >
                  Registre-se para fazer backup
                </button>
              </div>
            ) : (
              <div className="profile-wrap">
                <button
                  className="profile-button"
                  aria-label="Trocar perfil de cuidado"
                  aria-haspopup="menu"
                  aria-expanded={profileOpen}
                  onClick={() => setProfileOpen(!profileOpen)}
                >
                  <div className="avatar">{activeProfile?.name?.slice(0, 2).toUpperCase() || '?'}</div>
                  <div className="profile-name">
                    <small className="profile-switch-label">Trocar perfil</small>
                    <strong>{activeProfile?.name || 'Meu perfil'}</strong>
                    <small>{activeProfile?.relation || 'Perfil principal'}</small>
                  </div>
                  <span className="profile-chevron">⌄</span>
                </button>
                {profileOpen && (
                  <div className="profile-menu" role="menu">
                    <div className="profile-menu-header">
                      <strong>Perfil de cuidado</strong>
                      <small>Escolha quem você está acompanhando</small>
                    </div>
                    {profiles.length ? (
                      profiles.map((profile) => (
                        <button
                          key={profile.id}
                          role="menuitem"
                          className={profile.id === activeProfile?.id ? 'profile-option selected' : 'profile-option'}
                          onClick={() => selectProfile(profile)}
                        >
                          <span
                            className="profile-option-avatar"
                            style={{ background: getProfileTheme(profile).soft, color: getProfileTheme(profile).color }}
                          >
                            {profile.name.slice(0, 2).toUpperCase()}
                          </span>
                          <span>
                            <b>{profile.name}</b>
                            <small>{profile.relation}</small>
                          </span>
                          {profile.id === activeProfile?.id && <Icon name="check" size={14} />}
                        </button>
                      ))
                    ) : (
                      <p className="profile-empty">Conclua o tutorial para criar o perfil principal.</p>
                    )}
                    <button
                      className="profile-add"
                      onClick={() => {
                        setProfileOpen(false)
                        openAction({ kind: 'form', title: 'Adicionar perfil dependente', endpoint: '/profiles' })
                      }}
                    >
                      <Icon name="plus" size={14} /> Adicionar dependente
                    </button>
                    <button
                      className="profile-logout"
                      role="menuitem"
                      onClick={() => {
                        setProfileOpen(false)
                        logout()
                      }}
                    >
                      <Icon name="arrow" size={14} /> {isDemoMode() ? 'Sair da demonstração' : 'Sair da conta'}
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </header>
        <div className="content-wrap">
          {guestMigrationPending && !guestMode && (
            <div className="guest-migration-banner" role="status">
              <span>Há registros Guest aguardando importação. Os originais continuam salvos neste dispositivo.</span>
              <button className="soft-btn" disabled={connection !== 'online'} onClick={retryGuestMigration}>
                {connection === 'online' ? 'Tentar importar novamente' : 'Aguardando conexão'}
              </button>
            </div>
          )}
          {active === 'Ajuda' ? (
            <HelpPage />
          ) : active === 'Visão geral' ? (
            <>
              <section className="welcome-row">
                <div>
                  <p className="eyebrow">
                    {guestMode
                      ? 'MODO GUEST · DADOS NESTE DISPOSITIVO'
                      : isDemoMode()
                        ? 'DEMONSTRAÇÃO · DADOS FICTÍCIOS'
                        : 'SEU ESPAÇO DE SAÚDE'}
                  </p>
                  <h1>
                    Olá{activeProfile?.name ? ` ${activeProfile.name}` : ''} <span>✦</span>
                  </h1>
                  <p className="welcome-copy">
                    {guestMode
                      ? 'Comece registrando suas informações. Elas ficam salvas neste navegador.'
                      : isDemoMode()
                        ? 'Explore os recursos e alterne entre Ana e Lucas no seletor de perfil.'
                        : 'Seu espaço está pronto. Comece adicionando sua primeira informação.'}
                  </p>
                </div>
                <div className="quick-add-wrap">
                  <button
                    className="outline-btn"
                    aria-expanded={quickAddOpen}
                    aria-haspopup="menu"
                    onClick={() => setQuickAddOpen(!quickAddOpen)}
                  >
                    <Icon name="plus" size={17} /> Adicionar registro
                  </button>
                  {quickAddOpen && (
                    <QuickAddMenu
                      femaleProfile={femaleProfile}
                      isGuest={guestMode}
                      aiAvailable={isDemoMode() || isAiPrescriptionEnabled()}
                      onAction={(action) => {
                        setQuickAddOpen(false)
                        openAction(action)
                      }}
                    />
                  )}
                </div>
              </section>
              <section className="hero-card">
                <div className="hero-copy">
                  <span className="hero-kicker">SEU RESUMO DE HOJE</span>
                  <h2>Como você está se sentindo?</h2>
                  <p>Registrar como você está ajuda a perceber padrões e acompanhar sua saúde ao longo do tempo.</p>
                  <button
                    className="primary-btn"
                    onClick={() => openAction({ kind: 'form', title: 'Registrar como estou', endpoint: '/symptoms' })}
                  >
                    Registrar como estou <Icon name="arrow" size={16} />
                  </button>
                </div>
              </section>
              <section className="section-heading">
                <div>
                  <h2>Seu dia em foco</h2>
                  <p>Um panorama simples para você se cuidar melhor.</p>
                </div>
                <button
                  className="text-btn"
                  onClick={() => {
                    setDashboardCalendarExpanded(true)
                    window.setTimeout(
                      () =>
                        document
                          .querySelector('.calendar-card')
                          ?.scrollIntoView({ behavior: 'smooth', block: 'center' }),
                      0,
                    )
                  }}
                >
                  Ver histórico <Icon name="arrow" size={15} />
                </button>
              </section>
              <DashboardSummary
                profile={activeProfile}
                onAction={openAction}
                onNavigate={setActive}
                selectedDate={selectedDate}
                onDateSelect={setSelectedDate}
                calendarExpanded={dashboardCalendarExpanded}
                onCalendarExpandedChange={setDashboardCalendarExpanded}
                calendarRecords={calendarRecords}
                calendarLoadError={calendarLoadError}
              />
            </>
          ) : active === 'Configurações' ? (
            <SettingsPage
              profile={activeProfile}
              darkMode={darkMode}
              onToggleDarkMode={() => toggleProfilePreference('darkMode')}
              notificationsEnabled={notificationsEnabled}
              onToggleNotifications={() => toggleProfilePreference('notificationsEnabled')}
              weeklySummaryEnabled={weeklySummaryEnabled}
              onToggleWeeklySummary={() => toggleProfilePreference('weeklySummaryEnabled')}
              audioFeedbackEnabled={audioFeedbackEnabled}
              onToggleAudioFeedback={toggleAudioFeedback}
              pushEnabled={pushEnabled}
              pushConfigured={isPushConfigured() && !isDemoMode()}
              notificationPermission={notificationPermission}
              onTogglePush={toggleBackgroundPush}
              onEnableLocalNotifications={enableLocalNotifications}
              accessibility={accessibility}
              onToggleAccessibility={updateProfileAccessibility}
              onSaveProfileTheme={saveProfileTheme}
              aiSettings={activeProfile?.preferences?.ai}
              onSaveAiSettings={saveAiSettings}
              onExportBackup={exportBackup}
              onImportBackup={importBackup}
              onClearLocalData={clearLocalData}
              onSyncBackup={syncBackup}
              isGuest={guestMode}
              isDemo={isDemoMode()}
              onRegister={() => setAuthView('register')}
              backupBusy={backupBusy}
              backupStatus={backupStatus}
            />
          ) : (
            <ModulePage
              active={active}
              onAction={openAction}
              darkMode={darkMode}
              onToggleDarkMode={() => toggleProfilePreference('darkMode')}
              profile={activeProfile}
              onPregnancyChange={savePregnancy}
              selectedDate={selectedDate}
              onDateSelect={setSelectedDate}
              calendarRecords={calendarRecords}
              calendarLoadError={calendarLoadError}
              isGuest={guestMode}
              usePushAlarms={!guestMode && !isDemoMode() && isPushConfigured()}
              onExportBackup={exportBackup}
              onImportBackup={importBackup}
              onSyncBackup={syncBackup}
              backupBusy={backupBusy}
              backupStatus={backupStatus}
            />
          )}
        </div>
      </main>
      {modal && (
        <ActionModal
          action={modal}
          connection={connection}
          selectedDate={selectedDate}
          pushEnabled={pushEnabled}
          onClose={() => setModal(null)}
          onSubmit={submitAction}
        />
      )}
      {feedbackMessage && (
        <div className={`action-feedback${globalActionError ? ' raised' : ''}`} role="status" aria-live="polite">
          <span>{feedbackMessage}</span>
          {undoInfo && (
            <button type="button" className="undo-action" disabled={undoInProgress} onClick={undoLastAction}>
              {undoInProgress ? 'Desfazendo…' : 'Desfazer registro (Ctrl+Z)'}
            </button>
          )}
          <button
            type="button"
            className="dismiss-feedback"
            aria-label="Fechar aviso"
            onClick={() => setFeedbackMessage('')}
          >
            ×
          </button>
        </div>
      )}
      {globalActionError && (
        <div className="submission-error-toast" role="alert">
          {globalActionError}
        </div>
      )}
    </div>
  )
}
