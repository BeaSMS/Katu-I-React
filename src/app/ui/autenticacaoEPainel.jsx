import { useEffect, useRef, useState } from 'react'
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
} from '../../services/api'
import { clearSecureStorage, flushSecureStorage, secureStorage } from '../../services/secureStorage'
import { medications, guestCollections } from './dadosAplicacao'
import { Icon, Brand, EmptyState, recordOccursOnDate } from './interfaceCompartilhada'
import { CalendarCard } from './modulosCalendario'
import { DemoAccessButton, DemoCredentials } from './authDemo'
import { demoCredentials } from '../../services/api/demoData'

export function ConnectionBadge({ status }) {
  const label =
    status === 'guest'
      ? 'Modo Guest — somente neste dispositivo'
      : status === 'demo'
        ? 'Demonstração local'
        : status === 'online'
          ? 'Servidor conectado'
          : status === 'checking'
            ? 'Verificando conexão'
            : 'Modo offline'
  return (
    <span className={`connection-badge ${status}`}>
      <i></i>
      {label}
    </span>
  )
}

export function SyncBadge({ connection, pendingCount, error = '' }) {
  const pending = pendingCount > 0
  const syncing = connection === 'checking'
  const label =
    error ||
    (connection === 'guest'
      ? 'Seus registros estão salvos somente neste dispositivo. Crie uma conta para ter backup.'
      : connection === 'demo'
        ? 'Dados fictícios salvos neste navegador'
        : syncing
          ? 'Verificando sincronização'
          : pending
            ? `${pendingCount} ${pendingCount === 1 ? 'dado pendente' : 'dados pendentes'} neste dispositivo`
            : 'Dados sincronizados')
  return (
    <span
      className={`sync-badge ${syncing ? 'checking' : pending ? 'pending' : 'synced'}`}
      title={label}
      aria-label={label}
    >
      <Icon name={connection === 'guest' ? 'file' : syncing || pending ? 'cloud' : 'check'} size={15} />
      {pending && <b>{pendingCount > 99 ? '99+' : pendingCount}</b>}
    </span>
  )
}

export function hasGuestRecords() {
  try {
    const data = JSON.parse(secureStorage.getItem('katui.guest-data') || '{}')
    return guestCollections.some((collection) => Array.isArray(data[collection]) && data[collection].length > 0)
  } catch {
    return false
  }
}

