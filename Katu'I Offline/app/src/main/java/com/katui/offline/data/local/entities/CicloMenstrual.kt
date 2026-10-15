package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "ciclos_menstruais")
data class CicloMenstrual(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val dataInicio: LocalDate,
    val dataFim: LocalDate? = null,
    val intensidade: IntensidadeFluxo = IntensidadeFluxo.MEDIA,
    val sintomas: String? = null, // JSON ou string: "COLICA,MAU_HUMOR"
    val humor: String? = null,     // "FELIZ,IRRITADA,TRISTE"
    val mucoCervical: String? = null, // "CLARA_OVO,CREMOSO"
    val observacoes: String? = null
)

@Entity(tableName = "sintomas_diarios_femininos")
data class SintomaDiarioFeminino(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val data: LocalDate,
    val intensidade: IntensidadeFluxo = IntensidadeFluxo.MEDIA,
    val sintomas: String? = null,
    val humor: String? = null,
    val mucoCervical: String? = null,
    val observacoes: String? = null
)

@Entity(tableName = "anticoncepcionais")
data class Anticoncepcional(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val tipoCartela: Int, // 21, 24, 28, 0 (contínuo)
    val dataInicio: LocalDate,
    val horarioTomada: String, // "HH:mm"
    val ativo: Boolean = true
)

@Entity(tableName = "registro_anticoncepcional")
data class RegistroAnticoncepcional(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val anticoncepcionalId: Long,
    val data: LocalDate,
    val tomado: Boolean = true,
    val horarioEfetivo: String? = null
)

enum class IntensidadeFluxo {
    LEVE, MEDIA, FORTE
}
