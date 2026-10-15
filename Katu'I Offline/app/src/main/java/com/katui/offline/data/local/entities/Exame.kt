package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "exames")
data class Exame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val categoria: String = "Outros",
    val arquivoPath: String?,
    val observacao: String?,
    val dataRegistro: LocalDateTime = LocalDateTime.now(),
    val isPdf: Boolean = false
)
