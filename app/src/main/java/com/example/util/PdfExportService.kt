package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.WeldJoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportService {

    /**
     * Generates a professional A4 technical welding traceability report PDF for a WeldJoint.
     * Launches the Android Share/View intent directly so the user can print, view, or save the PDF.
     */
    fun exportJointReport(context: Context, weld: WeldJoint) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 dpi)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawWeldReport(canvas, weld)

        pdfDocument.finishPage(page)

        try {
            val fileName = "WDB_Joint_${weld.isoNumber.replace("/", "_")}_${weld.spoolNumber}_${weld.jointNumber}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Fiche de Traçabilité WDB - Joint ${weld.jointNumber} (${weld.isoNumber})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Partager le rapport PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }

    private fun drawWeldReport(canvas: Canvas, weld: WeldJoint) {
        val paint = Paint()

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Top Banner (Dark Industrial Navy)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(30f, 30f, 565f, 105f, paint)

        // Title WDB
        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("WDB - WELDING DATA BASE", 50f, 65f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("FICHE OFFICIELLE DE TRAÇABILITÉ DU JOINT DE SOUDURE", 50f, 88f, paint)

        // Date on top right
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        paint.textSize = 9f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawText("Édité le: $dateStr", 420f, 65f, paint)

        var y = 135f

        // Helper box drawer
        fun drawSection(title: String, block: (Float) -> Float) {
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(title, 35f, y, paint)

            paint.color = Color.parseColor("#0284C7")
            paint.strokeWidth = 2f
            canvas.drawLine(35f, y + 4f, 560f, y + 4f, paint)

            y += 22f
            y = block(y)
            y += 15f
        }

        // 1. IDENTIFICATION
        drawSection("1. IDENTIFICATION DU JOINT") { curY ->
            var cy = curY
            drawKeyValue(canvas, "ISO (Plan isométrique):", weld.isoNumber.ifBlank { "N/A" }, 45f, cy, 220f)
            drawKeyValue(canvas, "SPOOL (Tronçon):", weld.spoolNumber.ifBlank { "N/A" }, 300f, cy, 460f)
            cy += 20f
            drawKeyValue(canvas, "JOINT N°:", weld.jointNumber.ifBlank { "N/A" }, 45f, cy, 220f, highlight = true)
            drawKeyValue(canvas, "STATUT GLOBAL:", weld.status, 300f, cy, 460f)
            cy += 10f
            cy
        }

        // 2. PART 1 & PART 2
        drawSection("2. TRAÇABILITÉ DES COMPOSANTS (HEAT NUMBERS)") { curY ->
            var cy = curY

            // Part 1
            paint.color = Color.parseColor("#334155")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("▶ PARTIE 01 (Élément amont):", 45f, cy, paint)
            cy += 18f
            drawKeyValue(canvas, "Description:", weld.part1Description.ifBlank { "Tuyau / Composant" }, 55f, cy, 200f)
            drawKeyValue(canvas, "Matière:", weld.part1Material.ifBlank { weld.material }, 320f, cy, 440f)
            cy += 18f
            drawKeyValue(canvas, "Heat Number (Coulée):", weld.part1HeatNumber.ifBlank { "NON RENSEIGNÉ" }, 55f, cy, 230f, highlight = true)

            cy += 24f

            // Part 2
            canvas.drawText("▶ PARTIE 02 (Élément aval):", 45f, cy, paint)
            cy += 18f
            drawKeyValue(canvas, "Description:", weld.part2Description.ifBlank { "Composant / Raccord" }, 55f, cy, 200f)
            drawKeyValue(canvas, "Matière:", weld.part2Material.ifBlank { weld.material }, 320f, cy, 440f)
            cy += 18f
            drawKeyValue(canvas, "Heat Number (Coulée):", weld.part2HeatNumber.ifBlank { "NON RENSEIGNÉ" }, 55f, cy, 230f, highlight = true)

            cy += 10f
            cy
        }

        // 3. WELDING
        drawSection("3. DONNÉES DE SOUDAGE") { curY ->
            var cy = curY
            drawKeyValue(canvas, "Procédé de soudage:", weld.process, 45f, cy, 200f)
            drawKeyValue(canvas, "WPS (DMOS N°):", weld.wps, 300f, cy, 440f)
            cy += 20f
            drawKeyValue(canvas, "Soudeur (Poinçon):", weld.welder.ifBlank { weld.welderName }, 45f, cy, 200f)
            drawKeyValue(canvas, "Date de soudage:", weld.date.ifBlank { "N/A" }, 300f, cy, 440f)
            cy += 20f
            drawKeyValue(canvas, "Type de joint:", weld.weldType, 45f, cy, 200f)
            drawKeyValue(canvas, "Diamètre / Épaisseur:", "${weld.diameterInch}\" (${weld.thicknessMm} mm)", 300f, cy, 440f)
            cy += 10f
            cy
        }

        // 4. INSPECTION & NDT
        drawSection("4. CONTRÔLES NON DESTRUCTIFS (CND / NDT)") { curY ->
            var cy = curY
            drawKeyValue(canvas, "Contrôle Visuel (VT):", weld.visual, 45f, cy, 200f)
            drawKeyValue(canvas, "Résultat RT (Radio):", weld.rt, 300f, cy, 440f)
            cy += 20f
            drawKeyValue(canvas, "Résultat PT (Ressuage):", weld.pt, 45f, cy, 200f)
            drawKeyValue(canvas, "Résultat UT (Ultrasons):", weld.ut, 300f, cy, 440f)
            cy += 20f
            drawKeyValue(canvas, "N° Rapport / PV NDT:", weld.ndtReportNo.ifBlank { "N/A" }, 45f, cy, 200f)
            drawKeyValue(canvas, "Remarques:", weld.notes.ifBlank { "R.A.S - Conforme aux spécifications" }, 300f, cy, 440f)
            cy += 10f
            cy
        }

        // Signatures block
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(35f, y + 10f, 560f, y + 80f, 8f, 8f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("VISA CONTRÔLE QUALITÉ (QC INSPECTION)", 50f, y + 30f, paint)
        canvas.drawText("VISA CLIENT / SUPERVISION", 320f, y + 30f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Nom & Signature: _______________________", 50f, y + 65f, paint)
        canvas.drawText("Nom & Signature: _______________________", 320f, y + 65f, paint)

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("WDB - Welding Data Base v1.0 • Document officiel de traçabilité matière et CND", 130f, 825f, paint)
    }

    private fun drawKeyValue(
        canvas: Canvas,
        key: String,
        value: String,
        keyX: Float,
        y: Float,
        valX: Float,
        highlight: Boolean = false
    ) {
        val paint = Paint()
        paint.textSize = 10.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#475569")
        canvas.drawText(key, keyX, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (highlight) Color.parseColor("#0369A1") else Color.parseColor("#0F172A")
        canvas.drawText(value, valX, y, paint)
    }
}
