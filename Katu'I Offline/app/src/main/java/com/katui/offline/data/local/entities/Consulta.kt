package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "consultas")
data class Consulta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val medico: String,
    val especialidade: String?,
    val local: String?,
    val dataHora: LocalDateTime,
    val observacao: String?
)
