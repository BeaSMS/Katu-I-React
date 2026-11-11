import { useEffect, useRef, useState } from 'react'
import { dateKeyForFemaleHealth, getCycleDayKind, getCycleInsights } from '../../services/femaleHealth'
import {
  getContraceptiveDay,
  getContraceptivePattern,
  getContraceptivePatternDescription,
} from '../../services/contraceptive'
import { medications } from './dadosAplicacao'
import { Icon, vitalNumericValue } from './interfaceCompartilhada'
import { DependentModal } from './configuracoes'
import { PrescriptionReview } from './revisaoPrescricao'
import { SymptomModal } from './modalSintomas'

const medicationWizardSteps = [
  {
    title: 'Medicamento',
    description: 'Informe o nome, a dosagem e a finalidade do tratamento.',
    fields: ['name', 'dosage', 'purpose'],
  },
  {
    title: 'Frequência e início',
    description: 'Defina quando o tratamento começa e com que frequência tomar.',
    fields: ['date', 'time', 'frequencyType', 'frequencyValue'],
  },
  {
    title: 'Duração e estoque',
    description: 'Informe a duração do tratamento e acompanhe suas unidades.',
    fields: ['continuous', 'durationDays', 'quantity', 'minimumStock'],
  },
  {
    title: 'Dose e lembretes',
    description: 'Complete as doses por tomada e escolha se quer criar alarmes.',
    fields: ['dosePerTake', 'unit', 'generateAlarms'],
  },
]