export function AuthPage({ view, onViewChange, onSubmit, onGuestLogin, loading, error }) {
  const [form, setForm] = useState({ name: '', email: '', password: '', passwordConfirmation: '' })
  const [migrateGuestData, setMigrateGuestData] = useState(false)
  const isLanding = view === 'landing'
  const isRegister = view === 'register'
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value })
  const submit = (event) => {
    event.preventDefault()
    onSubmit({ ...form, migrateGuestData }, view)
  }
  if (isLanding)
    return (
      <main className="auth-page">
        <div className="auth-brand">
          <Brand />
        </div>
        <div className="auth-layout">
          <section className="auth-hero">
            <p className="eyebrow">CUIDADO QUE ACOMPANHA VOCÊ</p>
            <h1>Um jeito mais simples de cuidar da sua saúde.</h1>
            <p>
              Organize medicamentos, sinais vitais, consultas e documentos em um espaço feito para você e para quem
              depende do seu cuidado.
            </p>
            <div className="auth-highlights">
              <span>
                <Icon name="check" size={15} /> Seus dados organizados
              </span>
              <span>
                <Icon name="cloud" size={15} /> Funciona mesmo sem internet
              </span>
              <span>
                <Icon name="heart" size={15} /> Um perfil para cada pessoa
              </span>
            </div>
          </section>
          <section className="auth-card">
            <div className="auth-mark">✦</div>
            <h2>Bem-vindo ao Katu’I</h2>
            <p>Entre na sua conta, crie um espaço de cuidado ou experimente sem cadastro.</p>
            <button className="primary-btn auth-main-button" onClick={() => onViewChange('login')}>
              Entrar na minha conta <Icon name="arrow" size={16} />
            </button>
            <DemoAccessButton
              onClick={() => {
                setForm({ ...form, email: demoCredentials.email, password: demoCredentials.password })
                onViewChange('login')
              }}
            />
            <button className="outline-btn auth-secondary-button" onClick={() => onViewChange('register')}>
              Criar minha conta
            </button>
            <button className="outline-btn guest-login-button" onClick={onGuestLogin}>
              Continuar como Guest
            </button>
            <small>
              No modo Guest, os registros ficam somente neste dispositivo e não há acesso à IA nem a backup na nuvem.
            </small>
          </section>
        </div>
      </main>
    )
  return (
    <main className="auth-page">
      <div className="auth-brand">
        <Brand />
      </div>
      <div className="auth-form-wrap">
        <section className="auth-card auth-form-card">
          <button className="auth-back" onClick={() => onViewChange('landing')}>
            ← Voltar
          </button>
          <div className="auth-mark small-mark">✦</div>
          <h2>{isRegister ? 'Criar sua conta' : 'Entrar no Katu’I'}</h2>
          <p>
            {isRegister
              ? 'Crie seu acesso para começar seu espaço de cuidado.'
              : 'Acesse seus perfis e registros de saúde.'}
          </p>
          {!isRegister && <DemoCredentials onFill={() => setForm({ ...form, email: demoCredentials.email, password: demoCredentials.password })} />}
          <form onSubmit={submit}>
            {isRegister && (
              <label>
                Nome completo
                <input
                  autoFocus
                  value={form.name}
                  onChange={update('name')}
                  placeholder="Como podemos chamar você?"
                  required
                  minLength={2}
                />
              </label>
            )}
            <label>
              E-mail
              <input
                autoFocus={!isRegister}
                type="email"
                value={form.email}
                onChange={update('email')}
                placeholder="voce@exemplo.com"
                required
                autoComplete={isRegister ? 'email' : 'username'}
              />
            </label>
            <label>
              Senha
              <input
                type="password"
                value={form.password}
                onChange={update('password')}
                placeholder="Mínimo de 8 caracteres"
                required
                minLength={8}
                autoComplete={isRegister ? 'new-password' : 'current-password'}
              />
            </label>
            {isRegister && (
              <label>
                Confirme sua senha
                <input
                  type="password"
                  value={form.passwordConfirmation}
                  onChange={update('passwordConfirmation')}
                  placeholder="Digite a senha novamente"
                  required
                  minLength={8}
                  autoComplete="new-password"
                />
              </label>
            )}
            {hasGuestRecords() && isGuestMode() && (
              <label className="check-field guest-migration-choice">
                <input
                  type="checkbox"
                  checked={migrateGuestData}
                  onChange={(event) => setMigrateGuestData(event.target.checked)}
                />
                Importar meus registros Guest para esta conta. Essa ação enviará seus dados de saúde ao servidor.
              </label>
            )}
            {error && <small className="auth-error">{error}</small>}
            <button className="primary-btn auth-submit" disabled={loading}>
              {loading ? 'Aguarde...' : isRegister ? 'Criar conta' : 'Entrar'}
            </button>
          </form>
          <button className="auth-switch" onClick={() => onViewChange(isRegister ? 'login' : 'register')}>
            {isRegister ? 'Já tenho uma conta' : 'Ainda não tenho uma conta'}
          </button>
        </section>
      </div>
    </main>
  )
}

