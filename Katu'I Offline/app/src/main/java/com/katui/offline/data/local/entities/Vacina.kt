package com.katui.offline.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "vacinas")
data class Vacina(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val dose: String,        // "1ª Dose", "2ª Dose", "Reforço 1", "Anual 2025", etc.
    val categoria: String,   // "Criança", "Adolescente", "Adulto", "Idoso", "Anual", "Outras"
    val observacao: String? = null,
    val tomada: Boolean = false,
    val dataAgendada: LocalDate? = null,
    val dataTomada: LocalDate? = null,
    val recorrente: Boolean = false,   // Vacina que se repete periodicamente
    val mesesParaReforco: Int? = null, // Intervalo em meses (12 = anual, 120 = 10 anos, 6 = semestral)
    val mesCampanha: Int? = null,      // Mês (1–12) da campanha nacional. Ex: 4 = Influenza em abril
    // ── Campos para vacinas com múltiplas doses ───────────────────────────────
    // Agrupa doses de uma mesma vacina no card (ex: "COVID-19", "Hepatite B")
    val grupoVacina: String? = null,
    // Ordem da dose dentro do grupo (1 = 1ª dose, 2 = 2ª, etc.) para exibição ordenada
    val ordemDose: Int = 0,
    // Se true, o usuário pode adicionar mais doses manualmente (ex: reforços de COVID)
    val permiteNovasDoses: Boolean = false
)
