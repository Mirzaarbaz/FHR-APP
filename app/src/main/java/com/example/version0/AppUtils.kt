package com.example.version0

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.jjoe64.graphview.GraphView
import androidx.appcompat.app.AlertDialog

class AppUtils {

    companion object {

        fun showConfirmationDialog(
            context: Context,
            pdfUtils: PDFUtils,
            clockTextView: TextView,
            graph: GraphView,
            speechLogTable: TableLayout,
            userName: String?,
            userNumber: Int,
            resetApp: () -> Unit
        ) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to finish? This will reset the application.")
                .setPositiveButton("Yes & Print") { dialog, _ ->
                    // Share the PDF
                    pdfUtils.sharePDF(clockTextView, graph, speechLogTable, userName, userNumber.toString())

                    // Clear everything and reset the app
                    resetApp()
                    dialog.dismiss() // Close the dialog
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Just close the dialog
                }
                .create()
                .show()
        }

        fun clearGraph(graph: GraphView) {
            // Clear all data points from the graph
            graph.removeAllSeries()
        }

        fun resetApp(
            context: Context,
            clockTextView: TextView,
            graph: GraphView,
            speechLogTable: TableLayout,
            resultTextView: TextView,
            mediaPlayer: MediaPlayer,
            textToSpeechManager: TextToSpeechManager,
            clockManager: ClockManager,
            updateComponents: (GraphInitializer, ClockManager) -> Unit
        ) {
            // Stop any ongoing processes
            textToSpeechManager.stop()
            clockManager.stopClock()
            mediaPlayer.release()

            // Clear the graph data
            clearGraph(graph)

            // Clear UI elements
            resultTextView.text = "000"
            clockTextView.text = "00:00:00"
            // Preserve and clear speechLogTable while keeping the header row
            preserveAndClearTable(speechLogTable)

            // Reinitialize components
            val (newGraphInitializer, newClockManager) = initializeComponents(context, graph, clockTextView)

            // Set the new instances
            updateComponents(newGraphInitializer, newClockManager)
        }

        fun initializeComponents(
            context: Context,
            graph: GraphView,
            clockTextView: TextView
        ): Pair<GraphInitializer, ClockManager> {
            // Reinitialize MediaPlayer
            val newMediaPlayer = MediaPlayer.create(context, R.raw.fhr)

            // Reinitialize or reset other components
            val newGraphInitializer = GraphInitializer(context, graph)
            val newClockManager = ClockManager(clockTextView)
            val newTextToSpeechManager = TextToSpeechManager(context, context as TextToSpeech.OnInitListener)

            // Return the new instances
            return Pair(newGraphInitializer, newClockManager)
        }

        fun preserveAndClearTable(speechLogTable: TableLayout) {
            // Retrieve the header row
            val headerRow = if (speechLogTable.childCount > 0) {
                speechLogTable.getChildAt(0) as TableRow
            } else {
                null
            }

            // Remove all rows from the table
            speechLogTable.removeAllViews()

            // Re-add the header row if it exists
            headerRow?.let {
                speechLogTable.addView(it)
            }
        }
    }
}
