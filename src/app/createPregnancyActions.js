export function createPregnancyActions({ storageKey, startDate, setPregnancy, setPregnancyConfirming, setPregnancyError, setStartDate, onPregnancyChange }) {
  const requestActivation = () => {
    if (!startDate) {
      setPregnancyError('Informe a data que será usada como início do acompanhamento.')
      return
    }
    setPregnancyError('')
    setPregnancyConfirming(true)
  }
  const confirmActivation = async () => {
    const next = { enabled: true, startDate }
    localStorage.setItem(storageKey, JSON.stringify(next))
    setPregnancy(next)
    setPregnancyConfirming(false)
    await onPregnancyChange(next)
  }
  const disablePregnancy = async () => {
    if (!window.confirm('Desativar o acompanhamento de gestação deste perfil?')) return
    const next = { enabled: false, startDate: '' }
    localStorage.setItem(storageKey, JSON.stringify(next))
    setPregnancy(next)
    setStartDate('')
    await onPregnancyChange(next)
  }
  return { requestActivation, confirmActivation, disablePregnancy }
}
