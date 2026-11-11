import { useEffect, useRef, useState } from 'react'
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
} from '../../services/push'
import { defaultProfilePreferences, profileThemeOptions, getProfileTheme } from './dadosAplicacao'
import { Icon } from './interfaceCompartilhada'
import { AiSettings } from './recursos'
import { HealthTagsInput } from './autenticacaoEPainel'

export function SettingsPage({
  profile,
  darkMode,
  onToggleDarkMode,
  notificationsEnabled,
  onToggleNotifications,
  weeklySummaryEnabled,
  onToggleWeeklySummary,
  audioFeedbackEnabled,
  onToggleAudioFeedback,
  pushEnabled,
  pushConfigured,
  notificationPermission,
  onTogglePush,
  onEnableLocalNotifications,
  accessibility,
  onToggleAccessibility,
  onExportBackup,
  onImportBackup,
  onClearLocalData,
  onSyncBackup,
  onSaveProfileTheme,
  aiSettings,
  onSaveAiSettings,
  isGuest = false,
  isDemo = false,
  onRegister,
  backupBusy = false,
  backupStatus = '',
}) {
  const [tab, setTab] = useState('general')
  const [colorOpen, setColorOpen] = useState(false)
  const localReminderMode = isGuest || isDemo || !pushConfigured || !pushEnabled
  const tabs = [
    ['general', 'Preferências gerais'],
    ...(!isGuest ? [['ai', 'Inteligência artificial']] : []),
    ['accessibility', 'Acessibilidade'],
    ['privacy', 'Privacidade e segurança'],
    ['backup', 'Backup local'],
  ]
  const toggle = (label, enabled, onChange) => (
    <button className={enabled ? 'toggle on' : 'toggle'} aria-label={label} aria-pressed={enabled} onClick={onChange}>
      <i></i>
    </button>
  )
  const textScale = Number(accessibility.textScale || (accessibility.largeText ? 125 : 100))
  return (
    <div className="settings-grid">
      <div className="panel settings-nav">
        {tabs.map(([value, label]) => (
          <button
            key={value}
            className={tab === value ? 'settings-tab active' : 'settings-tab'}
            onClick={() => setTab(value)}
          >
            {label}
          </button>
        ))}
      </div>
      <div className="panel settings-content">
        {tab === 'general' && (
          <>
            <h3>Preferências gerais</h3>
            <p>
              Escolha como o Katu’I funciona {isGuest ? 'neste dispositivo' : `para ${profile?.name || 'este perfil'}`}.
            </p>
            {isGuest && (
              <div className="guest-settings-notice">
                <strong>Modo Guest</strong>
                <span>
                  Seus registros ficam somente neste navegador. Crie uma conta para ativar backup e sincronização.
                </span>
                <button className="soft-btn" onClick={onRegister}>
                  Registre-se para fazer backup <Icon name="arrow" size={14} />
                </button>
              </div>
            )}
            <div className="setting-row">
              <div>
                <strong>Notificações de medicamentos</strong>
                <span>Receba lembretes no horário certo para este perfil.</span>
              </div>
              {toggle('Alternar notificações de medicamentos', notificationsEnabled, onToggleNotifications)}
            </div>
            {notificationPermission !== 'granted' && (
              <div className="notification-permission-notice" role="status" aria-live="polite">
                <strong>
                  {notificationPermission === 'default'
                    ? 'Autorize as notificações'
                    : notificationPermission === 'denied'
                      ? 'Notificações bloqueadas'
                      : 'Notificações indisponíveis'}
                </strong>
                <span>
                  {notificationPermission === 'default'
                    ? 'Permita que o navegador mostre os lembretes do Katu’I neste dispositivo.'
                    : notificationPermission === 'denied'
                      ? 'A permissão foi negada. Libere as notificações deste site nas configurações do navegador.'
                      : 'Este navegador não oferece suporte a notificações.'}
                </span>
                {notificationPermission === 'default' && (
                  <button className="soft-btn" onClick={onEnableLocalNotifications}>
                    Autorizar notificações
                  </button>
                )}
              </div>
            )}
            <div className="setting-row">
              <div>
                <strong>Resumo semanal</strong>
                <span>Receba um panorama dos registros deste perfil.</span>
              </div>
              {toggle('Alternar resumo semanal', weeklySummaryEnabled, onToggleWeeklySummary)}
            </div>
            <div className="setting-row">
              <div>
                <strong>Modo escuro</strong>
                <span>{darkMode ? 'Ativado para este perfil.' : 'Use uma aparência mais confortável à noite.'}</span>
              </div>
              {toggle('Alternar modo escuro', darkMode, onToggleDarkMode)}
            </div>
            <div className="setting-row">
              <div>
                <strong>Feedback sonoro</strong>
                <span>Toques breves ao salvar, desfazer ou encontrar um erro. Desativado por padrão.</span>
              </div>
              {toggle('Alternar feedback sonoro', audioFeedbackEnabled, onToggleAudioFeedback)}
            </div>
            {!isGuest && (
              <div className="setting-row profile-theme-setting">
                <div>
                  <strong>Cor do perfil</strong>
                  <span>
                    Personalize os destaques de {profile?.name || 'este perfil'} sem alterar os outros perfis.
                  </span>
                </div>
                <button className="soft-btn" onClick={() => setColorOpen(true)}>
                  Escolher cor
                </button>
              </div>
            )}
            {colorOpen && (
              <ProfileColorModal
                profile={profile}
                onClose={() => setColorOpen(false)}
                onSave={(themeId) => {
                  setColorOpen(false)
                  onSaveProfileTheme(themeId)
                }}
              />
            )}
          </>
        )}
        {tab === 'general' && (
          <div className="setting-row">
            <div>
              <strong>{localReminderMode ? 'Lembretes locais' : 'Alarmes em segundo plano'}</strong>
              <span>
                {localReminderMode
                  ? `Os alarmes ${isGuest ? 'Guest' : isDemo ? 'da demonstração' : 'deste perfil'} avisam neste dispositivo enquanto o Katu’I estiver aberto.`
                  : pushConfigured
                    ? 'Receba lembretes de medicamentos mesmo quando o site estiver fechado.'
                    : 'O serviço de notificações do servidor ainda precisa ser configurado.'}
              </span>
            </div>
            {localReminderMode ? (
              notificationPermission === 'granted' ? (
                <span className="security-status">Permissão ativa · com o app aberto</span>
              ) : notificationPermission === 'default' ? (
                <span className="security-status">Autorização pendente</span>
              ) : (
                <span className="security-status">
                  {notificationPermission === 'unsupported' ? 'Notificações indisponíveis neste navegador.' : 'Permita notificações nas configurações do navegador.'}
                </span>
              )
            ) : pushConfigured ? (
              toggle('Alternar alarmes em segundo plano', pushEnabled, onTogglePush)
            ) : (
              <span className="security-status">Não configurado</span>
            )}
          </div>
        )}
        {tab === 'ai' && (
          <>
            <h3>Inteligência artificial</h3>
            <p>Escolha qual serviço será usado para organizar receitas e outros documentos deste perfil.</p>
            <AiSettings configuracoes={aiSettings} onSave={onSaveAiSettings} />
          </>
        )}
        {tab === 'accessibility' && (
          <>
            <h3>Acessibilidade</h3>
            <p>Adapte a experiência para ficar mais confortável e fácil de usar.</p>
            <div className="setting-row accessibility-size-row">
              <div>
                <strong>Tamanho do texto</strong>
                <span>
                  Escolha o tamanho que fica mais confortável para você. O layout se reorganiza automaticamente.
                </span>
              </div>
              <select
                className="accessibility-size-select"
                aria-label="Escolher tamanho do texto"
                value={textScale}
                onChange={(event) => onToggleAccessibility('textScale', Number(event.target.value))}
              >
                {[100, 125, 150, 175, 200].map((size) => (
                  <option key={size} value={size}>
                    {size}%
                  </option>
                ))}
              </select>
            </div>
            <div className="setting-row">
              <div>
                <strong>Fonte para dislexia</strong>
                <span>Usa uma fonte com formas mais fáceis de diferenciar.</span>
              </div>
              {toggle('Alternar fonte para dislexia', accessibility.dyslexiaFont, () =>
                onToggleAccessibility('dyslexiaFont'),
              )}
            </div>
            <div className="setting-row">
              <div>
                <strong>Alto contraste</strong>
                <span>Reforça a diferença entre texto, fundo, controles e foco do teclado.</span>
              </div>
              {toggle('Alternar alto contraste', accessibility.highContrast, () =>
                onToggleAccessibility('highContrast'),
              )}
            </div>
            <div className="setting-row">
              <div>
                <strong>Reduzir animações</strong>
                <span>Evita movimentos desnecessários na interface.</span>
              </div>
              {toggle('Alternar redução de animações', accessibility.reduceMotion, () =>
                onToggleAccessibility('reduceMotion'),
              )}
            </div>
          </>
        )}
        {tab === 'privacy' && (
          <>
            <h3>Privacidade e segurança</h3>
            <p>Controle o que permanece salvo neste dispositivo.</p>
            <div className="privacy-notice">
              <strong>Seus dados são seus.</strong>
              <span>
                O Katu’I não inclui tokens de acesso nos arquivos de backup e só envia dados ao servidor quando a
                sincronização estiver disponível.
              </span>
            </div>
            <div className="setting-row">
              <div>
                <strong>Sessão atual</strong>
                <span>O acesso do servidor é enviado somente em chamadas autenticadas.</span>
              </div>
              <span className="security-status">Protegida</span>
            </div>
            <div className="setting-row">
              <div>
                <strong>Dados locais</strong>
                <span>Remove rascunhos, perfis, preferências e ações pendentes deste dispositivo.</span>
              </div>
              <button className="danger-btn small-danger" onClick={onClearLocalData}>
                Apagar dados locais
              </button>
            </div>
          </>
        )}
        {tab === 'backup' && (
          <>
            <h3>Backup pessoal</h3>
            <p>
              Exporte e restaure registros de saúde. A importação em uma conta mescla os dados sem apagar os existentes.
            </p>
            <div className="backup-actions">
              <button className="primary-btn" disabled={backupBusy} onClick={onExportBackup}>
                <Icon name="arrow" size={16} /> {backupBusy ? 'Processando…' : 'Exportar backup'}
              </button>
              <label className={`soft-btn import-btn${backupBusy ? ' disabled' : ''}`}>
                {' '}
                <Icon name="file" size={15} /> Importar backup
                <input
                  className="file-input"
                  type="file"
                  accept="application/json,.json"
                  disabled={backupBusy}
                  onChange={(event) => {
                    const file = event.target.files?.[0]
                    if (file) onImportBackup(file)
                    event.target.value = ''
                  }}
                />
              </label>
            </div>
            <div className="privacy-notice">
              <strong>Conteúdo e privacidade</strong>
              <span>
                O arquivo inclui perfis, preferências e registros de saúde. Tokens de sessão e chaves de autenticação
                nunca são exportados. Guest substitui os registros locais ao importar; conta autenticada mescla
                registros e exige conexão.
              </span>
            </div>
            {backupStatus && (
              <p className="backup-status" role="status" aria-live="polite">
                {backupStatus}
              </p>
            )}
            {!isGuest && (
              <button className="outline-btn backup-sync-btn" onClick={onSyncBackup}>
                Sincronizar backup quando possível <Icon name="arrow" size={14} />
              </button>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export function BackupPage({ onExportBackup, onImportBackup, onSyncBackup, backupBusy = false, backupStatus = '' }) {
  return (
    <section className="panel backup-page">
      <div className="panel-header">
        <div>
          <h3>Backup pessoal</h3>
          <p>Exporte ou restaure os registros e perfis deste espaço.</p>
        </div>
        <Icon name="file" size={22} />
      </div>
      <p>
        O arquivo inclui perfis, preferências e registros de saúde. Tokens de sessão e chaves de autenticação não são
        exportados.
      </p>
      <div className="backup-actions">
        <button className="primary-btn" disabled={backupBusy} onClick={onExportBackup}>
          <Icon name="arrow" size={16} />
          {backupBusy ? 'Processando…' : 'Exportar backup'}
        </button>
        <label className={`soft-btn import-btn${backupBusy ? ' disabled' : ''}`}>
          <Icon name="file" size={15} /> Importar backup
          <input
            className="file-input"
            type="file"
            accept="application/json,.json"
            disabled={backupBusy}
            onChange={(event) => {
              const file = event.target.files?.[0]
              if (file) onImportBackup(file)
              event.target.value = ''
            }}
          />
        </label>
        <button className="outline-btn" disabled={backupBusy} onClick={onSyncBackup}>
          <Icon name="cloud" size={16} /> Sincronizar com a conta
        </button>
      </div>
      {backupStatus && (
        <p className="backup-status" role="status" aria-live="polite">
          {backupStatus}
        </p>
      )}
    </section>
  )
}

export function DependentModal({ action, onClose, onSubmit, connection, submissionError, submitting }) {
  const isEditing = /^\/profiles\/[^/]+$/.test(action.endpoint || '')
  const steps = ['Nome', 'Vínculo', 'Idade', 'Sexo', 'Peso', 'Altura', 'Alergias', 'Condições', 'Revisão']
  const toForm = (profile = {}) => ({
    name: profile.name || '',
    relation: profile.relation || '',
    age: profile.age ?? '',
    sex: profile.sex || 'nao-informado',
    weight: profile.weight ?? '',
    height: profile.heightCm ?? profile.height ?? '',
    allergies: profile.allergies || '',
    conditions: profile.specialConditions || profile.conditions || '',
  })
  const initialForm = toForm(action.data)
  const [step, setStep] = useState(0)
  const [form, setForm] = useState(initialForm)
  const initialFormSnapshot = useRef(JSON.stringify(initialForm))
  useEffect(() => {
    const nextForm = toForm(action.data)
    initialFormSnapshot.current = JSON.stringify(nextForm)
    setForm(nextForm)
    setStep(0)
  }, [action])
  const close = () => {
    if (
      JSON.stringify(form) !== initialFormSnapshot.current &&
      !window.confirm('Há alterações não salvas. Deseja fechar e descartá-las?')
    )
      return
    onClose()
  }
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value })
  const validation =
    step === 0 && form.name.trim().length < 2
      ? 'Informe pelo menos 2 caracteres.'
      : step === 2 && form.age && (Number(form.age) < 0 || Number(form.age) > 120)
        ? 'Informe uma idade entre 0 e 120 anos.'
        : step === 4 && form.weight && (Number(form.weight) <= 0 || Number(form.weight) > 500)
          ? 'Informe um peso válido.'
          : step === 5 && form.height && (Number(form.height) < 30 || Number(form.height) > 250)
            ? 'Informe uma altura entre 30 e 250 cm.'
            : ''
  const canContinue = !validation && (step !== 0 || form.name.trim().length > 1)
  const finish = () =>
    onSubmit({
      ...action,
      data: {
        name: form.name.trim(),
        relation: form.relation || 'Dependente',
        age: form.age ? Number(form.age) : null,
        sex: form.sex,
        weight: form.weight ? Number(form.weight) : null,
        heightCm: form.height ? Number(form.height) : null,
        allergies: form.allergies.trim() || null,
        specialConditions: form.conditions.trim() || null,
        ...(!isEditing
          ? {
              themeColor: 'turquoise',
              corHex: profileThemeOptions[0].color,
              preferences: { ...defaultProfilePreferences },
            }
          : {}),
      },
    })
  const next = () => canContinue && (step === steps.length - 1 ? finish() : setStep(step + 1))
  if (step === 6 || step === 7)
    return (
      <DependentHealthStep
        step={step}
        steps={steps}
        form={form}
        setForm={setForm}
        onClose={close}
        onBack={() => setStep(step - 1)}
        onNext={() => setStep(step + 1)}
        connection={connection}
      />
    )
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && close()}>
      <div className="modal-card dependent-modal">
        <button className="modal-close" aria-label="Fechar" onClick={close}>
          ×
        </button>
        <div className="modal-icon">
          <Icon name="profile" size={22} />
        </div>
        <p className="eyebrow">{isEditing ? 'EDITAR PERFIL DE CUIDADO' : 'NOVO PERFIL DE CUIDADO'}</p>
        <h2>
          {step === steps.length - 1
            ? 'Confira os dados'
            : isEditing
              ? 'Atualize as informações'
              : 'Vamos conhecer essa pessoa'}
        </h2>
        <p className="modal-subtitle">
          Etapa {step + 1} de {steps.length}. Você poderá alterar essas informações depois.
        </p>
        <div className="dependent-progress">
          {steps.map((label, index) => (
            <i key={label} className={index < step ? 'done' : index === step ? 'current' : ''} />
          ))}
        </div>
        {step === 0 && (
          <div className="dependent-step">
            <strong>Como podemos chamar?</strong>
            <p>Use o nome da pessoa que você vai acompanhar.</p>
            <input autoFocus value={form.name} onChange={update('name')} placeholder="Digite o nome" />
          </div>
        )}
        {step === 1 && (
          <div className="dependent-step">
            <strong>Qual é o vínculo?</strong>
            <p>Isso ajuda a organizar seus perfis.</p>
            <div className="dependent-choices">
              {['Filho(a)', 'Pai ou mãe', 'Avô ou avó', 'Cônjuge', 'Outro'].map((relation) => (
                <button
                  key={relation}
                  className={form.relation === relation ? 'selected' : ''}
                  onClick={() => setForm({ ...form, relation })}
                >
                  {relation}
                </button>
              ))}
            </div>
          </div>
        )}
        {step === 2 && (
          <div className="dependent-step">
            <strong>Qual a idade?</strong>
            <p>Essa informação ajuda a personalizar alguns cuidados.</p>
            <div className="unit-input">
              <input
                autoFocus
                type="number"
                min="0"
                max="120"
                value={form.age}
                onChange={update('age')}
                placeholder="00"
              />
              <span>anos</span>
            </div>
          </div>
        )}
        {step === 3 && (
          <div className="dependent-step">
            <strong>Qual o sexo biológico?</strong>
            <p>Usado apenas para habilitar cuidados específicos, quando necessário.</p>
            <div className="dependent-choices">
              {[
                ['feminino', 'Feminino'],
                ['masculino', 'Masculino'],
                ['intersexo', 'Intersexo'],
                ['nao-informado', 'Prefiro não informar'],
              ].map(([value, label]) => (
                <button
                  key={value}
                  className={form.sex === value ? 'selected' : ''}
                  onClick={() => setForm({ ...form, sex: value })}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>
        )}
        {step === 4 && (
          <div className="dependent-step">
            <strong>Qual o peso?</strong>
            <p>Você poderá atualizar essa informação depois.</p>
            <div className="unit-input">
              <input
                autoFocus
                type="number"
                min="0"
                max="500"
                value={form.weight}
                onChange={update('weight')}
                placeholder="00"
              />
              <span>kg</span>
            </div>
          </div>
        )}
        {step === 5 && (
          <div className="dependent-step">
            <strong>Qual a altura?</strong>
            <p>Usaremos essa informação apenas para cálculos de saúde.</p>
            <div className="unit-input">
              <input
                autoFocus
                type="number"
                min="30"
                max="250"
                value={form.height}
                onChange={update('height')}
                placeholder="000"
              />
              <span>cm</span>
            </div>
          </div>
        )}
        {step === 6 && (
          <div className="dependent-step">
            <strong>Há alguma alergia?</strong>
            <p>Inclua medicamentos, alimentos ou outras alergias importantes.</p>
            <textarea
              autoFocus
              value={form.allergies}
              onChange={update('allergies')}
              placeholder="Digite aqui ou escreva ‘Nenhuma’"
            />
          </div>
        )}
        {step === 7 && (
          <div className="dependent-step">
            <strong>Há alguma condição especial?</strong>
            <p>Compartilhe algo importante para o cuidado dessa pessoa.</p>
            <textarea
              autoFocus
              value={form.conditions}
              onChange={update('conditions')}
              placeholder="Digite aqui ou escreva ‘Nenhuma’"
            />
          </div>
        )}
        {step === 8 && (
          <div className="dependent-summary">
            <div>
              <span>Nome</span>
              <strong>{form.name || 'Não informado'}</strong>
            </div>
            <div>
              <span>Vínculo</span>
              <strong>{form.relation || 'Dependente'}</strong>
            </div>
            <div>
              <span>Idade</span>
              <strong>{form.age ? `${form.age} anos` : 'Não informado'}</strong>
            </div>
            <div>
              <span>Sexo</span>
              <strong>{form.sex === 'nao-informado' ? 'Prefiro não informar' : form.sex}</strong>
            </div>
            <div>
              <span>Peso</span>
              <strong>{form.weight ? `${form.weight} kg` : 'Não informado'}</strong>
            </div>
            <div>
              <span>Altura</span>
              <strong>{form.height ? `${form.height} cm` : 'Não informado'}</strong>
            </div>
          </div>
        )}
        {validation && <small className="form-error">{validation}</small>}
        {submissionError && (
          <small className="form-error" role="alert">
            {submissionError}
          </small>
        )}
        <div className="modal-actions">
          <button className="cancel-btn" onClick={step ? () => setStep(step - 1) : close}>
            {step ? 'Voltar' : 'Cancelar'}
          </button>
          <button className="primary-btn" disabled={!canContinue || submitting} onClick={next}>
            {submitting
              ? 'Salvando...'
              : step === steps.length - 1
                ? isEditing
                  ? 'Salvar alterações'
                  : 'Criar perfil'
                : 'Continuar'}{' '}
            <Icon name="arrow" size={15} />
          </button>
        </div>
        <small className="sync-note">
          {connection === 'online'
            ? 'Será sincronizado com o servidor.'
            : 'Salvo localmente e sincronizado quando o servidor conectar.'}
        </small>
      </div>
    </div>
  )
}

export function DependentHealthStep({ step, steps, form, setForm, onClose, onBack, onNext, connection }) {
  const isAllergy = step === 6
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className="modal-card dependent-modal">
        <button className="modal-close" onClick={onClose}>
          ×
        </button>
        <div className="modal-icon">
          <Icon name="heart" size={22} />
        </div>
        <p className="eyebrow">NOVO PERFIL DE CUIDADO</p>
        <h2>{isAllergy ? 'Você tem alguma alergia?' : 'Acompanhamento de saúde'}</h2>
        <p className="modal-subtitle">
          Etapa {step + 1} de {steps.length}. Selecione opções ou adicione outras manualmente.
        </p>
        <div className="dependent-progress">
          {steps.map((label, index) => (
            <i key={label} className={index < step ? 'done' : index === step ? 'current' : ''} />
          ))}
        </div>
        <div className="dependent-step dependent-health-step">
          <HealthTagsInput
            kind={isAllergy ? 'allergy' : 'condition'}
            value={isAllergy ? form.allergies : form.conditions}
            onChange={(value) => setForm({ ...form, [isAllergy ? 'allergies' : 'conditions']: value })}
          />
        </div>
        <div className="modal-actions">
          <button className="cancel-btn" onClick={onBack}>
            Voltar
          </button>
          <button className="primary-btn" onClick={onNext}>
            Continuar <Icon name="arrow" size={15} />
          </button>
        </div>
        <small className="sync-note">
          {connection === 'online'
            ? 'Será sincronizado com o servidor.'
            : 'Salvo localmente e sincronizado quando o servidor conectar.'}
        </small>
      </div>
    </div>
  )
}

export function ProfileColorModal({ profile, onClose, onSave }) {
  const [selected, setSelected] = useState(getProfileTheme(profile).id)
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className="modal-card profile-color-modal">
        <button className="modal-close" onClick={onClose}>
          ×
        </button>
        <div className="modal-icon">
          <Icon name="sun" size={22} />
        </div>
        <p className="eyebrow">PERSONALIZE SEU PERFIL</p>
        <h2>Escolha uma cor</h2>
        <p className="modal-subtitle">
          A cor aparece no menu e nos principais destaques enquanto você acompanha {profile?.name || 'este perfil'}.
        </p>
        <div className="color-choice-grid">
          {profileThemeOptions.map((option) => (
            <button
              type="button"
              key={option.id}
              className={selected === option.id ? 'color-choice selected' : 'color-choice'}
              onClick={() => setSelected(option.id)}
            >
              <span className="color-swatch" style={{ background: option.color }}></span>
              <strong>{option.label}</strong>
              {selected === option.id && <Icon name="check" size={14} />}
            </button>
          ))}
        </div>
        <div className="modal-actions">
          <button className="cancel-btn" onClick={onClose}>
            Cancelar
          </button>
          <button className="primary-btn" onClick={() => onSave(selected)}>
            Salvar cor
          </button>
        </div>
      </div>
    </div>
  )
}
