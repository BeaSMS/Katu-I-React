package com.katui.offline.util

object MedicalUnitConverter {

    /**
     * Unidades padrão usadas pelo aplicativo (baseadas no ExamCategorizer).
     */
    val STANDARD_UNITS = mapOf(
        "Glicose" to "mg/dL",
        "Glicemia" to "mg/dL",
        "Colesterol Total" to "mg/dL",
        "Colesterol LDL" to "mg/dL",
        "Colesterol HDL" to "mg/dL",
        "Triglicerídeos" to "mg/dL",
        "Hemoglobina" to "g/dL",
        "Creatinina" to "mg/dL",
        "T4 Livre" to "ng/dL",
        "TSH" to "mIU/L",
        "Ferritina" to "ng/mL"
    )

    /**
     * Normaliza um valor para a unidade padrão do aplicativo.
     * @param value O valor original.
     * @param fromUnit A unidade original (ex: "mmol/L").
     * @param metric O nome da métrica (ex: "Glicose").
     * @return O valor convertido para a unidade padrão.
     */
    fun normalize(value: Double, fromUnit: String?, metric: String): Double {
        if (fromUnit == null) return value
        
        val targetUnit = STANDARD_UNITS[metric] ?: return value
        val unitClean = fromUnit.lowercase().trim().replace(" ", "")
        val targetClean = targetUnit.lowercase().trim().replace(" ", "")

        if (unitClean == targetClean) return value

        return when (metric.lowercase()) {
            "glicose", "glicemia" -> convertGlucose(value, unitClean)
            "colesterol total", "colesterol ldl", "colesterol hdl" -> convertCholesterol(value, unitClean)
            "triglicerídeos" -> convertTriglycerides(value, unitClean)
            "hemoglobina" -> convertHemoglobin(value, unitClean)
            "creatinina" -> convertCreatinine(value, unitClean)
            "t4 livre" -> convertT4(value, unitClean)
            else -> value
        }
    }

    private fun convertGlucose(value: Double, unit: String): Double {
        return when (unit) {
            "mmol/l" -> value * 18.016
            else -> value
        }
    }

    private fun convertCholesterol(value: Double, unit: String): Double {
        return when (unit) {
            "mmol/l" -> value * 38.67
            else -> value
        }
    }

    private fun convertTriglycerides(value: Double, unit: String): Double {
        return when (unit) {
            "mmol/l" -> value * 88.57
            else -> value
        }
    }

    private fun convertHemoglobin(value: Double, unit: String): Double {
        return when (unit) {
            "g/l" -> value / 10.0
            "mmol/l" -> value * 1.611 // Aproximado para hemoglobina
            else -> value
        }
    }

    private fun convertCreatinine(value: Double, unit: String): Double {
        return when (unit) {
            "µmol/l", "umol/l" -> value / 88.4
            "mmol/l" -> value / 0.0884
            else -> value
        }
    }

    private fun convertT4(value: Double, unit: String): Double {
        return when (unit) {
            "pmol/l" -> value / 12.87
            else -> value
        }
    }

    /**
     * Tenta extrair o intervalo de referência (mín e máx) de uma string.
     * Ex: "70 - 99", "Inferior a 150", "> 40"
     */
    fun parseReferenceRange(ref: String?): Pair<Double, Double>? {
        if (ref == null) return null
        
        try {
            // Caso 1: "70 - 99" ou "70 a 99"
            val parts = ref.split(Regex("[-a]")).map { it.trim().replace(",", ".").filter { char -> char.isDigit() || char == '.' } }
            if (parts.size == 2) {
                val min = parts[0].toDoubleOrNull()
                val max = parts[1].toDoubleOrNull()
                if (min != null && max != null) return min to max
            }

            // Caso 2: "Inferior a 150" ou "< 150"
            if (ref.contains("<") || ref.lowercase().contains("inferior")) {
                val max = ref.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".").toDoubleOrNull()
                if (max != null) return 0.0 to max
            }

            // Caso 3: "> 40" ou "Superior a 40"
            if (ref.contains(">") || ref.lowercase().contains("superior")) {
                val min = ref.filter { it.isDigit() || it == '.' || it == ',' }.replace(",", ".").toDoubleOrNull()
                if (min != null) return min to (min * 2) // Dummy max
            }
        } catch (e: Exception) {
            return null
        }
        
        return null
    }
}
