import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './styles.css'
import { initializeSecureStorage } from './services/secureStorage'

const root = createRoot(document.getElementById('root'))

initializeSecureStorage()
  .then(() => {
    root.render(
      <StrictMode>
        <App />
      </StrictMode>,
    )
  })
  .catch((error) => {
    console.error('Falha ao inicializar o armazenamento local protegido.', error)
    root.render(
      <main role="alert" style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
        <h1>Não foi possível abrir o Katu’I</h1>
        <p>
          Este navegador não conseguiu inicializar o armazenamento local protegido. Atualize o navegador e tente
          novamente.
        </p>
      </main>,
    )
  })
