import { useEffect, useState } from 'react'
import { Icon } from './interfaceCompartilhada'

const steps = [
  { title: 'Tipo e data', description: 'Escolha o tipo de registro e informe a data.' },
  { title: 'Sintoma ou clima', description: 'Descreva o sintoma físico ou escolha o clima do seu humor.' },
  { title: 'Contexto', description: 'Acrescente detalhes que ajudem a entender como você está.' },
  { title: 'Intensidade', description: 'Indique a intensidade do sintoma ou sentimento.' },
]

function SymptomForm({ form, setForm, step, direction }) {
  const set = (key, value) => setForm({ ...form, [key]: value })
  const isPhysical = form.symptomKind === 'FISICO'
  const physicalLocations = [
    ['Geral', 'pulse'],
    ['Cabeça', 'profile'],
    ['Costas', 'pulse'],
    ['Peito', 'heart'],
    ['Estômago', 'heart'],
    ['Braços', 'pulse'],
    ['Pernas', 'pulse'],
  ]
  const moodOptions = {
    Positivo: [
      ['Alegre', 'sun'],
      ['Calmo', 'spark'],
      ['Motivado', 'bolt'],
      ['Produtivo', 'spark'],
      ['Grato', 'heart'],
      ['Energizado', 'bolt'],
      ['Eufórico', 'spark'],
    ],
    Neutro: [
      ['Normal', 'check'],
      ['Pensativo', 'spark'],
      ['Cansado', 'battery'],
      ['Apático', 'pulse'],
    ],
    Baixo: [
      ['Triste', 'cloud'],
      ['Desanimado', 'battery'],
      ['Solitário', 'profile'],
      ['Sensível', 'cloud'],
      ['Sem energia', 'battery'],
      ['Depressivo', 'sad'],
    ],
    Tenso: [
      ['Ansioso', 'cloud'],
      ['Irritado', 'bolt'],
      ['Estressado', 'cloud'],
      ['Agitado', 'spark'],
      ['Sob pressão', 'pulse'],
      ['Confuso', 'brain'],
    ],
  }
  const input = (key, label, placeholder, icon) => (
    <label className="symptom-field">
      {icon && <Icon name={icon} size={15} />}
      <span>{label}</span>
      <input
        value={form[key] || ''}
        onChange={(event) => set(key, event.target.value)}
        placeholder={placeholder || ''}
      />
    </label>
  )
  return (
    <div className={`symptom-form form-step-panel slide-${direction}`}>
      {step === 0 && (
        <div className="symptom-kind-tabs">
          <button
            type="button"
            className={isPhysical ? 'selected' : ''}
            aria-pressed={isPhysical}
            onClick={() => set('symptomKind', 'FISICO')}
          >
            <Icon name="pulse" size={16} /> Físico
          </button>
          <button
            type="button"
            className={!isPhysical ? 'selected' : ''}
            aria-pressed={!isPhysical}
            onClick={() => set('symptomKind', 'PSICOLOGICO')}
          >
            <Icon name="heart" size={16} /> Humor
          </button>
        </div>
      )}
      {step === 1 && isPhysical && (
        <>
          {input('quality', 'O que você está sentindo?', 'Ex.: Dor de cabeça', 'pulse')}
          <div className="symptom-section-title">
            <Icon name="location" size={15} /> Localização
          </div>
          <div className="symptom-chip-grid">
            {physicalLocations.map(([label, icon]) => (
              <button
                type="button"
                key={label}
                className={form.location === label ? 'symptom-chip selected' : 'symptom-chip'}
                aria-pressed={form.location === label}
                onClick={() => set('location', label)}
              >
                <Icon name={icon} size={15} />
                {label}
              </button>
            ))}
          </div>
          {input('location', 'Ou digite o local...', 'Ex.: lado esquerdo da cabeça', 'location')}
        </>
      )}
      {step === 1 && !isPhysical && (
        <>
          <div className="symptom-section-title">
            <Icon name="brain" size={15} /> O clima geral
          </div>
          <div className="mood-grid">
            {['Positivo', 'Neutro', 'Baixo', 'Tenso'].map((mood) => (
              <button
                type="button"
                key={mood}
                className={form.climate === mood ? 'selected' : ''}
                aria-pressed={form.climate === mood}
                onClick={() => setForm({ ...form, climate: mood, nuances: [] })}
              >
                <Icon
                  name={mood === 'Positivo' ? 'sun' : mood === 'Neutro' ? 'cloud' : mood === 'Baixo' ? 'sad' : 'brain'}
                  size={17}
                />
                {mood}
              </button>
            ))}
          </div>
        </>
      )}
      {step === 2 && isPhysical && (
        <>
          {input('duration', 'Quando começou / quanto dura?', 'Ex.: desde ontem', 'clock')}
          {input('factors', 'O que piora ou melhora?', 'Opcional', 'search')}
          {input('impact', 'Como afeta seu dia?', 'Opcional', 'trend')}
          <label className="symptom-check">
            <input
              type="checkbox"
              checked={Boolean(form.incapacitating)}
              onChange={(event) => set('incapacitating', event.target.checked)}
            />{' '}
            Isso te impede de fazer algo?
          </label>
        </>
      )}
      {step === 2 && !isPhysical && (
        <>
          {form.climate && (
            <>
              <div className="symptom-section-title">
                <Icon name="spark" size={15} /> Nuances
              </div>
              <div className="symptom-chip-grid">
                {(moodOptions[form.climate] || []).map(([label, icon]) => {
                  const selected = (form.nuances || []).includes(label)
                  return (
                    <button
                      type="button"
                      key={label}
                      className={selected ? 'symptom-chip selected' : 'symptom-chip'}
                      aria-pressed={selected}
                      onClick={() =>
                        set(
                          'nuances',
                          selected ? form.nuances.filter((item) => item !== label) : [...(form.nuances || []), label],
                        )
                      }
                    >
                      <Icon name={icon} size={14} />
                      {label}
                    </button>
                  )
                })}
              </div>
            </>
          )}
          {input('factors', 'Gatilhos ou motivos', 'Opcional: por que se sente assim?', 'search')}
        </>
      )}
      {step === 3 && (
        <div className="symptom-intensity">
          <div>
            <span>{isPhysical ? 'Intensidade da dor' : 'Força do sentimento'}</span>
            <strong>
              {Number(form.intensity || 5) <= 3
                ? isPhysical
                  ? 'Leve'
                  : 'Estável'
                : Number(form.intensity || 5) <= 7
                  ? isPhysical
                    ? 'Moderada'
                    : 'Presente'
                  : isPhysical
                    ? 'Intensa'
                    : 'Muito forte'}
            </strong>
          </div>
          <input
            type="range"
            min="1"
            max="10"
            value={form.intensity || 5}
            onChange={(event) => set('intensity', event.target.value)}
          />
        </div>
      )}
    </div>
  )
}

