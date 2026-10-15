package com.katui.offline.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.katui.offline.data.SecureStorage
import com.katui.offline.data.local.entities.ExameResultado
import com.katui.offline.data.local.entities.Medicamento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OCRManager @Inject constructor(private val secureStorage: SecureStorage) {

    // --- Prompt compartilhado para receitas ---
    private val medicationPrompt = """
        Você é um assistente médico especialista em ler receitas brasileiras. 
        Extraia a data da receita e os medicamentos da imagem.
        Retorne APENAS um JSON no formato exato:
        {
          "dataReceita": "String (formato YYYY-MM-DD)",
          "medicamentos": [
            {
              "nome": "String",
              "horario": "String (formato HH:mm)",
              "dosagem": "String (ex: 500mg)",
              "finalidade": "String",
              "tipoFrequencia": "INTERVALO_HORAS ou VEZES_DIA",
              "valorFrequencia": Integer,
              "dias": Integer,
              "observacoes": "String",
              "estoqueAtual": Double (Quantidade total prescrita),
              "dosePorTomada": Double (Quantidade por dose, ex: 1.0, 2.5),
              "unidadeMedida": "String (ex: comprimido, gotas, ml, caps)"
            }
          ]
        }
        Se não encontrar a data, use a data de hoje.
        Se não encontrar o horário, use "08:00".
        Se não tiver certeza de um campo numérico, use 0 — EXCETO para "dias": se a receita
        não especificar por quantos dias o tratamento dura (uso contínuo, ex: medicação
        crônica/diária sem data de término), retorne "dias": null. Não use 0 para "dias"
        nesse caso, pois 0 significa "tratamento já terminado hoje", o que é diferente de
        "sem prazo definido".
        Não escreva nenhum texto explicativo, apenas o JSON puro.
    """.trimIndent()

    // --- Prompt compartilhado para exames ---
    private val examPrompt = """
        Você é um assistente médico especialista em ler laudos de exames brasileiros.
        Extraia as principais métricas e valores encontrados na imagem (ex: Glicose, Colesterol, Hemoglobina).
        Retorne APENAS um JSON no formato:
        [
          {
            "metrica": "String",
            "valor": Double,
            "unidade": "String (ex: mg/dL, g/dL, %)",
            "referencia": "String (ex: 70 - 99)"
          }
        ]
        Não escreva texto explicativo, apenas o JSON puro. Se não encontrar valores, retorne [].
    """.trimIndent()

    // -------------------------------------------------------------------------
    // EXTRAÇÃO DE MEDICAMENTOS
    // -------------------------------------------------------------------------

    suspend fun extractMedicationsFromImage(context: Context, uri: Uri, usuarioId: Long): List<Medicamento> {
        Log.d("KATUI_OCR", "--- INÍCIO DO PROCESSO DE EXTRAÇÃO DE MEDICAMENTOS ---")

        val bitmap = loadBitmap(context, uri)
        val provider = secureStorage.getOcrProvider()
        Log.d("KATUI_OCR", "Provider selecionado: $provider")

        val jsonText = when (provider) {
            "openrouter" -> callOpenRouter(bitmap, medicationPrompt)
            "mistral" -> callMistral(bitmap, medicationPrompt)
            else -> callGemini(bitmap, medicationPrompt)
        }

        Log.d("KATUI_OCR", "Parseando resposta...")
        val jsonString = jsonText.trim().removePrefix("```json").removeSuffix("```").trim()

        val result = try {
            Json { ignoreUnknownKeys = true }.decodeFromString<OCRResult>(jsonString)
        } catch (e: Exception) {
            Log.e("KATUI_OCR", "Erro de parsing JSON", e)
            throw Exception("A IA retornou um formato inválido. Tente novamente.")
        }

        Log.d("KATUI_OCR", "${result.medicamentos.size} medicamentos extraídos")
        return result.medicamentos.map { it.toMedicamento(usuarioId, result.dataReceita) }
    }

    // -------------------------------------------------------------------------
    // EXTRAÇÃO DE EXAMES
    // -------------------------------------------------------------------------

    suspend fun extractExamMetrics(context: Context, uri: Uri): List<ExameResultado> {
        Log.d("KATUI_OCR", "--- INÍCIO DO PROCESSO DE EXTRAÇÃO DE EXAMES ---")
        val bitmap = loadBitmap(context, uri)
        val provider = secureStorage.getOcrProvider()
        Log.d("KATUI_OCR", "Provider selecionado: $provider")

        val jsonText = when (provider) {
            "openrouter" -> callOpenRouter(bitmap, examPrompt)
            "mistral" -> callMistral(bitmap, examPrompt)
            else -> callGemini(bitmap, examPrompt)
        }

        val jsonString = jsonText.trim().removePrefix("```json").removeSuffix("```").trim()

        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<ExtractedMetric>>(jsonString)
                .map { it.toExameResultado() }
        } catch (e: Exception) {
            Log.e("KATUI_OCR", "JSON inválido retornado pela IA", e)
            throw Exception("A IA retornou uma resposta em formato inesperado. Tente novamente ou adicione os valores manualmente.")
        }
    }

    // -------------------------------------------------------------------------
    // GEMINI (SDK nativo)
    // -------------------------------------------------------------------------

    private suspend fun callGemini(bitmap: Bitmap, prompt: String): String {
        val apiKey = secureStorage.getGeminiKey()
        if (apiKey.isNullOrBlank()) throw Exception("Chave do Gemini não configurada. Insira uma chave nas Configurações.")

        // Trocado para "gemini-2.5-flash", o modelo estável (GA) atual com suporte multimodal
        val model = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey)
        return try {
            val response = model.generateContent(content { image(bitmap); text(prompt) })
            response.text ?: throw Exception("A IA não conseguiu processar esta imagem.")
        } catch (e: com.google.ai.client.generativeai.type.ResponseStoppedException) {
            throw Exception("O conteúdo foi bloqueado por motivos de segurança da IA.")
        } catch (e: com.google.ai.client.generativeai.type.ServerException) {
            val msg = e.message ?: ""
            when {
                msg.contains("404") || msg.contains("NOT_FOUND") ->
                    throw Exception("Modelo Gemini indisponível. Atualize o aplicativo.")
                msg.contains("503") || msg.contains("UNAVAILABLE") ->
                    throw Exception("Servidor do Google com alta demanda. Tente novamente em breve.")
                else -> throw Exception("Erro no servidor do Google. Verifique sua conexão.")
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                msg.contains("429") || msg.contains("Quota") -> throw Exception("Limite de uso atingido. Tente em 1 minuto.")
                msg.contains("401") || msg.contains("API key") -> throw Exception("Sua Gemini API Key é inválida ou expirou.")
                else -> throw Exception("Erro Gemini: ${e.localizedMessage}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // OPENROUTER (HTTP direto — spec OpenAI, suporte multimodal)
    // -------------------------------------------------------------------------

    private suspend fun callOpenRouter(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = secureStorage.getOpenRouterKey()
        if (apiKey.isNullOrBlank()) throw Exception("Chave do OpenRouter não configurada. Insira uma chave nas Configurações.")

        val model = secureStorage.getOpenRouterModel()
        Log.d("KATUI_OCR", "OpenRouter: modelo=$model")

        // Converte bitmap para base64 JPEG
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteStream)
        val base64Image = Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)

        // Monta body no formato OpenAI (vision)
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                    })
                })
            })
        }.toString()

        val url = URL("https://openrouter.ai/api/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("HTTP-Referer", "https://katui.app")
            conn.setRequestProperty("X-Title", "Katu'I")
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e("KATUI_OCR", "OpenRouter erro $code: $error")
                when (code) {
                    401 -> throw Exception("Chave do OpenRouter inválida ou expirada. Verifique nas Configurações.")
                    402 -> throw Exception("Créditos do OpenRouter esgotados. Adicione créditos em openrouter.ai.")
                    429 -> throw Exception("Limite de requisições atingido. Tente novamente em breve.")
                    503 -> throw Exception("OpenRouter indisponível no momento. Tente novamente.")
                    else -> throw Exception("Erro do OpenRouter ($code). Verifique o modelo selecionado.")
                }
            }

            // Extrai o texto da resposta
            val json = JSONObject(responseText)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } finally {
            conn.disconnect()
        }
    }

    // -------------------------------------------------------------------------
    // MISTRAL AI (HTTP direto — modelo Pixtral)
    // -------------------------------------------------------------------------

    private suspend fun callMistral(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = secureStorage.getMistralKey()
        if (apiKey.isNullOrBlank()) throw Exception("Chave da Mistral não configurada. Insira uma chave nas Configurações.")

        // Converte bitmap para base64 JPEG
        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteStream)
        val base64Image = Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)

        // Monta body para Mistral (especificação compatível com visão)
        val body = JSONObject().apply {
            put("model", "pixtral-12b-2409")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", "data:image/jpeg;base64,$base64Image")
                        })
                    })
                })
            })
            // Garante que a Mistral responda em JSON se o prompt pedir
            put("response_format", JSONObject().apply { put("type", "json_object") })
        }.toString()

        val url = URL("https://api.mistral.ai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e("KATUI_OCR", "Mistral erro $code: $error")
                when (code) {
                    401 -> throw Exception("Chave da Mistral inválida ou expirada.")
                    429 -> throw Exception("Limite de requisições da Mistral atingido.")
                    else -> throw Exception("Erro da Mistral AI ($code).")
                }
            }

            val json = JSONObject(responseText)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } finally {
            conn.disconnect()
        }
    }

    // -------------------------------------------------------------------------
    // UTILITÁRIOS
    // -------------------------------------------------------------------------

    private fun loadBitmap(context: Context, uri: Uri): Bitmap {
        return try {
            if (PdfUtils.isPdf(context, uri)) {
                Log.d("KATUI_OCR", "PDF detectado, renderizando página 1...")
                PdfUtils.pdfToBitmap(context, uri)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            Log.e("KATUI_OCR", "Erro ao carregar imagem/pdf", e)
            null
        } ?: throw Exception("Não foi possível carregar o arquivo selecionado.")
    }
}

