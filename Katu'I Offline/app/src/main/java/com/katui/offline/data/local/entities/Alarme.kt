package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "alarmes")
data class Alarme(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val medicamentoId: Long?,
    val horario: LocalDateTime,
    val titulo: String,
    val tomado: Boolean = false,
    val somAlarme: String? = null
)
