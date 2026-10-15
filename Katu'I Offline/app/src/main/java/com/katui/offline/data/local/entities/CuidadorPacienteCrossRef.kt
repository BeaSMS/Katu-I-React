package com.katui.offline.data.local.entities

import androidx.room.Entity

@Entity(primaryKeys = ["cuidadorId", "pacienteId"], tableName = "cuidador_paciente")
data class CuidadorPacienteCrossRef(
    val cuidadorId: Long,
    val pacienteId: Long
)