@kotlinx.serialization.Serializable
data class ExtractedMetric(
    val metrica: String,
    val valor: Double,
    val unidade: String? = null,
    val referencia: String? = null
) {
    fun toExameResultado() = com.katui.offline.data.local.entities.ExameResultado(
        exameId = 0,
        metrica = metrica,
        valor = valor,
        unidade = unidade,
        referencia = referencia
    )
}

@kotlinx.serialization.Serializable
data class OCRResult(
    val dataReceita: String? = null,
    val medicamentos: List<ExtractedMedication>
)

@kotlinx.serialization.Serializable
data class ExtractedMedication(
    val nome: String,
    val horario: String,
    val dosagem: String? = null,
    val finalidade: String? = null,
    val tipoFrequencia: String,
    val valorFrequencia: Int? = null,
    val dias: Int? = null,
    val observacoes: String? = null,
    val estoqueAtual: Double? = 0.0,
    val dosePorTomada: Double? = 1.0,
    val unidadeMedida: String? = "un"
) {
    fun toMedicamento(usuarioId: Long, dataReferencia: String? = null) = Medicamento(
        nome = nome,
        horario = if (horario.length == 5) horario else "08:00",
        dosagem = dosagem,
        finalidade = finalidade,
        tipoFrequencia = tipoFrequencia,
        valorFrequencia = valorFrequencia,
        dias = dias,
        observacoes = if (dataReferencia != null) "Data Receita: $dataReferencia. $observacoes" else observacoes,
        usuarioId = usuarioId,
        dataInicio = LocalDate.now(),
        // FIX: quando a IA não extrai "dias" (tratamento de uso contínuo, sem prazo
        // definido na receita — comum em antidepressivos, anti-hipertensivos, etc.),
        // dataFim deve ficar null (sem prazo) em vez de virar "hoje". Forcá-lo para hoje
        // fazia o tratamento expirar no mesmo dia em que foi cadastrado, zerando a janela
        // de geração de alarmes (ver MedicationViewModel.createAlarmsForMedication).
        // FIX 2: o prompt instrui a IA a usar 0 (não null) quando não tem certeza de um
        // campo numérico — então dias=0 chega com tanta frequência quanto dias=null para
        // o mesmo caso de "uso contínuo, sem prazo". Tratamos dias <= 0 da mesma forma que
        // null, e só um valor positivo real define um prazo de tratamento.
        dataFim = dias?.takeIf { it > 0 }?.let { LocalDate.now().plusDays((it.toLong() - 1)) },
        estoqueAtual = estoqueAtual ?: 0.0,
        estoqueMinimo = 5.0,
        dosePorTomada = dosePorTomada ?: 1.0,
        unidadeMedida = unidadeMedida ?: "un"
    )
}