export function Onboarding({ step, setStep, form, setForm, onFinish, onSkip, darkMode, error = '', isGuest = false }) {
  const steps = [
    'Boas-vindas',
    'Seu nome',
    'Sua idade',
    'Sexo biológico',
    'Seu peso',
    'Sua altura',
    'Alergias',
    'Condições especiais',
    'IA opcional',
    'Tudo pronto',
  ]
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value })
  const isLast = step === steps.length - 1
  const validationMessage =
    step === 1 && form.name.trim().length < 2
      ? 'Informe pelo menos 2 caracteres.'
      : step === 2 && form.age && (Number(form.age) < 0 || Number(form.age) > 120)
        ? 'Informe uma idade entre 0 e 120 anos.'
        : step === 4 && form.weight && (Number(form.weight) <= 0 || Number(form.weight) > 500)
          ? 'Informe um peso válido.'
          : step === 5 && form.height && (Number(form.height) < 30 || Number(form.height) > 250)
            ? 'Informe uma altura entre 30 e 250 cm.'
            : ''
  const canContinue = !validationMessage && (step !== 1 || form.name.trim().length > 1)
  const next = () => canContinue && (isLast ? onFinish() : setStep(step + 1))
  const back = () => setStep(Math.max(0, step - 1))
  return (
    <div className={darkMode ? 'onboarding dark-mode' : 'onboarding'}>
      <div className="onboarding-top">
        <div className="onboarding-brand">
          <Brand />
        </div>
        {step > 0 && step < steps.length - 1 && (
          <span className="onboarding-counter">
            {step} de {steps.length - 2}
          </span>
        )}
        {!isGuest && (
          <button className="onboarding-skip" onClick={onSkip}>
            Pular por agora
          </button>
        )}
      </div>
      <div className="onboarding-body">
        <div className="onboarding-progress">
          {steps.slice(1, -1).map((label, index) => (
            <i key={label} className={step > index + 1 ? 'done' : step === index + 1 ? 'current' : ''}></i>
          ))}
        </div>
        {step === 0 && (
          <div className="onboarding-content welcome-onboarding">
            <div className="onboarding-mark">✦</div>
            <p className="eyebrow">SEU ESPAÇO DE CUIDADO</p>
            <h1>Olá! Vamos cuidar da sua saúde juntos?</h1>
            <p>
              Para te ajudar melhor, precisamos conhecer um pouco sobre você. É rápido, seguro e você pode alterar tudo
              depois.
            </p>
            <button className="primary-btn onboarding-main-btn" onClick={() => setStep(1)}>
              Começar <Icon name="arrow" size={17} />
            </button>
          </div>
        )}
        {step === 1 && (
          <OnboardingField
            eyebrow="SOBRE VOCÊ"
            title="Como podemos te chamar?"
            description="Escreva seu nome para personalizar sua experiência."
            input={<input autoFocus value={form.name} onChange={update('name')} placeholder="Digite seu nome" />}
          />
        )}{' '}
        {step === 2 && (
          <OnboardingField
            eyebrow="SOBRE VOCÊ"
            title="Qual a sua idade?"
            description="Essa informação ajuda a personalizar alguns cuidados."
            input={
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
            }
          />
        )}{' '}
        {step === 3 && (
          <div className="onboarding-content">
            <p className="eyebrow">SOBRE VOCÊ</p>
            <h1>Qual seu sexo biológico?</h1>
            <p>Isso ajuda a habilitar funções específicas, como saúde feminina.</p>
            <div className="choice-grid">
              {[
                ['feminino', 'Feminino', '♀'],
                ['masculino', 'Masculino', '♂'],
                ['intersexo', 'Intersexo', '⚥'],
                ['nao-informado', 'Prefiro não informar', '—'],
              ].map(([value, label, sign]) => (
                <button
                  key={value}
                  className={form.sex === value ? 'choice-card selected' : 'choice-card'}
                  onClick={() => setForm({ ...form, sex: value })}
                >
                  <span>{sign}</span>
                  <strong>{label}</strong>
                </button>
              ))}
            </div>
          </div>
        )}{' '}
        {step === 4 && (
          <OnboardingField
            eyebrow="MEDIDAS DE SAÚDE"
            title="Qual o seu peso?"
            description="Você poderá atualizar essa informação sempre que quiser."
            input={
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
            }
          />
        )}{' '}
        {step === 5 && (
          <OnboardingField
            eyebrow="MEDIDAS DE SAÚDE"
            title="Qual a sua altura?"
            description="Usaremos essa informação apenas para cálculos de saúde."
            input={
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
            }
          />
        )}{' '}
        {step === 6 && (
          <OnboardingField
            eyebrow="SEU CUIDADO"
            title="Você tem alguma alergia?"
            description="Inclua medicamentos, alimentos ou outras alergias importantes. Você também pode deixar para depois."
            input={
              <textarea
                autoFocus
                value={form.allergies}
                onChange={update('allergies')}
                placeholder="Digite aqui ou escreva ‘Nenhuma’"
              />
            }
          />
        )}{' '}
        {step === 7 && (
          <OnboardingField
            eyebrow="SEU CUIDADO"
            title="Há alguma condição especial?"
            description="Compartilhe algo que devemos considerar para cuidar melhor de você."
            input={
              <textarea
                autoFocus
                value={form.conditions}
                onChange={update('conditions')}
                placeholder="Digite aqui ou escreva ‘Nenhuma’"
              />
            }
          />
        )}{' '}
        {step === 8 &&
          (isGuest ? (
            <div className="onboarding-content">
              <p className="eyebrow">MODO GUEST</p>
              <h1>IA indisponível sem uma conta</h1>
              <p>
                Seus registros permanecem neste dispositivo. Crie uma conta para acessar os recursos de inteligência
                artificial.
              </p>
            </div>
          ) : (
            <div className="onboarding-content">
              <p className="eyebrow">UM RECURSO OPCIONAL</p>
              <h1>Quer usar a IA do Katu’I?</h1>
              <p>
                Ela pode ajudar a organizar receitas e documentos. Você poderá ativá-la ou desativá-la quando quiser.
              </p>
              <button
                className={form.aiEnabled ? 'choice-card wide selected' : 'choice-card wide'}
                onClick={() => setForm({ ...form, aiEnabled: true })}
              >
                <span>✦</span>
                <strong>Sim, quero experimentar</strong>
                <small>Leitura inteligente de receitas</small>
              </button>
              <button
                className={!form.aiEnabled ? 'choice-card wide selected' : 'choice-card wide'}
                onClick={() => setForm({ ...form, aiEnabled: false })}
              >
                <span>○</span>
                <strong>Agora não</strong>
                <small>Você pode mudar essa escolha depois</small>
              </button>
            </div>
          ))}{' '}
        {step === 9 && (
          <div className="onboarding-content">
            <div className="onboarding-success">✓</div>
            <p className="eyebrow">TUDO PRONTO</p>
            <h1>Seu espaço está preparado.</h1>
            <p>Confira seus dados. Você poderá editar qualquer informação nas configurações.</p>
            <div className="onboarding-summary">
              <div>
                <span>Nome</span>
                <strong>{form.name || 'Não informado'}</strong>
              </div>
              <div>
                <span>Idade</span>
                <strong>{form.age ? `${form.age} anos` : 'Não informado'}</strong>
              </div>
              <div>
                <span>Sexo biológico</span>
                <strong>{form.sex === 'nao-informado' ? 'Prefiro não informar' : form.sex || 'Não informado'}</strong>
              </div>
              <div>
                <span>IA de receitas</span>
                <strong>{form.aiEnabled ? 'Ativada' : 'Desativada'}</strong>
              </div>
            </div>
          </div>
        )}
      </div>
      {validationMessage && <small className="onboarding-error">{validationMessage}</small>}
      {error && <small className="onboarding-error">{error}</small>}
      <div className="onboarding-footer">
        {step > 0 && (
          <button className="back-btn" onClick={back}>
            Voltar
          </button>
        )}
        {step > 0 && (
          <button className="primary-btn onboarding-next" disabled={!canContinue} onClick={next}>
            {isLast ? 'Entrar no Katu’I' : 'Continuar'} <Icon name="arrow" size={16} />
          </button>
        )}
      </div>
    </div>
  )
}

