package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoUsuario {
    PACIENTE,
    CUIDADOR
}

enum class SexoBiologico {
    MASCULINO,
    FEMININO,
    INTERSEXO,
    NAO_INFORMADO
}

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val email: String? = null,
    val senha: String? = null,
    val telefone: String? = null,
    val peso: Double? = null,
    val alturaCm: Int? = null,
    val idade: Int? = null,
    val sexo: SexoBiologico = SexoBiologico.NAO_INFORMADO,
    val alergias: String? = null,
    val condicoesEspeciais: String? = null,
    val tipo: TipoUsuario = TipoUsuario.PACIENTE,
    val cuidadorId: Long? = null, // ID of the primary caregiver if this is a dependent
    val corHex: String? = null // Cor personalizada em formato Hex (ex: #FF5722)
)