export function SymptomModal({ action, form, setForm, onClose, onSubmit, connection, canSubmit }) {
  const [step, setStep] = useState(0)
  const [direction, setDirection] = useState('next')
  useEffect(() => {
    setStep(0)
    setDirection('next')
  }, [action])
  const moveStep = (nextStep) => {
    setDirection(nextStep > step ? 'next' : 'previous')
    setStep(nextStep)
  }
  const save = () =>
    onSubmit({
      ...action,
      data: {
        ...form,
        name: form.quality || form.climate,
        type: form.symptomKind,
        quality:
          form.symptomKind === 'PSICOLOGICO' ? `[${form.climate}] ${(form.nuances || []).join(', ')}` : form.quality,
      },
    })
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className="modal-card symptom-modal">
        <button className="modal-close" aria-label="Fechar" onClick={onClose}>
          ×
        </button>
        <div className="modal-icon">
          <Icon name={form.symptomKind === 'FISICO' ? 'pulse' : 'brain'} size={22} />
        </div>
        <h2>{action.title}</h2>
        <p className="modal-subtitle">Registre sintomas físicos ou como você está se sentindo.</p>
        <div className="form-stepper">
          <div className="form-step-heading" aria-live="polite">
            <span>
              Etapa {step + 1} de {steps.length}
            </span>
            <strong>{steps[step].title}</strong>
            <small>{steps[step].description}</small>
          </div>
          <div className="form-step-progress" aria-hidden="true">
            {steps.map((item, index) => (
              <span key={item.title} className={index <= step ? 'active' : ''} />
            ))}
          </div>
        </div>
        {step === 0 && (
          <label className="symptom-date-field">
            Data do registro
            <input
              type="date"
              value={form.date || ''}
              onChange={(event) => setForm({ ...form, date: event.target.value })}
            />
          </label>
        )}
        <SymptomForm key={step} form={form} setForm={setForm} step={step} direction={direction} />
        {step === steps.length - 1 && !canSubmit && (
          <small className="form-error">
            {form.symptomKind === 'FISICO'
              ? 'Informe o que você está sentindo.'
              : 'Escolha o clima geral do seu humor.'}
          </small>
        )}
        <div className="modal-actions">
          <button className="cancel-btn" onClick={onClose}>
            Cancelar
          </button>
          {step > 0 && (
            <button className="cancel-btn" onClick={() => moveStep(step - 1)}>
              Voltar
            </button>
          )}
          {step < steps.length - 1 ? (
            <button className="primary-btn" onClick={() => moveStep(step + 1)}>
              Próximo
            </button>
          ) : (
            <button className="primary-btn" disabled={!canSubmit} onClick={save}>
              Registrar
            </button>
          )}
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
