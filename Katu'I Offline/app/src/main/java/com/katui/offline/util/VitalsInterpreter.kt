package com.katui.offline.util

import androidx.compose.ui.graphics.Color
import com.katui.offline.data.local.entities.Vitals
import com.katui.offline.ui.theme.*

enum class VitalsStatus(val label: String, val color: Color) {
    NORMAL("Normal", Color(0xFF4CAF50)),
    ALTENCAO("Atenção", Color(0xFFFF9800)),
    ALTA("Alta", Color(0xFFF44336)),
    BAIXA("Baixa", Color(0xFF2196F3)),
    CRITICA("Crítica", Color(0xFFB71C1C)),
    DESCONHECIDO("", Color.Gray)
}

object VitalsInterpreter {

    fun getPressureStatus(sistolica: Int?, diastolica: Int?): VitalsStatus {
        if (sistolica == null || diastolica == null) return VitalsStatus.DESCONHECIDO
        
        // Conversão interna apenas para o cálculo de status (caso não tenha sido corrigido no ViewModel)
        val s = if (sistolica > 0 && sistolica < 40) sistolica * 10 else sistolica
        val d = if (diastolica > 0 && diastolica < 40) diastolica * 10 else diastolica

        return when {
            s >= 180 || d >= 120 -> VitalsStatus.CRITICA
            s >= 140 || d >= 90 -> VitalsStatus.ALTA
            // 120-139/80-89 costuma ser considerado pré-hipertensão/atenção.
            // 120/80 é o limite superior do normal, mantemos normal se for EXATAMENTE 120/80 ou menos.
            s > 130 || d > 85 -> VitalsStatus.ALTENCAO
            s < 90 || d < 60 -> VitalsStatus.BAIXA
            else -> VitalsStatus.NORMAL
        }
    }

    fun getGlucoseStatus(glicose: Int?): VitalsStatus {
        if (glicose == null) return VitalsStatus.DESCONHECIDO
        
        // Valores de referência simplificados (Jejum)
        return when {
            glicose >= 126 -> VitalsStatus.ALTA
            glicose >= 100 -> VitalsStatus.ALTENCAO
            glicose < 70 -> VitalsStatus.BAIXA
            else -> VitalsStatus.NORMAL
        }
    }

    fun getSpO2Status(spo2: Int?): VitalsStatus {
        if (spo2 == null) return VitalsStatus.DESCONHECIDO
        
        return when {
            spo2 < 90 -> VitalsStatus.CRITICA
            spo2 < 95 -> VitalsStatus.ALTENCAO
            else -> VitalsStatus.NORMAL
        }
    }

    fun getTempStatus(temp: Double?): VitalsStatus {
        if (temp == null) return VitalsStatus.DESCONHECIDO
        
        return when {
            temp >= 38.5 -> VitalsStatus.ALTA
            temp >= 37.3 -> VitalsStatus.ALTENCAO
            temp < 35.0 -> VitalsStatus.BAIXA
            else -> VitalsStatus.NORMAL
        }
    }
}
