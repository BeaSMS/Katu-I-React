package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sintomas")
data class Sintoma(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val catalogoId: Long? = null, // Link para SintomaCatalogo (3NF)
    val localizacao: String?,
    val qualidade: String?, // Nome do sintoma (ex: "Dor de cabeça")
    val intensidadeEscala: Int?,
    val incapacitante: Boolean?,
    val padraoTempo: String?,
    val fatoresAssociados: String?,
    val impactoFuncional: String?,
    val tipo: String = "FISICO", // "FISICO" ou "PSICOLOGICO"
    val dataHoraRegistro: LocalDateTime = LocalDateTime.now()
)
