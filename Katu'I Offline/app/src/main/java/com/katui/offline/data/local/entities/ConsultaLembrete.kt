package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class TipoLembreteConsulta {
    RELATIVO, // disparado X minutos antes do horário da consulta
    ABSOLUTO  // disparado num horário fixo, escolhido pelo usuário
}

enum class MetodoNotificacao {
    NOTIFICACAO, // Notificação silenciosa/padrão
    ALARME       // Alarme com som/tela cheia
}

/**
 * Um lembrete configurável de uma consulta. Uma consulta pode ter zero, um, ou
 * vários lembretes (ex: "1 dia antes às 12h" + "3h antes" + "no horário exato"
 * + "30 min antes, de novo, porque a pessoa esquece fácil").
 *
 * RELATIVO: minutosAntes é obrigatório, horarioAbsoluto é ignorado. O horário
 * real de disparo é sempre recalculado a partir de Consulta.dataHora -
 * minutosAntes, então se a consulta for reagendada, os lembretes relativos
 * acompanham automaticamente (basta re-agendar via ConsultationScheduler).
 *
 * ABSOLUTO: horarioAbsoluto é obrigatório, minutosAntes é ignorado. Útil para
 * "sempre às 8h do dia da consulta, independente do horário marcado" ou
 * "às 19h da véspera para arrumar a mala do exame".
 */
@Entity(
    tableName = "consulta_lembretes",
    foreignKeys = [
        ForeignKey(
            entity = Consulta::class,
            parentColumns = ["id"],
            childColumns = ["consultaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("consultaId")]
)
data class ConsultaLembrete(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val consultaId: Long,
    val tipo: TipoLembreteConsulta,
    val metodo: MetodoNotificacao = MetodoNotificacao.NOTIFICACAO,
    val minutosAntes: Int? = null,
    val horarioAbsoluto: LocalDateTime? = null,
    // Rótulo opcional para o usuário identificar o lembrete (ex: "Levar exames")
    val rotulo: String? = null
)
