package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicamento_catalogo")
data class MedicamentoCatalogo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val principioAtivo: String,
    val laboratorio: String?,
    val registroMS: String?,
    val classeTerapeutica: String?,
    val formaFarmaceutica: String? // Ex: Comprimido, Xarope, Gotas
)
