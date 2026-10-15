package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "medicamentos")
data class Medicamento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val catalogoId: Long? = null, // Link para MedicamentoCatalogo (3NF)
    val nome: String,
    val horario: String, // ex: "08:00"
    val tipoFrequencia: String, // INTERVALO_HORAS, VEZES_DIA, DIAS_ESPECIFICOS
    val valorFrequencia: Int?,
    val dias: Int?,
    val finalidade: String?,
    val dosagem: String?, // Descritivo (ex: "500mg")
    val dataInicio: LocalDate?,
    val dataFim: LocalDate?,
    val observacoes: String?,
    val diasSemana: String? = null,
    val ativo: Boolean = true,
    
    // Novos campos para Estoque Inteligente
    val estoqueAtual: Double = 0.0,
    val estoqueMinimo: Double = 0.0,
    val dosePorTomada: Double = 1.0,
    val unidadeMedida: String = "un" // ex: "un", "ml", "gotas"
)
