package com.katui.offline.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utilitário para criação de PDFs a partir de imagens.
 * Usa apenas android.graphics.pdf.PdfDocument (API 19+), sem dependências externas.
 */
object PdfManager {

    private const val PAGE_WIDTH = 595   // A4 em pontos (72 dpi) ≈ 210mm
    private const val PAGE_HEIGHT = 842  // A4 em pontos ≈ 297mm
    private const val MARGIN = 20        // margem interna em pontos

    /**
     * Gera um PDF consolidado de consulta médica.
     * Página 1: capa com nome do paciente, especialidade e data.
     * Páginas seguintes: uma por exame, com nome/categoria/resultados e imagem (se houver).
     */
    fun gerarPdfConsulta(
        context: Context,
        nomesPaciente: String,
        especialidade: String,
        exames: List<com.katui.offline.data.local.entities.Exame>,
        resultadosPorExame: Map<Long, List<com.katui.offline.data.local.entities.ExameResultado>>,
        outputDir: File = File(context.filesDir, "pdfs")
    ): File? {
        outputDir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val outFile = File(outputDir, "consulta_$timestamp.pdf")
        val document = PdfDocument()
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#1a237e")
        }
        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            color = android.graphics.Color.parseColor("#555555")
        }
        val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#333333")
        }
        val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = android.graphics.Color.parseColor("#444444")
        }
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#dddddd")
            strokeWidth = 1f
        }

        try {
            // --- Página 1: Capa ---
            val capaInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val capaPage = document.startPage(capaInfo)
            val c = capaPage.canvas
            
            // Fundo decorativo leve para a capa
            val paintRect = Paint().apply { color = android.graphics.Color.parseColor("#f5f5f5") }
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 200f, paintRect)

            c.drawText("📄 Relatório para Consulta", MARGIN.toFloat(), 80f, paintTitle)
            c.drawLine(MARGIN.toFloat(), 95f, (PAGE_WIDTH - MARGIN).toFloat(), 95f, paintLine)
            
            var capaY = 140f
            c.drawText("Paciente:", MARGIN.toFloat(), capaY, paintLabel)
            c.drawText(nomesPaciente, MARGIN.toFloat() + 80f, capaY, paintBody.apply { textSize = 14f; isFakeBoldText = true })
            capaY += 30f
            
            c.drawText("Especialidade:", MARGIN.toFloat(), capaY, paintLabel)
            c.drawText(especialidade, MARGIN.toFloat() + 80f, capaY, paintBody)
            capaY += 30f
            
            c.drawText("Data de Emissão:", MARGIN.toFloat(), capaY, paintLabel)
            c.drawText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), MARGIN.toFloat() + 80f, capaY, paintBody)
            capaY += 30f
            
            c.drawText("Total de Exames:", MARGIN.toFloat(), capaY, paintLabel)
            c.drawText(exames.size.toString(), MARGIN.toFloat() + 80f, capaY, paintBody)
            
            // Rodapé da capa
            c.drawText("Gerado pelo Katu'I Offline", PAGE_WIDTH / 2f - 60f, PAGE_HEIGHT - 40f, paintSub.apply { textSize = 10f })
            
            document.finishPage(capaPage)

            // --- Uma página por exame ---
            exames.forEachIndexed { index, exame ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 2).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                var y = MARGIN.toFloat() + 20f

                // Cabeçalho da página de exame
                canvas.drawRect(MARGIN.toFloat(), y - 15f, (PAGE_WIDTH - MARGIN).toFloat(), y + 10f, Paint().apply { color = android.graphics.Color.parseColor("#f0f4c3") })
                canvas.drawText("${index + 1}. ${exame.nome}", MARGIN.toFloat() + 5f, y, paintTitle.apply { textSize = 14f; color = android.graphics.Color.BLACK })
                
                y += 35f
                canvas.drawText("Categoria:", MARGIN.toFloat(), y, paintLabel)
                canvas.drawText(exame.categoria, MARGIN.toFloat() + 70f, y, paintBody)
                
                y += 20f
                exame.observacao?.let {
                    canvas.drawText("Observações:", MARGIN.toFloat(), y, paintLabel)
                    y += 15f
                    // Quebra de linha simples para observação
                    val lines = it.chunked(80)
                    lines.take(3).forEach { line ->
                        canvas.drawText(line, MARGIN.toFloat() + 10f, y, paintBody)
                        y += 14f
                    }
                }
                
                canvas.drawLine(MARGIN.toFloat(), y + 4f, (PAGE_WIDTH - MARGIN).toFloat(), y + 4f, paintLine)
                y += 25f

                // Resultados numéricos com destaque
                val resultados = resultadosPorExame[exame.id] ?: emptyList()
                if (resultados.isNotEmpty()) {
                    canvas.drawText("DADOS QUANTITATIVOS:", MARGIN.toFloat(), y, paintLabel.apply { color = android.graphics.Color.parseColor("#d32f2f") })
                    y += 20f
                    resultados.forEach { res ->
                        canvas.drawText(res.metrica, MARGIN.toFloat() + 10f, y, paintBody)
                        canvas.drawText("${res.valor} ${res.unidade ?: ""}", PAGE_WIDTH - MARGIN - 100f, y, paintBody.apply { isFakeBoldText = true })
                        y += 16f
                    }
                    y += 10f
                }

                // Imagem do exame
                if (exame.arquivoPath != null && !exame.isPdf) {
                    val bitmap = try {
                        BitmapFactory.decodeFile(exame.arquivoPath)
                    } catch (e: Exception) { null }

                    bitmap?.let {
                        val availW = (PAGE_WIDTH - MARGIN * 2).toFloat()
                        val availH = (PAGE_HEIGHT - y - MARGIN - 40).coerceAtLeast(100f)
                        val scale = minOf(availW / it.width, availH / it.height)
                        val scaledW = it.width * scale
                        val scaledH = it.height * scale
                        val left = MARGIN + (availW - scaledW) / 2f
                        val matrix = Matrix()
                        matrix.setScale(scale, scale)
                        matrix.postTranslate(left, y)
                        canvas.drawBitmap(it, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
                        it.recycle()
                    }
                } else if (exame.isPdf) {
                    canvas.drawText("📎 [Arquivo PDF original anexo no app]", PAGE_WIDTH / 2f - 100f, y + 50f, paintSub)
                }

                // Número da página
                canvas.drawText("Página ${index + 2} de ${exames.size + 1}", PAGE_WIDTH - MARGIN - 60f, PAGE_HEIGHT - 20f, paintSub.apply { textSize = 9f })

                document.finishPage(page)
            }

            FileOutputStream(outFile).use { fos -> document.writeTo(fos) }
        } catch (e: Exception) {
            e.printStackTrace()
            outFile.delete()
            return null
        } finally {
            document.close()
        }
        return outFile
    }

    /**
     * Converte uma lista de URIs de imagem em um único arquivo PDF.
     * Cada imagem ocupa uma página inteira (mantendo proporção, centralizada).
     *
     * @param context Context para resolver os URIs
     * @param imageUris Lista ordenada de URIs de imagem
     * @param outputDir Diretório de saída (padrão: filesDir/pdfs)
     * @return File apontando para o PDF gerado, ou null em caso de erro
     */
    fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        outputDir: File = File(context.filesDir, "pdfs")
    ): File? {
        if (imageUris.isEmpty()) return null

        outputDir.mkdirs()

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val outFile = File(outputDir, "exame_$timestamp.pdf")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val document = PdfDocument()

        try {
            imageUris.forEachIndexed { index, uri ->
                val bitmap = decodeBitmapFromUri(context, uri) ?: return@forEachIndexed

                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Área disponível descontando margens
                val availW = (PAGE_WIDTH - MARGIN * 2).toFloat()
                val availH = (PAGE_HEIGHT - MARGIN * 2).toFloat()

                // Calcular escala mantendo proporção
                val scaleX = availW / bitmap.width
                val scaleY = availH / bitmap.height
                val scale = minOf(scaleX, scaleY)

                val scaledW = bitmap.width * scale
                val scaledH = bitmap.height * scale

                // Centralizar na página
                val left = MARGIN + (availW - scaledW) / 2f
                val top = MARGIN + (availH - scaledH) / 2f

                val matrix = Matrix()
                matrix.setScale(scale, scale)
                matrix.postTranslate(left, top)

                canvas.drawBitmap(bitmap, matrix, paint)
                bitmap.recycle()

                document.finishPage(page)
            }

            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            outFile.delete()
            return null
        } finally {
            document.close()
        }

        return outFile
    }

    /**
     * Copia um PDF externo (content:// URI) para o armazenamento interno do app.
     * Necessário para que o path seja persistente mesmo após a sessão do picker.
     */
    fun copyPdfToInternal(
        context: Context,
        sourceUri: Uri,
        outputDir: File = File(context.filesDir, "pdfs")
    ): File? {
        outputDir.mkdirs()

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val outFile = File(outputDir, "doc_$timestamp.pdf")

        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            outFile.delete()
            null
        }
    }

    /**
     * Copia uma imagem externa (content:// URI) para o armazenamento interno do app.
     * Garante que o arquivo esteja disponível permanentemente.
     */
    fun copyImageToInternal(
        context: Context,
        sourceUri: Uri,
        outputDir: File = File(context.filesDir, "images")
    ): File? {
        outputDir.mkdirs()

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        
        // Tenta descobrir a extensão original
        val extension = context.contentResolver.getType(sourceUri)?.substringAfterLast("/") ?: "jpg"
        val outFile = File(outputDir, "img_$timestamp.$extension")

        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            outFile.delete()
            null
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
