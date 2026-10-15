package com.katui.offline.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Science

object ExamCategorizer {
    
    val CATEGORIES = listOf(
        "Sangue",
        "Imagem",
        "Cardiológico",
        "Urina/Fezes",
        "Hormonal",
        "Infeccioso",
        "Odontológico",
        "Outros"
    )

    // Mapa de especialidade médica → categorias de exame priorizadas
    val ESPECIALIDADES = listOf(
        "Clínico Geral",
        "Cardiologista",
        "Endocrinologista",
        "Ortopedista",
        "Ginecologista",
        "Urologista",
        "Neurologista",
        "Gastroenterologista",
        "Infectologista",
        "Odontologista",
        "Outro"
    )

    /** Retorna as categorias de exame mais relevantes para uma especialidade. */
    fun categoriasPorEspecialidade(especialidade: String): List<String> = when (especialidade) {
        "Cardiologista"        -> listOf("Cardiológico", "Sangue", "Imagem")
        "Endocrinologista"     -> listOf("Hormonal", "Sangue")
        "Ortopedista"         -> listOf("Imagem", "Sangue")
        "Ginecologista"       -> listOf("Hormonal", "Imagem", "Infeccioso", "Sangue")
        "Urologista"          -> listOf("Urina/Fezes", "Sangue", "Hormonal")
        "Neurologista"        -> listOf("Imagem", "Sangue")
        "Gastroenterologista" -> listOf("Urina/Fezes", "Sangue", "Imagem")
        "Infectologista"      -> listOf("Infeccioso", "Sangue")
        "Odontologista"       -> listOf("Odontológico", "Imagem")
        else                  -> CATEGORIES // Clínico Geral / Outro: todos
    }

    private val KEYWORDS = mapOf(
        "Sangue" to listOf("hemograma", "glicemia", "colesterol", "triglic", "ferro", "hemoglobina", "plaquetas", "tgo", "tgp", "creatinina", "ureia", "ácido úrico", "vhs", "pcr", "vitamina", "tsh", "t4"),
        "Imagem" to listOf("raio-x", "raio x", "rx", "ultrassom", "ultrassonografia", "eco", "ressonância", "tomografia", "mamografia", "densitometria", "endoscopia", "colonoscopia", "cintilografia"),
        "Cardiológico" to listOf("eletrocardiograma", "ecocardiograma", "holter", "mapa", "teste ergométrico", "eletro", "ecg"),
        "Urina/Fezes" to listOf("urina", "eas", "urocultura", "fezes", "parasitológico", "coprocultura", "sangue oculto", "sumário de urina"),
        "Hormonal" to listOf("tsh", "t4", "t3", "psa", "hcg", "insulina", "prolactina", "cortisol", "estradiol", "progesterona", "lh", "fsh", "testosterona", "progesterona", "shbg"),
        "Infeccioso" to listOf("hiv", "sífilis", "vdrl", "hepatite", "dengue", "toxoplasmose", "rubéola", "cmv", "chagas", "covid", "sorologia"),
        "Odontológico" to listOf("panorâmica", "periapical", "dental", "odontológico", "dente")
    )

    /** Retorna valores de referência típicos (mínimo e máximo) para métricas comuns. */
    fun getNormalRange(metric: String): Pair<Double, Double>? {
        return when (metric.lowercase()) {
            "glicose", "glicemia" -> 70.0 to 99.0
            "colesterol total" -> 0.0 to 190.0
            "colesterol ldl" -> 0.0 to 130.0
            "colesterol hdl" -> 40.0 to 100.0
            "triglicerídeos" -> 0.0 to 150.0
            "hemoglobina" -> 12.0 to 17.0
            "creatinina" -> 0.6 to 1.3
            "tsh" -> 0.4 to 4.5
            "t4 livre" -> 0.8 to 1.8
            "ferritina" -> 30.0 to 400.0
            else -> null
        }
    }

    fun suggestCategory(name: String): String {
        val lowerName = name.lowercase()
        for ((category, keywords) in KEYWORDS) {
            if (keywords.any { lowerName.contains(it) }) {
                return category
            }
        }
        return "Outros"
    }

    fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
        return when (category) {
            "Sangue" -> Icons.Default.Opacity
            "Imagem" -> Icons.Default.Image
            "Cardiológico" -> Icons.Default.MonitorHeart
            "Urina/Fezes" -> Icons.Default.Science
            "Hormonal" -> Icons.Default.Balance
            "Infeccioso" -> Icons.Default.Coronavirus
            "Odontológico" -> Icons.Default.MedicalServices
            else -> Icons.AutoMirrored.Filled.Assignment
        }
    }

    /** Cor temática associada a cada categoria, para usar junto com o ícone de getCategoryIcon. */
    fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
        return when (category) {
            "Sangue" -> androidx.compose.ui.graphics.Color(0xFFE53935)
            "Imagem" -> androidx.compose.ui.graphics.Color(0xFF1E88E5)
            "Cardiológico" -> androidx.compose.ui.graphics.Color(0xFFE91E63)
            "Urina/Fezes" -> androidx.compose.ui.graphics.Color(0xFFFFA000)
            "Hormonal" -> androidx.compose.ui.graphics.Color(0xFF8E24AA)
            "Infeccioso" -> androidx.compose.ui.graphics.Color(0xFF43A047)
            "Odontológico" -> androidx.compose.ui.graphics.Color(0xFF00ACC1)
            else -> androidx.compose.ui.graphics.Color(0xFF78909C)
        }
    }

    fun getSuggestedMetrics(category: String): List<Pair<String, String>> {
        return when (category) {
            "Sangue" -> listOf(
                "Glicose" to "mg/dL",
                "Colesterol Total" to "mg/dL",
                "Colesterol LDL" to "mg/dL",
                "Colesterol HDL" to "mg/dL",
                "Triglicerídeos" to "mg/dL",
                "Hemoglobina" to "g/dL",
                "Ferritina" to "ng/mL",
                "Creatinina" to "mg/dL",
                "TGO" to "U/L",
                "TGP" to "U/L"
            )
            "Cardiológico" -> listOf(
                "Freq. Cardíaca" to "bpm",
                "Pressão Sistólica" to "mmHg",
                "Pressão Diastólica" to "mmHg"
            )
            "Hormonal" -> listOf(
                "TSH" to "mIU/L",
                "T4 Livre" to "ng/dL",
                "PSA Total" to "ng/mL",
                "Beta-HCG" to "mUI/mL",
                "Cortisol" to "µg/dL"
            )
            "Urina/Fezes" -> listOf(
                "Proteínas" to "mg/dL",
                "pH" to "",
                "Densidade" to ""
            )
            else -> emptyList()
        }
    }
}
