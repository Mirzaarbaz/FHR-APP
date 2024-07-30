package com.example.version0

// PDFUtils.kt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import com.jjoe64.graphview.GraphView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PDFUtils(private val context: Context) {

    fun sharePDF(clockTextView: TextView, graph: GraphView, speechLogTable: TableLayout, userName: String?) {
        val pdfFilePath = generatePDF(clockTextView, graph, speechLogTable, userName)
        if (pdfFilePath != null) {
            val file = File(pdfFilePath)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(Intent.createChooser(intent, "Share PDF"))
        } else {
            // Handle PDF generation failure
            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generatePDF(clockTextView: TextView, graph: GraphView, speechLogTable: TableLayout,userName: String? ): String? {
        return try {
            // A4 paper size in points (1 inch = 72 points)
            val pageWidth = 842
            val pageHeight = 595

            // Margin in points
            val margin = 36

            // Create a PDF document
            val document = Document(PageSize.A4.rotate()) // Use rotate() to switch to landscape mode
            val pdfFilePath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath + "/graph_and_table.pdf"

            // Create a PdfWriter instance
            val writer = PdfWriter.getInstance(document, FileOutputStream(pdfFilePath))
            document.open()

            // Add heading line to the PDF
            val heading = Paragraph("Fetal Heart Rate Monitoring Report")
            heading.font.size = 16f
            heading.alignment = Element.ALIGN_CENTER
            document.add(heading)

            // Add Name line to the PDF
            val name = Paragraph("Name: $userName")
            name.font.size = 12f
            name.font.color = BaseColor.BLUE
            name.alignment = Element.ALIGN_CENTER
            document.add(name)

            // Add the timer value
            val timerValue = clockTextView.text.toString()
            val timerParagraph = Paragraph("Total Time: $timerValue")
            timerParagraph.font.color = BaseColor.BLUE
            timerParagraph.alignment = Element.ALIGN_CENTER
            document.add(timerParagraph)

            // Add a paragraph break
            document.add(Paragraph("\n"))

            // Add the graph to the PDF
            val graphBitmap = captureGraphBitmap(graph)
            addImageToPDF(document, graphBitmap, 36F, 66F)

            // Add a paragraph break
            document.add(Paragraph("\n"))

            // Add the table to the PDF
            val tableBitmap = captureTableBitmap(speechLogTable)
            addImageToPDF(document, tableBitmap, 420F, 66F)

            document.close()
            pdfFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun captureGraphBitmap(graphView: GraphView): Bitmap {
        val bitmap = Bitmap.createBitmap(
            graphView.width,
            graphView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        graphView.draw(canvas)
        return bitmap
    }

    private fun captureTableBitmap(table: TableLayout): Bitmap {
        val bitmap = Bitmap.createBitmap(
            table.width,
            table.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        table.draw(canvas)
        return bitmap
    }

    private fun addImageToPDF(document: Document, bitmap: Bitmap, x: Float, y: Float) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val image = Image.getInstance(stream.toByteArray())
        image.setAbsolutePosition(x, y)
        image.scaleAbsolute(360F, minOf(360F, image.height - 30))
        document.add(image)
    }
}