export function ActionModal({ action, onClose, onSubmit, connection, selectedDate, pushEnabled }) {
  const currentTime = new Date().toTimeString().slice(0, 5)
  const initialForm = {
    name: '',
    detail: '',
    date: selectedDate || '',
    time: currentTime,
    notes: '',
    type: action.vitalType || '',
    value: '',
    systolic: '',
    diastolic: '',
    quality: '',
    location: '',
    duration: '',
    factors: '',
    impact: '',
    severity: '',
    category: '',
    examType: '',
    medication: '',
    flow: '',
    mood: '',
    physicalSymptoms: '',
    unknownDate: false,
    dosage: '',
    purpose: '',
    continuous: false,
    durationDays: '7',
    quantity: '30',
    minimumStock: '5',
    dosePerTake: '1',
    unit: 'un',
    frequencyType: 'A cada intervalo de horas',
    frequencyValue: '8',
    generateAlarms: true,
    symptomKind: 'FISICO',
    climate: '',
    nuances: [],
    intensity: 5,
    incapacitating: false,
    ...(action.data || {}),
  }
  const [form, setForm] = useState(initialForm)
  const [medicationStep, setMedicationStep] = useState(0)
  const [stepDirection, setStepDirection] = useState('next')
  const initialFormSnapshot = useRef(JSON.stringify(initialForm))
  useEffect(() => {
    const nextForm = { ...initialForm, ...(action.data || {}) }
    initialFormSnapshot.current = JSON.stringify(nextForm)
    setForm(nextForm)
    setMedicationStep(0)
    setStepDirection('next')
    setFile(null)
    setFileName('')
    setFileError('')
  }, [action])
  const [fileName, setFileName] = useState('')
  const [file, setFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [submissionError, setSubmissionError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const vitalTypeOptions = [
    { label: 'Pressão arterial', icon: 'heart' },
    { label: 'Glicose', icon: 'droplet' },
    { label: 'Batimentos', icon: 'pulse' },
    { label: 'Saturação', icon: 'battery' },
    { label: 'Temperatura', icon: 'thermometer' },
    { label: 'Peso', icon: 'scale' },
    { label: 'Altura', icon: 'ruler' },
  ]
  const isConfirm = action.kind === 'confirm'
  const isAi = action.kind === 'ai'
  const close = () => {
    const hasChanges = JSON.stringify(form) !== initialFormSnapshot.current || Boolean(file)
    if (!isConfirm && hasChanges && !window.confirm('Há alterações não salvas. Deseja fechar e descartá-las?')) return
    onClose()
  }
  const endpoint = action.endpoint || ''
  const title = action.title.toLowerCase()
  const isProfileForm = endpoint === '/profiles' || /^\/profiles\/[^/]+$/.test(endpoint)
  const isMedication =
    endpoint === '/medications' ||
    endpoint.startsWith('/medications/') ||
    title.includes('medicamento') ||
    title.includes('remédio')
  const isContraceptiveSetup =
    isMedication &&
    (action.contraceptiveSetup ||
      title.includes('anticoncepcional') ||
      String(form.purpose || form.category || '')
        .toLowerCase()
        .includes('anticoncepcional') ||
      Boolean(form.contraceptivePackType))
  const isMedicationWizard = isMedication && !isContraceptiveSetup && !isConfirm && !isAi
  const isAlarm = endpoint === '/alarms' || title.includes('alarme')
  const isPrescription = endpoint === '/ai/prescription' || endpoint === '/prescriptions' || title.includes('receita')
  const isDocument = endpoint === '/documents' || title.includes('documento')
  const isVitals =
    endpoint === '/vital-signs' ||
    endpoint.startsWith('/vital-signs/') ||
    title.includes('sinal vital') ||
    title.includes('pressão') ||
    title.includes('temperatura')
  const isAppointment = endpoint === '/appointments' || title.includes('consulta')
  const isExam = endpoint === '/exams' || title.includes('exame')
  const isSymptom =
    endpoint === '/symptoms' ||
    (title.includes('sintoma') &&
      !endpoint.startsWith('/feminine-health') &&
      !title.includes('ciclo') &&
      !title.includes('fluxo') &&
      !title.includes('anticoncepcional'))
  const isFemale =
    !isMedication &&
    (endpoint === '/feminine-health' ||
      endpoint.startsWith('/feminine-health/') ||
      title.includes('saúde feminina') ||
      title.includes('ciclo') ||
      title.includes('sintoma do ciclo') ||
      title.includes('anticoncepcional'))
  const isCycleEnd = isFemale && (title.includes('fim de ciclo') || Boolean(form.endDate))
  const isFemaleDailyLog =
    isFemale && !isCycleEnd && !title.includes('início de ciclo') && !title.includes('tomada de anticoncepcional')
  const isVaccine = endpoint === '/vaccines' || title.includes('vacina')
  const hasExistingAttachment = Boolean(
    action.data?.attachment?.name || action.data?.attachment?.url || action.data?.attachment?.downloadUrl,
  )
  const specialized =
    isMedication ||
    isAlarm ||
    isPrescription ||
    isVitals ||
    isAppointment ||
    isExam ||
    isSymptom ||
    isFemale ||
    isVaccine
  const vitalType = form.type || action.vitalType || ''
  const vitalValue = vitalType === 'Pressão arterial' ? form.systolic && form.diastolic : form.value
  const numericVitalValues = vitalType === 'Pressão arterial' ? [form.systolic, form.diastolic] : [form.value]
  const vitalValuesValid =
    numericVitalValues.every((value) => vitalNumericValue({ value }, 'value') > 0) &&
    (vitalType !== 'Saturação' || Number(String(form.value).replace(',', '.')) <= 100)
  const symptomReady = form.symptomKind === 'FISICO' ? form.quality : form.climate
  const femaleValue = isCycleEnd
    ? form.endDate
    : title.includes('início de ciclo')
      ? form.date
      : title.includes('tomada de anticoncepcional')
        ? form.date && form.time
        : form.date &&
          [form.flow, form.physicalSymptoms, form.mood, form.notes].some((value) => String(value || '').trim())
  const requiredValue = isMedication
    ? isContraceptiveSetup
      ? form.name && form.date && form.time && form.contraceptivePackType
      : form.name
    : isAlarm
      ? form.name && form.date && form.time
      : isPrescription
        ? isAi
          ? file || form.prescriptionId
          : form.name
        : isVitals
          ? vitalType && vitalValue && vitalValuesValid
          : isAppointment
            ? form.name
            : isExam
              ? form.name && (file || hasExistingAttachment)
              : isSymptom
                ? symptomReady
                : isFemale
                  ? femaleValue
                  : isVaccine
                    ? form.name
                    : form.name
  const isFutureVitals =
    isVitals && form.date && form.time && new Date(`${form.date}T${form.time}`).getTime() > Date.now()
  const isFutureAlarmInvalid =
    isAlarm && form.date && form.time && new Date(`${form.date}T${form.time}`).getTime() <= Date.now()
  const medicationFrequency = Number(form.frequencyValue)
  const medicationStart = new Date(`${form.date}T${form.time}`)
  const medicationScheduleInvalid =
    isMedication &&
    form.generateAlarms &&
    (!form.date ||
      !form.time ||
      Number.isNaN(medicationStart.getTime()) ||
      (!isContraceptiveSetup && medicationStart.getTime() <= Date.now()) ||
      !Number.isInteger(medicationFrequency) ||
      medicationFrequency < 1 ||
      medicationFrequency > 24 ||
      (!form.continuous &&
        (!Number.isInteger(Number(form.durationDays)) ||
          Number(form.durationDays) < 1 ||
          Number(form.durationDays) > 3650)))
  const today = dateKeyForFemaleHealth(new Date())
  const femaleDateError = isCycleEnd
    ? form.endDate > today
      ? 'O último dia do fluxo não pode ser uma data futura.'
      : form.date && form.endDate < form.date
        ? 'O último dia do fluxo não pode ser anterior ao início.'
        : ''
    : form.date > today
      ? 'Escolha uma data de hoje ou anterior.'
      : ''
  const femaleDateInvalid = isFemale && Boolean(femaleDateError)
  const canSubmit =
    isConfirm ||
    (Boolean(requiredValue) &&
      !fileError &&
      !isFutureVitals &&
      !isFutureAlarmInvalid &&
      !medicationScheduleInvalid &&
      !femaleDateInvalid)
  const update = (key) => (event) =>
    setForm(
      key === 'type'
        ? { ...form, type: event.target.value, value: '', systolic: '', diastolic: '' }
        : { ...form, [key]: event.target.value },
    )
  const medicationStepIndex = Math.min(medicationStep, medicationWizardSteps.length - 1)
  const isLastMedicationStep = medicationStepIndex === medicationWizardSteps.length - 1
  const moveMedicationStep = (nextStep) => {
    setStepDirection(nextStep > medicationStepIndex ? 'next' : 'previous')
    setMedicationStep(nextStep)
  }
  const submit = async (nextAction) => {
    setSubmitting(true)
    setSubmissionError('')
    try {
      await onSubmit(nextAction)
    } catch (error) {
      setSubmissionError(error.message || 'Não foi possível salvar. Tente novamente.')
    } finally {
      setSubmitting(false)
    }
  }
  const submittedForm = isContraceptiveSetup
    ? (() => {
        const pattern = getContraceptivePattern(form.contraceptivePackType)
        return {
          ...form,
          purpose: 'Anticoncepcional',
          dosage:
            form.contraceptivePackType === 'continuous' ? 'Uso contínuo' : `Cartela ${form.contraceptivePackType} dias`,
          continuous: true,
          frequencyType: 'Vezes ao dia',
          frequencyValue: '1',
          contraceptiveActiveDays: pattern?.activeDays,
          contraceptivePauseDays: pattern?.pauseDays,
        }
      })()
    : isVaccine
      ? { ...form, date: form.unknownDate ? '' : form.date }
      : isVitals
        ? {
            ...form,
            ...(vitalType === 'Pressão arterial'
              ? {
                  systolic: Number(String(form.systolic).replace(',', '.')),
                  diastolic: Number(String(form.diastolic).replace(',', '.')),
                }
              : { value: Number(String(form.value).replace(',', '.')) }),
          }
        : form
  const submitFormData = () => {
    if (isConfirm) return action.data
    const data = isAi
      ? { ...form, fileName, file }
      : isFemale && form.recordType === 'menstrual-cycle'
        ? { ...form, ...(isCycleEnd ? { date: form.endDate } : { startDate: form.date }) }
        : submittedForm
    const { attachment: _attachment, ...formData } = data
    return { ...formData, ...(file ? { file } : {}) }
  }
  const fields = []
  if (isContraceptiveSetup)
    fields.push(
      { key: 'name', label: 'Nome do anticoncepcional', placeholder: 'Ex.: Selene', required: true },
      { key: 'contraceptivePackType', label: 'Padrão da cartela', type: 'contraceptive-pack', required: true },
      { key: 'date', label: 'Data de início da cartela', type: 'date', required: true },
      { key: 'time', label: 'Horário diário da tomada', type: 'time', required: true },
      { key: 'generateAlarms', label: 'Criar lembrete diário', type: 'checkbox' },
    )
  else if (isMedication)
    fields.push(
      { key: 'name', label: 'Nome do remédio', placeholder: 'Ex.: Losartana 50 mg', required: true },
      { key: 'dosage', label: 'Dosagem', placeholder: 'Ex.: 1 comprimido' },
      { key: 'purpose', label: 'Finalidade', placeholder: 'Ex.: Controle da pressão' },
      { key: 'date', label: 'Data inicial', type: 'date' },
      { key: 'time', label: 'Horário inicial', type: 'time' },
      {
        key: 'frequencyType',
        label: 'Frequência',
        type: 'select',
        options: ['A cada intervalo de horas', 'Vezes ao dia'],
      },
      {
        key: 'frequencyValue',
        label: 'Quantidade da frequência (1 a 24)',
        type: 'number',
        min: 1,
        max: 24,
        step: 1,
        placeholder: 'Ex.: 8',
      },
      { key: 'continuous', label: 'Uso contínuo', type: 'checkbox' },
      ...(form.continuous
        ? []
        : [
            {
              key: 'durationDays',
              label: 'Duração (dias)',
              type: 'number',
              min: 1,
              max: 3650,
              step: 1,
              placeholder: 'Ex.: 7',
            },
          ]),
      { key: 'quantity', label: 'Quantidade em estoque', type: 'number', placeholder: 'Ex.: 30' },
      { key: 'minimumStock', label: 'Estoque mínimo', type: 'number', placeholder: 'Ex.: 5' },
      { key: 'dosePerTake', label: 'Doses por tomada', type: 'number', placeholder: 'Ex.: 1' },
      { key: 'unit', label: 'Unidade de medida', placeholder: 'Ex.: comprimidos, ml, gotas' },
      { key: 'generateAlarms', label: 'Criar alarmes automaticamente', type: 'checkbox' },
    )
  if (isAlarm)
    fields.push(
      { key: 'name', label: 'Título / descrição', placeholder: 'Ex.: Tomar remédio', required: true },
      { key: 'medication', label: 'Medicamento vinculado', placeholder: 'Opcional' },
      { key: 'date', label: 'Data', type: 'date' },
      { key: 'time', label: 'Horário', type: 'time' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  if (isPrescription && !isAi)
    fields.push({
      key: 'name',
      label: 'Título da receita',
      placeholder: 'Ex.: Receita de uso contínuo',
      required: true,
    })
  if (isPrescription)
    fields.push(
      { key: 'date', label: 'Data da receita', type: 'date' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  if (isVitals) {
    fields.push({ key: 'type', label: 'Tipo de registro', type: 'vital-type', required: true })
    const vitalFields = {
      'Pressão arterial': [
        {
          key: 'systolic',
          label: 'Sistólica (mmHg)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 120',
          required: true,
        },
        {
          key: 'diastolic',
          label: 'Diastólica (mmHg)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 80',
          required: true,
        },
      ],
      Glicose: [
        {
          key: 'value',
          label: 'Glicose (mg/dL)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 95',
          required: true,
        },
      ],
      Batimentos: [
        {
          key: 'value',
          label: 'Batimentos (BPM)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 72',
          required: true,
        },
      ],
      Saturação: [
        {
          key: 'value',
          label: 'Saturação (%)',
          type: 'number',
          min: 0,
          max: 100,
          step: 'any',
          placeholder: 'Ex.: 98',
          required: true,
        },
      ],
      Temperatura: [
        {
          key: 'value',
          label: 'Temperatura (°C)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 36,5',
          required: true,
        },
      ],
      Peso: [
        {
          key: 'value',
          label: 'Peso (kg)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 70,5',
          required: true,
        },
      ],
      Altura: [
        {
          key: 'value',
          label: 'Altura (cm)',
          type: 'number',
          min: 0,
          step: 'any',
          placeholder: 'Ex.: 170',
          required: true,
        },
      ],
    }
    fields.push(
      ...(vitalFields[vitalType] || []),
      { key: 'date', label: 'Data', type: 'date' },
      { key: 'time', label: 'Hora', type: 'time' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  }
  if (isAppointment)
    fields.push(
      { key: 'name', label: 'Nome do médico', placeholder: 'Ex.: Dra. Ana Silva', required: true },
      { key: 'detail', label: 'Especialidade', placeholder: 'Ex.: Cardiologia' },
      { key: 'location', label: 'Local / clínica' },
      { key: 'date', label: 'Data', type: 'date' },
      { key: 'time', label: 'Hora', type: 'time' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  if (isExam)
    fields.push(
      { key: 'name', label: 'Nome do exame', placeholder: 'Ex.: Hemograma', required: true },
      {
        key: 'category',
        label: 'Categoria',
        type: 'select',
        options: ['Outros', 'Laboratório', 'Imagem', 'Cardiologia', 'Endoscopia'],
      },
      {
        key: 'examType',
        label: 'Tipo de resultado',
        type: 'select',
        options: ['Imagem única', 'Série temporal', 'Resultado numérico'],
      },
      { key: 'date', label: 'Data do exame', type: 'date' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  if (isSymptom) fields.push({ key: 'date', label: 'Data do registro', type: 'date' })
  if (isFemale && isCycleEnd)
    fields.push({ key: 'endDate', label: 'Último dia do fluxo', type: 'date', required: true })
  else if (isFemale && title.includes('início de ciclo'))
    fields.push(
      { key: 'date', label: 'Data de início do fluxo', type: 'date', required: true },
      { key: 'flow', label: 'Intensidade do fluxo', type: 'select', options: ['Leve', 'Média', 'Intensa'] },
      { key: 'physicalSymptoms', label: 'Sintomas ou observações', type: 'textarea' },
    )
  else if (isFemale && title.includes('tomada de anticoncepcional'))
    fields.push(
      { key: 'date', label: 'Data', type: 'date', required: true },
      { key: 'time', label: 'Horário tomado', type: 'time', required: true },
      { key: 'taken', label: 'Tomei a dose', type: 'checkbox' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  else if (isFemale)
    fields.push(
      { key: 'date', label: 'Data do registro', type: 'date', required: true },
      {
        key: 'flow',
        label: 'Intensidade do fluxo',
        type: 'select',
        options: ['Sem fluxo', 'Leve', 'Média', 'Intensa'],
      },
      { key: 'physicalSymptoms', label: 'Sintomas físicos', type: 'textarea' },
      {
        key: 'mood',
        label: 'Humor',
        type: 'select',
        options: ['Estável', 'Triste', 'Ansiosa', 'Irritada', 'Disposta'],
      },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  if (isVaccine)
    fields.push(
      { key: 'name', label: 'Nome da vacina', placeholder: 'Ex.: Influenza', required: true },
      { key: 'date', label: 'Data em que foi tomada', type: 'date', disabled: Boolean(form.unknownDate) },
      { key: 'unknownDate', label: 'Não sei a data exata', type: 'checkbox' },
      { key: 'notes', label: 'Observações (lote, local etc.)', type: 'textarea' },
    )
  if (!specialized && !isAi)
    fields.push(
      { key: 'name', label: 'Nome ou título', placeholder: 'Digite um nome', required: true },
      { key: 'detail', label: 'Detalhes', placeholder: 'Digite os detalhes' },
      { key: 'date', label: 'Data', type: 'date' },
      { key: 'notes', label: 'Observações', type: 'textarea' },
    )
  const visibleFields = isMedicationWizard
    ? fields.filter((field) => medicationWizardSteps[medicationStepIndex].fields.includes(field.key))
    : fields
  if (action.kind === 'ai-review')
    return <PrescriptionReview action={action} onClose={onClose} onSubmit={onSubmit} pushEnabled={pushEnabled} />
  if (action.kind === 'success')
    return (
      <div className="modal-backdrop">
        <div className="modal-card success-modal">
          <div className="success-check">
            <Icon name="check" size={27} />
          </div>
          <h2>{action.title}</h2>
          <p>{action.message || 'A informação foi salva com sucesso no seu espaço de saúde.'}</p>
          {submissionError && (
            <small className="form-error" role="alert">
              {submissionError}
            </small>
          )}
          {action.followUp && (
            <button className="soft-btn modal-primary" disabled={submitting} onClick={() => submit(action.followUp)}>
              {submitting ? 'Enviando para análise...' : 'Ler receita com IA agora'}
            </button>
          )}
          <button className="primary-btn modal-primary" onClick={onClose}>
            Continuar
          </button>
        </div>
      </div>
    )
  if (isProfileForm && !isConfirm && !isAi)
    return (
      <>
        <DependentModal
          key={endpoint}
          action={action}
          onClose={onClose}
          onSubmit={submit}
          connection={connection}
          submissionError={submissionError}
          submitting={submitting}
        />
        {submissionError && (
          <div className="submission-error-toast" role="alert">
            {submissionError}
          </div>
        )}
      </>
    )
  if (isSymptom && !isConfirm && !isAi)
    return (
      <>
        <SymptomModal
          action={action}
          form={form}
          setForm={setForm}
          onClose={close}
          onSubmit={submit}
          connection={connection}
          canSubmit={Boolean(symptomReady)}
        />
        {submissionError && (
          <div className="submission-error-toast" role="alert">
            {submissionError}
          </div>
        )}
      </>
    )
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && close()}>
      <div className="modal-card">
        <button className="modal-close" aria-label="Fechar" onClick={close}>
          ×
        </button>
        <div className="modal-icon">{isAi ? '✦' : isConfirm ? '!' : <Icon name="plus" size={22} />}</div>
        <h2>{action.title}</h2>
        <p className="modal-subtitle">
          {isConfirm
            ? action.message
            : isAi
              ? 'A IA analisa o arquivo e sugere os dados da receita. Você revisa tudo antes de salvar.'
              : isContraceptiveSetup
                ? 'Cadastre este método separado dos medicamentos comuns. Escolha o padrão da cartela para acompanhar dias de uso e pausa.'
                : isExam
                  ? 'Anexe o resultado do exame em PDF ou imagem para manter o documento junto ao registro.'
                  : isDocument
                    ? 'Anexe um documento de saúde em PDF ou imagem, se disponível.'
                    : isPrescription
                      ? 'Anexe a foto ou o PDF da receita. Depois de salvar, você poderá pedir à IA para sugerir os medicamentos.'
                      : 'Preencha os dados abaixo. Você poderá editar essas informações depois.'}
        </p>
        {(isAi || isExam || isDocument || (isPrescription && !isAi)) && (
          <div className="upload-zone">
            <div className="upload-icon">
              <Icon name="file" size={22} />
            </div>
            <strong>
              {fileName ||
                action.data?.attachment?.name ||
                action.data?.attachmentName ||
                (isExam
                  ? 'Anexe o resultado do exame'
                  : isDocument
                    ? 'Anexe um documento'
                    : isPrescription
                      ? 'Anexe a receita'
                      : 'Escolha uma receita')}
            </strong>
            <span>
              {isExam
                ? 'Obrigatório · PDF, JPG ou PNG · até 15 MB'
                : isPrescription
                  ? isAi
                    ? action.data?.prescriptionId
                      ? 'Documento salvo · escolha outro arquivo para substituir'
                      : 'Obrigatório · PDF, JPG ou PNG · até 15 MB'
                    : 'Opcional · PDF, JPG ou PNG · até 15 MB'
                  : 'PDF, JPG ou PNG · até 15 MB'}
            </span>
            <label className="soft-btn">
              {fileName || action.data?.attachment?.name || action.data?.attachmentName
                ? 'Substituir arquivo'
                : 'Escolher arquivo'}
              <input
                className="file-input"
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                onChange={(event) => {
                  const selected = event.target.files?.[0] || null
                  event.target.value = ''
                  const extensionAllowed = Boolean(selected && /\.(pdf|jpe?g|png)$/i.test(selected.name))
                  const mimeAllowed =
                    !selected?.type || ['application/pdf', 'image/jpeg', 'image/png'].includes(selected.type)
                  const tooLarge = Boolean(selected && selected.size > 15 * 1024 * 1024)
                  if ((selected && !extensionAllowed) || (selected && !mimeAllowed)) {
                    setFile(null)
                    setFileName('')
                    setFileError('Escolha um arquivo PDF, JPG ou PNG.')
                    return
                  }
                  if (tooLarge) {
                    setFile(null)
                    setFileName('')
                    setFileError('O arquivo deve ter no máximo 15 MB.')
                    return
                  }
                  setFile(selected)
                  setFileName(selected?.name || '')
                  setFileError('')
                }}
              />
            </label>
            <small>Seus documentos são protegidos e privados.</small>
            {fileError && (
              <small className="form-error" role="alert">
                {fileError}
              </small>
            )}
          </div>
        )}
        {isConfirm ? (
          <div className="confirm-warning">Você poderá desfazer esta remoção logo após confirmar.</div>
        ) : (
          !isAi && (
            <>
              {isMedicationWizard && (
                <div className="form-stepper">
                  <div className="form-step-heading" aria-live="polite">
                    <span>
                      Etapa {medicationStepIndex + 1} de {medicationWizardSteps.length}
                    </span>
                    <strong>{medicationWizardSteps[medicationStepIndex].title}</strong>
                    <small>{medicationWizardSteps[medicationStepIndex].description}</small>
                  </div>
                  <div className="form-step-progress" aria-hidden="true">
                    {medicationWizardSteps.map((step, index) => (
                      <span key={step.title} className={index <= medicationStepIndex ? 'active' : ''} />
                    ))}
                  </div>
                </div>
              )}
              <div
                key={isMedicationWizard ? medicationStepIndex : 'form'}
                className={`modal-form${isContraceptiveSetup ? ' contraceptive-form' : ''}${
                  isMedicationWizard ? ` form-step-panel slide-${stepDirection}` : ''
                }`}
              >
                {visibleFields.map((field) =>
                  field.type === 'textarea' ? (
                  <label key={field.key}>
                    {field.label}
                    <textarea
                      value={form[field.key] || ''}
                      onChange={update(field.key)}
                      placeholder={field.placeholder || 'Opcional'}
                    />
                  </label>
                ) : field.type === 'vital-type' ? (
                  <fieldset className="vital-type-field" key={field.key}>
                    <legend>{field.label}</legend>
                    <div className="vital-type-grid" role="group" aria-label={field.label}>
                      {vitalTypeOptions.map((option) => (
                        <button
                          key={option.label}
                          type="button"
                          className={`vital-type-option${vitalType === option.label ? ' selected' : ''}`}
                          aria-pressed={vitalType === option.label}
                          onClick={() =>
                            setForm({ ...form, type: option.label, value: '', systolic: '', diastolic: '' })
                          }
                        >
                          <Icon name={option.icon} size={21} />
                          <span>{option.label}</span>
                        </button>
                      ))}
                    </div>
                  </fieldset>
                ) : field.type === 'contraceptive-pack' ? (
                  <fieldset className="contraceptive-pack-field" key={field.key}>
                    <legend>{field.label}</legend>
                    <div className="contraceptive-pack-options" role="group" aria-label={field.label}>
                      {[
                        ['21', '21 dias', '21 dias de uso e 7 dias de pausa'],
                        ['24', '24 dias', '24 dias de uso e 4 dias de pausa'],
                        ['28', '28 dias', 'Sem pausa entre cartelas'],
                        ['continuous', 'Contínuo', 'Sem pausa programada'],
                      ].map(([value, label, description]) => (
                        <button
                          type="button"
                          key={value}
                          className={`contraceptive-pack-option${form.contraceptivePackType === value ? ' selected' : ''}`}
                          aria-pressed={form.contraceptivePackType === value}
                          onClick={() => setForm({ ...form, contraceptivePackType: value })}
                        >
                          <strong>{label}</strong>
                          <small>{description}</small>
                        </button>
                      ))}
                    </div>
                    <small className="contraceptive-pack-note">
                      Confirme o esquema indicado para o anticoncepcional que você usa. O lembrete diário é suspenso
                      durante a pausa configurada.
                    </small>
                  </fieldset>
                ) : field.type === 'select' ? (
                  <label key={field.key}>
                    {field.label}
                    <select value={form[field.key] || ''} onChange={update(field.key)}>
                      <option value="">Selecione</option>
                      {field.options.map((option) => (
                        <option key={option} value={option}>
                          {option}
                        </option>
                      ))}
                    </select>
                  </label>
                ) : field.type === 'checkbox' ? (
                  <label className="check-field" key={field.key}>
                    <input
                      type="checkbox"
                      checked={Boolean(form[field.key])}
                      onChange={(event) =>
                        setForm(
                          field.key === 'unknownDate' && event.target.checked
                            ? { ...form, unknownDate: true, date: '' }
                            : { ...form, [field.key]: event.target.checked },
                        )
                      }
                    />
                    {field.label}
                  </label>
                ) : (
                  <label key={field.key}>
                    {field.label}
                    <input
                      type={field.type || 'text'}
                      value={form[field.key] || ''}
                      onChange={update(field.key)}
                      placeholder={field.placeholder || ''}
                      required={field.required}
                      min={field.min}
                      max={field.max}
                      step={field.step}
                      disabled={field.disabled}
                    />
                  </label>
                ),
                )}
              </div>
            </>
          )
        )}
        {isAi && (
          <div className="modal-form">
            <label>
              Data da receita
              <input type="date" value={form.date} onChange={update('date')} />
            </label>
            <label>
              Observações
              <textarea value={form.notes} onChange={update('notes')} placeholder="Opcional" />
            </label>
          </div>
        )}
        {submissionError && <small className="form-error">{submissionError}</small>}
        {!canSubmit && (!isMedicationWizard || isLastMedicationStep) && (
          <small className="form-error">
            {isFutureVitals
              ? 'Não é possível registrar sinais vitais no futuro.'
              : isVitals && !vitalValuesValid
                ? vitalType === 'Saturação' && Number(String(form.value).replace(',', '.')) > 100
                  ? 'Informe uma saturação numérica entre 0 e 100.'
                  : 'Informe valores numéricos maiores que zero.'
                : isFutureAlarmInvalid
                  ? 'Escolha uma data e horário futuros para o alarme.'
                  : medicationScheduleInvalid
                    ? 'Para gerar alarmes, informe um início futuro, frequência de 1 a 24 e uma duração válida.'
                    : femaleDateInvalid
                      ? femaleDateError
                      : isFemaleDailyLog && !femaleValue
                        ? 'Registre pelo menos o fluxo, um sintoma, o humor ou uma observação.'
                        : isAi
                          ? 'Escolha um arquivo para iniciar a análise.'
                          : isExam && !file && !hasExistingAttachment
                            ? 'Anexe o resultado do exame para continuar.'
                            : 'Preencha os campos obrigatórios para continuar.'}
          </small>
        )}
        <div className="modal-actions">
          <button className="cancel-btn" disabled={submitting} onClick={close}>
            Cancelar
          </button>
          {isMedicationWizard && medicationStepIndex > 0 && (
            <button
              className="cancel-btn"
              disabled={submitting}
              onClick={() => moveMedicationStep(medicationStepIndex - 1)}
            >
              Voltar
            </button>
          )}
          {isMedicationWizard && !isLastMedicationStep ? (
            <button
              className="primary-btn"
              disabled={submitting}
              onClick={() => moveMedicationStep(medicationStepIndex + 1)}
            >
              Próximo
            </button>
          ) : (
            <button
              className={isConfirm ? 'danger-btn' : 'primary-btn'}
              disabled={!canSubmit || submitting}
              onClick={() => submit({ ...action, data: submitFormData() })}
            >
              {submitting
                ? 'Salvando...'
                : isConfirm
                  ? 'Confirmar'
                  : isAi
                    ? 'Enviar para análise'
                    : isContraceptiveSetup
                      ? 'Salvar anticoncepcional'
                      : action.method === 'PATCH'
                        ? 'Salvar alterações'
                        : 'Salvar registro'}
            </button>
          )}
        </div>
        <small className="sync-note">
          {connection === 'guest'
            ? 'Salvo somente neste dispositivo. Crie uma conta para backup e sincronização.'
            : connection === 'online'
              ? 'Será sincronizado com o servidor.'
              : 'Salvo localmente e sincronizado quando o servidor conectar.'}
        </small>
      </div>
    </div>
  )
}