export function HealthTagsInput({ value, onChange, kind }) {
  const emptyLabel = kind === 'allergy' ? 'Nenhuma' : 'Nenhum'
  const suggestions =
    kind === 'allergy'
      ? ['Penicilina', 'Dipirona', 'AAS', 'Leite', 'Amendoim', 'Nenhuma']
      : ['Diabetes', 'Hipertensão', 'Asma', 'Cardiopatia', 'Epilepsia', 'Nenhum']
  const [draft, setDraft] = useState('')
  const tags = String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item && item !== emptyLabel)
  const setTags = (next) => onChange(next.length ? next.join(', ') : emptyLabel)
  const add = (item) => {
    const clean = item.trim()
    if (!clean) return
    if (clean === emptyLabel) return setTags([])
    if (!tags.includes(clean)) setTags([...tags, clean])
    setDraft('')
  }
  return (
    <div className="health-tags-input">
      <div className="health-tags-list">
        {tags.map((tag) => (
          <button
            type="button"
            key={tag}
            className="health-tag"
            onClick={() => setTags(tags.filter((item) => item !== tag))}
          >
            {tag}
            <span>×</span>
          </button>
        ))}
        {!tags.length && <span className="health-tags-empty">Nenhum item selecionado</span>}
      </div>
      <div className="health-tags-entry">
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ',') {
              event.preventDefault()
              add(draft)
            }
          }}
          placeholder="Digite e pressione Enter"
        />
        <button type="button" onClick={() => add(draft)}>
          Adicionar
        </button>
      </div>
      <div className="health-suggestions">
        {suggestions.map((suggestion) => (
          <button
            type="button"
            key={suggestion}
            className={tags.includes(suggestion) ? 'selected' : ''}
            onClick={() => (suggestion === emptyLabel ? setTags([]) : add(suggestion))}
          >
            {suggestion}
          </button>
        ))}
      </div>
      <small>Você pode adicionar mais de uma opção e tocar em uma tag para removê-la.</small>
    </div>
  )
}

