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
import { medications } from './dadosAplicacao'
import {
  Icon,
  EmptyState,
  recordDetailEntries,
  getAttachmentHref,
  recordDate,
  recordOccursOnDate,
} from './interfaceCompartilhada'
import { CalendarCard } from './modulosCalendario'

export function ResourcePage({
  active,
  icon,
  endpoint,
  resource,
  selectedDate,
  onDateSelect,
  calendarRecords,
  calendarLoadError,
  onAction,
  profileId,
  usePushAlarms = false,
}) {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [synced, setSynced] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [detailRecord, setDetailRecord] = useState(null)
  useEffect(() => {
    let mounted = true
    setLoading(true)
    setLoadError('')
    const recordsRequest = usePushAlarms
      ? listBackgroundAlarms({
          token: localStorage.getItem('katui.auth-token'),
          profileId: localStorage.getItem('katui.active-profile-id'),
        })
      : resourceApi.list(endpoint || '/records')
    recordsRequest
      .then((data) => {
        if (!mounted) return
        if (!Array.isArray(data)) throw new Error('A API retornou uma lista inválida.')
        setRecords(data)
        setSynced(true)
      })
      .catch((error) => {
        if (mounted) setLoadError(error.message || 'Não foi possível carregar os registros.')
      })
      .finally(() => mounted && setLoading(false))
    const handleResourceChange = (event) => {
      const changedEndpoint = event.detail?.endpoint || ''
      if (changedEndpoint === endpoint || changedEndpoint.startsWith(`${endpoint}/`))
        setRefreshKey((value) => value + 1)
    }
    window.addEventListener('katui:resource-change', handleResourceChange)
    return () => {
      mounted = false
      window.removeEventListener('katui:resource-change', handleResourceChange)
    }
  }, [endpoint, profileId, refreshKey, usePushAlarms])
  const visibleRecords =
    resource === 'alarms'
      ? records.filter((record) => record.status !== 'Concluído')
      : records.filter(
          (record) =>
            recordOccursOnDate(record, selectedDate, resource) || (!recordDate(record) && resource !== 'medications'),
        )
  const canAnalyzePrescriptions =
    resource === 'prescriptions' && !isGuestMode() && (isDemoMode() || isAiPrescriptionEnabled())
  const refresh = () => setRefreshKey((value) => value + 1)
  const analyzePrescription = (record) => {
    const id = record.id || record._id
    if (!id) return
    onAction({
      kind: 'ai',
      title: 'Ler receita com IA',
      endpoint: '/ai/prescription',
      data: {
        prescriptionId: id,
        attachmentName: record.attachment?.name || '',
        date: record.date || '',
        notes: record.notes || '',
      },
    })
  }
  const editRecord = (record) => {
    const id = record.id || record._id
    if (!id) return
    const isContraceptive =
      resource === 'medications' &&
      String(record.purpose || record.category || '')
        .toLowerCase()
        .includes('anticoncepcional')
    onAction({
      kind: 'form',
      title: isContraceptive ? 'Editar anticoncepcional' : `Editar ${active.toLowerCase()}`,
      endpoint: `${endpoint || '/records'}/${encodeURIComponent(id)}`,
      method: 'PATCH',
      data: record,
      ...(isContraceptive ? { contraceptiveSetup: true } : {}),
    })
  }
  const removeRecord = (record) => {
    const id = record.id || record._id
    if (!id) return
    const title = record.title || record.name || record.label || 'Registro'
    onAction({
      kind: 'confirm',
      method: 'DELETE',
      title: 'Remover registro?',
      message: `O registro “${title}” será removido.`,
      endpoint: `${endpoint || '/records'}/${encodeURIComponent(id)}`,
      data: record,
    })
  }
  return (
    <div className="resource-layout">
      <CalendarCard
        selectedDate={selectedDate}
        onDateSelect={onDateSelect}
        records={calendarRecords}
        resource={resource}
        loadError={calendarLoadError}
      />
      <div className="module-grid">
        <div className="panel wide-panel">
          <div className="panel-header">
            <div>
              <h3>Registros de {active.toLowerCase()}</h3>
              <p>
                {loading
                  ? 'Carregando seus registros...'
                  : loadError || (synced ? 'Dados atualizados' : 'Você ainda não adicionou nenhum registro.')}
              </p>
            </div>
            <button className="panel-link" onClick={refresh} disabled={loading}>
              Atualizar <Icon name="arrow" size={14} />
            </button>
          </div>
          {loadError && (
            <p className="form-error" role="alert">
              {loadError}
            </p>
          )}
          <div className="resource-list">
            {visibleRecords.length
              ? visibleRecords.map((item, index) => {
                  const id = item.id || item._id
                  const title = item.title || item.name || item.label || 'Registro'
                  const detail =
                    item.detail ||
                    item.description ||
                    item.date ||
                    (item.unknownDate ? 'Data não informada' : 'Sem detalhes adicionais')
                  const status = item.status
                  return (
                    <article className="resource-row" key={id || `${title}-${index}`}>
                      <div className="resource-icon">
                        <Icon name={icon} size={18} />
                      </div>
                      <div className="resource-main">
                        <strong>{title}</strong>
                        <span>{detail}</span>
                        {item.date && (
                          <small>
                            {item.date}
                            {item.time ? ` · ${item.time}` : ''}
                          </small>
                        )}
                        {item.attachment?.name && <small>{item.attachment.name}</small>}
                      </div>
                      {status && <span className="resource-status">{status}</span>}
                      {id && (
                        <div className="resource-actions">
                          <button type="button" className="row-action" onClick={() => setDetailRecord(item)}>
                            Detalhes
                          </button>
                          {resource === 'prescriptions' && item.attachment && canAnalyzePrescriptions && (
                            <button type="button" className="row-action" onClick={() => analyzePrescription(item)}>
                              Analisar com IA
                            </button>
                          )}
                          <button type="button" className="row-action" onClick={() => editRecord(item)}>
                            Editar
                          </button>
                          <button type="button" className="row-action" onClick={() => removeRecord(item)}>
                            Remover
                          </button>
                        </div>
                      )}
                    </article>
                  )
                })
              : !loading &&
                !loadError && (
                  <EmptyState
                    icon={icon}
                    title={`Nenhum registro em ${active.toLowerCase()} nesta data`}
                    message="Escolha outro dia no calendário ou adicione um novo registro."
                    action={`Adicionar em ${active}`}
                    onAction={() =>
                      onAction({ kind: 'form', title: `Adicionar em ${active}`, endpoint: endpoint || '/records' })
                    }
                  />
                )}
          </div>
        </div>
      </div>
      {detailRecord && (
        <div
          className="modal-backdrop"
          onMouseDown={(event) => event.target === event.currentTarget && setDetailRecord(null)}
        >
          <section
            className="modal-card record-detail-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="record-detail-title"
          >
            <button
              type="button"
              className="modal-close"
              aria-label="Fechar detalhes"
              onClick={() => setDetailRecord(null)}
            >
              ×
            </button>
            <h2 id="record-detail-title">{detailRecord.title || detailRecord.name || detailRecord.label || active}</h2>
            {getAttachmentHref(detailRecord) && (
              <a className="soft-btn" href={getAttachmentHref(detailRecord)} target="_blank" rel="noreferrer">
                Abrir anexo{detailRecord.attachment?.name ? `: ${detailRecord.attachment.name}` : ''}
              </a>
            )}
            {resource === 'prescriptions' && detailRecord.attachment && canAnalyzePrescriptions && (
              <button
                type="button"
                className="primary-btn"
                onClick={() => {
                  setDetailRecord(null)
                  analyzePrescription(detailRecord)
                }}
              >
                Analisar receita com IA
              </button>
            )}
            <dl>
              {recordDetailEntries(detailRecord).map(([key, value]) => (
                <div key={key}>
                  <dt>{key.replaceAll('_', ' ')}</dt>
                  <dd>
                    {Array.isArray(value)
                      ? value.join(', ')
                      : typeof value === 'object'
                        ? JSON.stringify(value)
                        : String(value)}
                  </dd>
                </div>
              ))}
            </dl>
            <div className="modal-actions">
              <button type="button" className="cancel-btn" onClick={() => setDetailRecord(null)}>
                Fechar
              </button>
              <button
                type="button"
                className="primary-btn"
                onClick={() => {
                  setDetailRecord(null)
                  editRecord(detailRecord)
                }}
              >
                Editar
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

export function EmptyModulePage({ active, icon, onAction }) {
  return (
    <div className="empty-module panel">
      <div className="empty-icon">
        <Icon name={icon} size={28} />
      </div>
      <h2>{active}</h2>
      <p>Esta área está pronta para receber seus registros. Comece adicionando uma nova informação de saúde.</p>
      <button
        className="primary-btn"
        onClick={() =>
          onAction({
            kind: 'form',
            title: `Adicionar em ${active}`,
            endpoint: `/${active.toLowerCase().replaceAll(' ', '-')}`,
          })
        }
      >
        <Icon name="plus" size={16} /> Adicionar registro
      </button>
    </div>
  )
}

export function AiSettings({ settings, onSave }) {
  const current = { provider: 'system', model: '', configured: false, maskedKey: '', ...(settings || {}) }
  const [provider, setProvider] = useState(current.provider)
  const [model, setModel] = useState(current.model || '')
  const [apiKey, setApiKey] = useState('')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const isCustom = provider !== 'system'
  const canSave = !isCustom || Boolean(apiKey.trim()) || current.configured
  const save = async () => {
    if (!canSave) return
    setSaving(true)
    setMessage('')
    try {
      await onSave({ provider, model: model.trim(), apiKey: apiKey.trim() })
      setApiKey('')
      setMessage('Preferência salva. A chave não fica guardada neste navegador.')
    } catch {
      setMessage('Não foi possível salvar agora. Tente novamente quando houver conexão.')
    } finally {
      setSaving(false)
    }
  }
  const remove = async () => {
    setProvider('system')
    setModel('')
    setApiKey('')
    setSaving(true)
    try {
      await onSave({ provider: 'system', model: '', apiKey: '' })
      setMessage('Chave removida. O Katu’I usará a IA padrão do sistema.')
    } catch {
      setMessage('Não foi possível remover a configuração agora.')
    } finally {
      setSaving(false)
    }
  }
  return (
    <div className="ai-settings">
      <div className="ai-settings-intro">
        <div className="ai-settings-icon">✦</div>
        <div>
          <strong>Escolha sua IA</strong>
          <span>Use a IA padrão do Katu’I ou conecte uma chave própria para este perfil.</span>
        </div>
      </div>
      <label>
        Provedor
        <select value={provider} onChange={(event) => setProvider(event.target.value)}>
          <option value="system">IA padrão do Katu’I</option>
          <option value="openai">OpenAI</option>
          <option value="google">Google Gemini</option>
          <option value="anthropic">Anthropic Claude</option>
        </select>
      </label>
      {isCustom && (
        <>
          <label>
            Modelo (opcional)
            <input
              value={model}
              onChange={(event) => setModel(event.target.value)}
              placeholder="Ex.: modelo recomendado pelo provedor"
            />
          </label>
          <label>
            Chave de API
            {current.configured && <small className="field-hint">Configurada: {current.maskedKey || '••••••••'}</small>}
            <input
              type="password"
              value={apiKey}
              onChange={(event) => setApiKey(event.target.value)}
              placeholder={current.configured ? 'Digite apenas para substituir' : 'Cole sua chave de API'}
              autoComplete="new-password"
            />
          </label>
          <div className="privacy-notice">
            <strong>A chave fica protegida</strong>
            <span>
              Ela é enviada somente ao servidor autenticado por HTTPS e não é salva no navegador, no backup ou no
              histórico.
            </span>
          </div>
        </>
      )}
      <div className="ai-settings-actions">
        <button className="primary-btn" disabled={!canSave || saving} onClick={save}>
          {saving ? 'Salvando...' : 'Salvar configuração'}
        </button>
        {current.configured && (
          <button className="cancel-btn" disabled={saving} onClick={remove}>
            Remover chave
          </button>
        )}
      </div>
      {message && <small className="form-message">{message}</small>}
    </div>
  )
}

export function HelpPage() {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(null)
  const faqs = [
    [
      'Primeiros passos',
      'Como começo a usar o Katu’I?',
      'Conclua o tutorial inicial ou pule para preencher os dados depois. Em seguida, escolha um perfil e use Adicionar registro para cadastrar medicamentos, consultas, sintomas e outros cuidados.',
    ],
    [
      'Dados e sincronização',
      'O que acontece quando estou sem internet?',
      'Os registros comuns ficam em uma fila local criptografada e serão enviados quando a conexão voltar. O indicador de nuvem no topo mostra quantos itens ainda aguardam sincronização.',
    ],
    [
      'Dados e sincronização',
      'Como sei se meus dados foram enviados?',
      'Quando não houver itens pendentes, o ícone de nuvem exibirá um check. A confirmação de um formulário offline significa que ele foi salvo neste dispositivo, não que já chegou ao servidor.',
    ],
    [
      'Privacidade',
      'Meus dados ficam seguros?',
      'O Katu’I usa autenticação e deve usar HTTPS ao conversar com o servidor. Os dados pendentes são protegidos no armazenamento local, mas mantenha seu dispositivo atualizado e protegido por senha.',
    ],
    [
      'Perfis',
      'Posso cuidar de mais de uma pessoa?',
      'Sim. Use o seletor de perfil no topo para alternar entre você e dependentes. Preferências como tema, acessibilidade e IA são independentes para cada perfil.',
    ],
    [
      'Inteligência artificial',
      'Qual IA posso usar?',
      'Você pode usar a IA padrão do Katu’I ou configurar um provedor próprio em Configurações > Inteligência artificial. A chave própria não é armazenada no navegador e exige conexão para ser salva.',
    ],
    [
      'Inteligência artificial',
      'A IA salva uma receita automaticamente?',
      'Não. A leitura gera uma sugestão revisável. Você precisa conferir os dados e confirmar antes que qualquer medicamento seja cadastrado.',
    ],
    [
      'Acessibilidade',
      'Como desfazer uma ação ou usar atalhos?',
      'Depois de criar ou remover um registro, use Ctrl+Z (ou Command+Z no Mac) para desfazer. O atalho não interfere na edição de texto: Ctrl+C e Ctrl+V continuam funcionando normalmente nos campos. Ative o feedback sonoro em Configurações > Preferências gerais se quiser ouvir confirmações.',
    ],
    [
      'Acessibilidade',
      'Como aumentar o tamanho das letras?',
      'Abra Configurações > Acessibilidade e escolha o tamanho desejado. O layout foi preparado para reorganizar menus e formulários em escalas maiores.',
    ],
  ]
  const filtered = faqs.filter(([category, question, answer]) =>
    (category + ' ' + question + ' ' + answer).toLowerCase().includes(query.toLowerCase()),
  )
  return (
    <div className="help-page">
      <section className="page-title-row">
        <div>
          <p className="eyebrow">CENTRAL DE AJUDA</p>
          <h1>Como podemos ajudar?</h1>
          <p className="page-description">Encontre respostas rápidas sobre seus dados, perfis e recursos do Katu’I.</p>
        </div>
      </section>
      <div className="help-search">
        <Icon name="search" size={18} />
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Busque uma dúvida, como “offline” ou “IA”"
          aria-label="Buscar perguntas frequentes"
        />
      </div>
      <div className="faq-list">
        {filtered.length ? (
          filtered.map(([category, question, answer]) => {
            const id = category + '-' + question
            const isOpen = open === id
            return (
              <article className={isOpen ? 'faq-item open' : 'faq-item'} key={id}>
                <button className="faq-question" aria-expanded={isOpen} onClick={() => setOpen(isOpen ? null : id)}>
                  <span>
                    <small>{category}</small>
                    <strong>{question}</strong>
                  </span>
                  <Icon name="chevron" size={17} />
                </button>
                {isOpen && <p className="faq-answer">{answer}</p>}
              </article>
            )
          })
        ) : (
          <div className="panel faq-empty">
            <div className="empty-icon">
              <Icon name="search" size={24} />
            </div>
            <strong>Nenhuma resposta encontrada</strong>
            <p>Tente buscar por outra palavra.</p>
          </div>
        )}
      </div>
    </div>
  )
}
