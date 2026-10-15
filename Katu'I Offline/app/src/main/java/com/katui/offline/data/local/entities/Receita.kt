package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "receitas")
data class Receita(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val imagemPath: String?,
    val observacao: String?,
    val medicamentoId: Long? = null,
    val dataRegistro: LocalDateTime = LocalDateTime.now(),
    val isPdf: Boolean = false
)
