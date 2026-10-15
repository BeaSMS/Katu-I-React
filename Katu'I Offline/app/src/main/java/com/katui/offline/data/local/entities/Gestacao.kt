package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "gestacoes")
data class Gestacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val dataUltimaMenstruacao: LocalDate? = null,
    val dataPartoEstimada: LocalDate? = null,
    val nomeBebe: String? = null,
    val ativa: Boolean = true
)
