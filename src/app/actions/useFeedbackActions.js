
export function useFeedbackActions(context) {
  const { audioFeedbackEnabled, setAudioFeedbackEnabled, setFeedbackMessage, audioContextRef } = context
  const playFeedbackTone = (kind, enabled = audioFeedbackEnabled) => {
    if (!enabled) return
    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext
    if (!AudioContextConstructor) {
      setFeedbackMessage('O navegador não oferece áudio; o feedback visual continua ativo.')
      return
    }
    try {
      const context = audioContextRef.current || new AudioContextConstructor()
      audioContextRef.current = context
      const oscillator = context.createOscillator()
      const gain = context.createGain()
      const now = context.currentTime
      oscillator.type = 'sine'
      oscillator.frequency.value = kind === 'error' ? 220 : kind === 'undo' ? 520 : 660
      gain.gain.setValueAtTime(0.0001, now)
      gain.gain.exponentialRampToValueAtTime(0.07, now + 0.015)
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.16)
      oscillator.connect(gain)
      gain.connect(context.destination)
      context.resume().then(() => {
        oscillator.start()
        oscillator.stop(context.currentTime + 0.17)
      }).catch(() => setFeedbackMessage('O som não pôde ser reproduzido. O feedback visual continua ativo.'))
    } catch {
      setFeedbackMessage('O som não pôde ser reproduzido. O feedback visual continua ativo.')
    }
  }
  const toggleAudioFeedback = () => {
    const enabled = !audioFeedbackEnabled
    setAudioFeedbackEnabled(enabled)
    localStorage.setItem('katui.audio-feedback', String(enabled))
    if (enabled) {
      setFeedbackMessage('Feedback sonoro ativado.')
      playFeedbackTone('success', true)
    } else {
      setFeedbackMessage('Feedback sonoro desativado.')
    }
  }
  return { playFeedbackTone, toggleAudioFeedback }
}
