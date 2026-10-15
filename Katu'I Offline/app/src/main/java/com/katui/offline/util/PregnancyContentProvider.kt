package com.katui.offline.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PregnancyWeekInfo(
    val week: Int,
    val description: String,
    val sizeComparison: String,
    val commonSymptoms: List<String>,
    val recommendedActions: List<String>
)

object PregnancyContentProvider {

    fun calculateGestationalWeek(dum: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(dum, LocalDate.now())
        return (days / 7).toInt().coerceIn(1, 42)
    }

    fun calculateEDD(dum: LocalDate): LocalDate {
        return dum.plusDays(280) // 40 weeks
    }

    fun getWeekInfo(week: Int): PregnancyWeekInfo {
        return when (week) {
            in 1..4 -> PregnancyWeekInfo(
                week,
                "O embrião está se fixando no útero. É o início da formação do sistema nervoso e circulatório.",
                "Grão de arroz",
                listOf("Atraso menstrual", "Cansaço", "Leves cólicas"),
                listOf("Agendar primeira consulta pré-natal", "Iniciar ácido fólico", "Evitar álcool e tabaco")
            )
            in 5..8 -> PregnancyWeekInfo(
                week,
                "O coração começa a bater e os membros começam a brotar. O bebê passa a ser chamado de feto.",
                "Framboesa",
                listOf("Náuseas", "Vômitos", "Mamas sensíveis", "Maior frequência urinária"),
                listOf("Realizar exames de sangue e urina", "Primeiro ultrassom obstétrico")
            )
            in 9..12 -> PregnancyWeekInfo(
                week,
                "O rosto está formado, as pálpebras fechadas e o bebê já se mexe (embora você ainda não sinta).",
                "Limão siciliano",
                listOf("Sono excessivo", "Variações de humor", "Melhora gradual das náuseas"),
                listOf("Ultrassonografia morfológica do 1º trimestre", "Monitorar pressão arterial")
            )
            in 13..16 -> PregnancyWeekInfo(
                week,
                "O bebê já consegue fazer expressões faciais e os órgãos genitais estão em desenvolvimento.",
                "Abacate pequeno",
                listOf("Aumento do apetite", "Melhora da disposição", "Possíveis tonturas"),
                listOf("Manter hidratação", "Alimentação rica em ferro")
            )
            in 17..20 -> PregnancyWeekInfo(
                week,
                "Metade do caminho! O bebê ouve sons externos e seus movimentos ficam mais perceptíveis.",
                "Banana",
                listOf("Início dos movimentos fetais", "Possíveis dores nas costas"),
                listOf("Ultrassonografia Morfológica do 2º trimestre", "Conversar com o bebê")
            )
            in 21..24 -> PregnancyWeekInfo(
                week,
                "O bebê está mais ativo. Os pulmões começam a produzir surfactante para a respiração.",
                "Milho",
                listOf("Azia", "Inchaço leve nos pés", "Cãibras"),
                listOf("Teste de Tolerância à Glicose", "Exercícios leves autorizados")
            )
            in 25..28 -> PregnancyWeekInfo(
                week,
                "Os olhos começam a abrir e o bebê já tem ciclos de sono e vigília definidos.",
                "Berinjela",
                listOf("Dificuldade para dormir", "Mudança no centro de gravidade"),
                listOf("Iniciar consultas quinzenais a partir da 28ª semana")
            )
            in 29..32 -> PregnancyWeekInfo(
                week,
                "O bebê ganha peso rapidamente e se posiciona para o nascimento.",
                "Couve-flor",
                listOf("Falta de ar", "Frequentes idas ao banheiro"),
                listOf("Preparar a mala da maternidade", "Repetir hemograma")
            )
            in 33..36 -> PregnancyWeekInfo(
                week,
                "O desenvolvimento físico está quase completo. O espaço no útero fica mais apertado.",
                "Melão",
                listOf("Inchaço nas pernas", "Contrações de treinamento (Braxton Hicks)"),
                listOf("Consultas semanais a partir da 36ª semana", "Pesquisa de Estreptococo (GBS)")
            )
            else -> PregnancyWeekInfo(
                week,
                "O bebê está pronto para nascer a qualquer momento!",
                "Melancia pequena",
                listOf("Pressão na pelve", "Ansiedade"),
                listOf("Monitorar contrações", "Descansar o máximo possível")
            )
        }
    }
    
    fun getTrimester(week: Int): Int {
        return when {
            week <= 13 -> 1
            week <= 26 -> 2
            else -> 3
        }
    }
}
