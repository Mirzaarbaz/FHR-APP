package com.example.version0

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
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
            onConfirm: () -> Unit // Callback with no parameters
        ) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to finish? This will reset the application.")
                .setPositiveButton("Yes (Save & Print)") { dialog, _ ->
                    // Share the PDF
                    pdfUtils.sharePDF(clockTextView, graph, speechLogTable, userName, userNumber.toString())

                    // Handle confirmation action
                    onConfirm() // Call the confirmation callback
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

        fun showLoaderAndSharePDF(
            context: Context,
            pdfUtils: PDFUtils,
            clockTextView: TextView,
            graph: GraphView,
            speechLogTable: TableLayout,
            userName: String?,
            userNumber: Int
        ) {
            // Inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val dialogView: View = inflater.inflate(R.layout.dialog_loading, null)

            // Create and show the custom loading dialog
            val loadingDialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            loadingDialog.show()

            // Set the dialog width and height to wrap content or a specific size
            val window = loadingDialog.window
            if (window != null) {
                val layoutParams = window.attributes
                layoutParams.width = dpToPx(context, 150) // Set width to 300dp
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT // Set height to wrap content
                window.attributes = layoutParams
            }

            // Perform the sharing operation after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                // Share the PDF
                pdfUtils.sharePDF(clockTextView, graph, speechLogTable, userName, userNumber.toString())

                // Hide the custom loading dialog after sharing
                loadingDialog.dismiss()
            }, 4000)
        }
        // Function to convert dp to pixels
        fun dpToPx(context: Context, dp: Int): Int {
            val displayMetrics = context.resources.displayMetrics
            return (dp * displayMetrics.density).toInt()
        }


        fun resetApp(
            context: Context,
            clockTextView: TextView,
            graph: GraphView,
            name: TextView,
            dilation: TextView,
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
            name.text="Name:                          "
            dilation.text="|     Dialation:  "
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

            // Remove all rows except the header
            if (headerRow != null) {
                // Start removing from the second row (index 1)
                for (i in speechLogTable.childCount - 1 downTo 1) {
                    speechLogTable.removeViewAt(i)
                }
            }
        }
    }
}