import { demoCredentials } from '../../services/api/demoData'

export function DemoAccessButton({ onClick }) {
  return (
    <button className="outline-btn auth-secondary-button" onClick={onClick}>
      Experimentar protótipo
    </button>
  )
}

export function DemoCredentials({ onFill }) {
  return (
    <div className="demo-credentials">
      <strong>Conta de demonstração</strong>
      <span>Usuário: {demoCredentials.email}</span>
      <span>Senha: {demoCredentials.password}</span>
      <button type="button" className="auth-switch" onClick={onFill}>
        Preencher acesso fictício
      </button>
    </div>
  )
}
