package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "vitals")
data class Vitals(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val dataHora: LocalDateTime = LocalDateTime.now(),
    val peso: Double? = null,
    val alturaCm: Int? = null,
    val sistolica: Int? = null, // Pressão Arterial Sistólica
    val diastolica: Int? = null, // Pressão Arterial Diastólica
    val frequenciaCardiaca: Int? = null,
    val glicose: Int? = null,
    val saturacaoOxigenio: Int? = null,
    val temperatura: Double? = null,
    val observacoes: String? = null
)
