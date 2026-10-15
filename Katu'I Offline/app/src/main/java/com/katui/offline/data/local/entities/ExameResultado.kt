package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exame_resultados",
    foreignKeys = [
        ForeignKey(
            entity = Exame::class,
            parentColumns = ["id"],
            childColumns = ["exameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exameId")]
)
data class ExameResultado(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exameId: Long,
    val metrica: String, // ex: "Glicose", "Colesterol LDL"
    val valor: Double,
    val unidade: String?, // ex: "mg/dL"
    val referencia: String? = null // ex: "70 - 99"
)
