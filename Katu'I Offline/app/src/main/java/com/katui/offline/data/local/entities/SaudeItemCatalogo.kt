package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoSaudeItem {
    ALERGIA,
    CONDICAO
}

@Entity(tableName = "saude_catalogo")
data class SaudeItemCatalogo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val categoria: String,
    val tipo: TipoSaudeItem
)
