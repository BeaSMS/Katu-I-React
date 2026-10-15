package com.katui.offline.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    /**
     * Converte a primeira página de um PDF para um Bitmap.
     * Útil para enviar para a IA (Gemini) processar como imagem.
     */
    fun pdfToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
            
            val renderer = PdfRenderer(fileDescriptor)
            if (renderer.pageCount == 0) {
                renderer.close()
                fileDescriptor.close()
                return null
            }

            // Renderiza apenas a primeira página
            val page = renderer.openPage(0)
            
            // Criar um bitmap com as dimensões da página (escala 2x para melhor qualidade de OCR)
            val scale = 2
            val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
            
            // Pintar fundo de branco (PDFs podem ter fundo transparente)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            renderer.close()
            fileDescriptor.close()
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Verifica se a URI aponta para um PDF.
     */
    fun isPdf(context: Context, uri: Uri): Boolean {
        return context.contentResolver.getType(uri) == "application/pdf" || 
               uri.toString().lowercase().endsWith(".pdf")
    }
}
