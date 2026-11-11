import { Icon, EmptyState } from './interfaceCompartilhada'

export function AppointmentsPage({ onAction }) {
  return (
    <div className="module-grid appointments-layout">
      <div className="panel wide-panel">
        <div className="panel-header">
          <div>
            <h3>Próximos compromissos</h3>
            <p>Seus compromissos aparecerão aqui.</p>
          </div>
        </div>
        <EmptyState
          icon="calendar"
          title="Nenhuma consulta agendada"
          message="Adicione sua primeira consulta ou exame para organizar sua agenda de saúde."
          action="Agendar consulta"
          onAction={() => onAction({ kind: 'form', title: 'Agendar consulta', endpoint: '/appointments' })}
        />
      </div>
      <div className="panel reminder-panel">
        <h3>Lembretes</h3>
        <p>Seus lembretes aparecerão aqui.</p>
        <EmptyState icon="bell" title="Sem lembretes" message="Nenhum lembrete foi configurado." />
      </div>
    </div>
  )
}

export function DocumentsPage({ onAction }) {
  return (
    <div className="module-grid documents-layout">
      <div className="panel wide-panel">
        <div className="panel-header">
          <div>
            <h3>Arquivos recentes</h3>
            <p>Seus documentos aparecerão aqui depois do primeiro envio.</p>
          </div>
        </div>
        <EmptyState
          icon="file"
          title="Nenhum documento adicionado"
          message="Envie receitas, exames ou outros arquivos para mantê-los organizados."
          action="Adicionar documento"
          onAction={() => onAction({ kind: 'form', title: 'Adicionar documento', endpoint: '/documents' })}
        />
      </div>
      <div className="panel ai-panel">
        <div className="ai-spark">✦</div>
        <h3>Leitura inteligente</h3>
        <p>Envie uma receita e o Katu’I ajuda a organizar os medicamentos.</p>
        <button
          className="soft-btn ai-btn"
          onClick={() =>
            onAction({ kind: 'ai', title: 'Leitura inteligente de receita', endpoint: '/ai/prescription' })
          }
        >
          ✦ Ler receita com IA <Icon name="arrow" size={14} />
        </button>
        <button
          className="outline-btn report-btn"
          onClick={() => onAction({ kind: 'form', title: 'Criar relatório PDF', endpoint: '/reports' })}
        >
          <Icon name="file" size={14} /> Criar relatório PDF
        </button>
      </div>
    </div>
  )
}