export function OnboardingField({ eyebrow, title, description, input }) {
  const healthKind =
    title === 'Você tem alguma alergia?' ? 'allergy' : title === 'Há alguma condição especial?' ? 'condition' : null
  const healthInput = healthKind ? (
    <HealthTagsInput
      kind={healthKind}
      value={input?.props?.value || ''}
      onChange={(value) => input?.props?.onChange?.({ target: { value } })}
    />
  ) : (
    input
  )
  return (
    <div className="onboarding-content">
      <p className="eyebrow">{eyebrow}</p>
      <h1>{title}</h1>
      <p>{description}</p>
      <div className="onboarding-input">{healthInput}</div>
    </div>
  )
}

export function DashboardSummary({
  profile,
  onAction,
  onNavigate,
  selectedDate,
  onDateSelect,
  calendarExpanded,
  onCalendarExpandedChange,
  calendarRecords,
  calendarLoadError,
}) {
  const [overview, setOverview] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const calendarRef = useRef(null)
  useEffect(() => {
    let mounted = true
    setLoadError('')
    apiRequest('/overview')
      .then((result) => {
        if (!result || typeof result !== 'object' || Array.isArray(result))
          throw new Error('A API retornou um resumo inválido.')
        if (mounted)
          setOverview({
            medications: Array.isArray(result.medications) ? result.medications : [],
            vitalSigns: Array.isArray(result.vitalSigns) ? result.vitalSigns : [],
            appointments: Array.isArray(result.appointments) ? result.appointments : [],
            symptoms: Array.isArray(result.symptoms) ? result.symptoms : [],
          })
      })
      .catch((error) => {
        if (mounted) setLoadError(error.message || 'Não foi possível carregar o resumo.')
      })
    const refresh = (event) => {
      const endpoint = event.detail?.endpoint || ''
      if (
        ['/overview', '/medications', '/vital-signs', '/appointments', '/symptoms'].some(
          (resource) => endpoint === resource || endpoint.startsWith(`${resource}/`),
        )
      ) {
        setRefreshKey((value) => value + 1)
      }
    }
    window.addEventListener('katui:resource-change', refresh)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', refresh)
    }
  }, [profile?.id, refreshKey])
  const medications = overview?.medications || []
  const vitalSigns = overview?.vitalSigns || []
  const appointments = overview?.appointments || []
  const symptoms = overview?.symptoms || []
  const medicationsForDate = medications.filter((item) => recordOccursOnDate(item, selectedDate, 'medications'))
  const vitalSignsForDate = vitalSigns.filter((item) => recordOccursOnDate(item, selectedDate, 'vitalSigns'))
  const appointmentsForDate = appointments.filter((item) => recordOccursOnDate(item, selectedDate, 'appointments'))
  const symptomsForDate = symptoms.filter((item) => recordOccursOnDate(item, selectedDate, 'symptoms'))
  const appointmentForDate = appointmentsForDate.sort((left, right) =>
    `${left.date} ${left.time || ''}`.localeCompare(`${right.date} ${right.time || ''}`),
  )[0]
  const medication = medicationsForDate[0]
  const formattedDate = (value) => {
    if (!value) return ''
    const date = new Date(`${value.slice(0, 10)}T00:00:00`)
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('pt-BR')
  }
  const count = (items) => (overview ? items.length : '—')
  return (
    <>
      {loadError && (
        <p className="form-error" role="status">
          {loadError}
        </p>
      )}
      <CalendarCard
        selectedDate={selectedDate}
        onDateSelect={onDateSelect}
        records={calendarRecords}
        expanded={calendarExpanded}
        onExpandedChange={onCalendarExpandedChange}
        cardRef={calendarRef}
        loadError={calendarLoadError}
      />
      <section className="metrics-grid">
        <article className="metric-card">
          <div className="metric-icon teal-bg">
            <Icon name="pill" size={20} />
          </div>
          <div>
            <span>Medicamentos</span>
            <strong>
              {count(medicationsForDate)} <small>cadastrados</small>
            </strong>
            <button className="mini-link" onClick={() => onNavigate('Medicamentos')}>
              Ver medicamentos <Icon name="arrow" size={13} />
            </button>
          </div>
          <span className="metric-arrow">
            <Icon name="chevron" size={17} />
          </span>
        </article>
        <article className="metric-card">
          <div className="metric-icon rose-bg">
            <Icon name="pulse" size={20} />
          </div>
          <div>
            <span>Sinais vitais</span>
            <strong>{overview ? vitalSignsForDate.length || 'Nenhum registro' : '—'}</strong>
            <button className="mini-link" onClick={() => onNavigate('Sinais vitais')}>
              Ver registros <Icon name="arrow" size={13} />
            </button>
          </div>
          <span className="metric-arrow">
            <Icon name="chevron" size={17} />
          </span>
        </article>
        <article className="metric-card">
          <div className="metric-icon blue-bg">
            <Icon name="calendar" size={20} />
          </div>
          <div>
            <span>Consulta nesta data</span>
            <strong>{appointmentForDate?.name || (overview ? 'Nenhuma agendada' : '—')}</strong>
            <button className="mini-link" onClick={() => onNavigate('Consultas')}>
              {appointmentForDate ? formattedDate(appointmentForDate.date) : 'Ver consultas'}{' '}
              <Icon name="arrow" size={13} />
            </button>
          </div>
          <span className="metric-arrow">
            <Icon name="chevron" size={17} />
          </span>
        </article>
      </section>
      <section className="dashboard-grid">
        <div className="panel medication-panel">
          <div className="panel-header">
            <div>
              <h3>Próximos medicamentos</h3>
              <p>{overview ? 'Tratamentos deste perfil' : 'Carregando seus dados...'}</p>
            </div>
          </div>
          {medication ? (
            <div className="resource-row">
              <div className="resource-icon">
                <Icon name="pill" size={18} />
              </div>
              <div className="resource-main">
                <strong>{medication.name || 'Medicamento'}</strong>
                <span>
                  {medication.time
                    ? `Próximo horário: ${medication.time}`
                    : medication.dosage || medication.dose || 'Tratamento ativo'}
                </span>
              </div>
            </div>
          ) : overview ? (
            <EmptyState
              icon="pill"
              title="Nenhum medicamento nesta data"
              message="Escolha outro dia ou adicione um medicamento."
              action="Adicionar medicamento"
              onAction={() => onNavigate('Medicamentos')}
            />
          ) : null}
        </div>
        <div className="panel week-panel">
          <div className="panel-header">
            <div>
              <h3>Seu bem-estar</h3>
              <p>Registros de sintomas nesta data</p>
            </div>
          </div>
          {overview && symptomsForDate.length ? (
            <div className="metric-card">
              <div>
                <span>Registros de sintomas</span>
                <strong>{symptomsForDate.length}</strong>
                <button
                  className="mini-link"
                  onClick={() => onAction({ kind: 'form', title: 'Registrar como estou', endpoint: '/symptoms' })}
                >
                  Novo registro <Icon name="arrow" size={13} />
                </button>
              </div>
            </div>
          ) : overview ? (
            <EmptyState
              icon="journal"
              title="Sem registros nesta data"
              message="Escolha outro dia para ver os registros."
              action="Fazer primeiro registro"
              onAction={() => onAction({ kind: 'form', title: 'Registrar como estou', endpoint: '/symptoms' })}
            />
          ) : null}
        </div>
      </section>
    </>
  )
}
