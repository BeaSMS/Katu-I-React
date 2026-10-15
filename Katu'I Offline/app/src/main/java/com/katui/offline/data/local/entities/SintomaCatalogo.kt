package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sintoma_catalogo")
data class SintomaCatalogo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val categoria: String?,
    val popular: Boolean = false
)

@Entity(
    tableName = "efeito_colateral_catalogo",
    primaryKeys = ["medicamentoId", "sintomaId"]
)
data class EfeitoColateralCatalogo(
    val medicamentoId: Long,
    val sintomaId: Long,
    val frequencia: String = "Comum" // Comum, Muito Comum
)
