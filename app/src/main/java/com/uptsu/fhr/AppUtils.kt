package com.uptsu.fhr

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.jjoe64.graphview.GraphView

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
            apiHelper: ApiHelper,
            textViewData: MutableMap<String, Any>,
            onConfirm: (String, String, String) -> Unit // ✅ Update callback to accept three parameters
        ) {
            Log.d("ConfirmationDialog", "✅ Dialog opened.")

            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confirmation, null)

            val deliveryTypeSpinner: Spinner = dialogView.findViewById(R.id.deliveryTypeSpinner)
//            val motherConditionSpinner: Spinner = dialogView.findViewById(R.id.motherConditionSpinner)
//            val childConditionSpinner: Spinner = dialogView.findViewById(R.id.childConditionSpinner)

            val deliveryOptions = arrayOf("Type of Delivery", "Normal", "Assisted", "C-Section", "Referred-undelivered", "Shift change")
//            val healthConditionOptions = arrayOf("Mother Outcome", "Pre-Delivery Referral", "Post-Delivery Referral", "Alive", "Dead")
//            val childConditionOptions = arrayOf("Baby Outcome", "Alive", "Referred", "Still-Birth")

            deliveryTypeSpinner.adapter = ArrayAdapter(context, R.layout.spinner_item, deliveryOptions)
//            motherConditionSpinner.adapter = ArrayAdapter(context, R.layout.spinner_item, healthConditionOptions)
//            childConditionSpinner.adapter = ArrayAdapter(context, R.layout.spinner_item, childConditionOptions)

            // ✅ Set Green Text when a valid selection is made
            fun applyGreenText(spinner: Spinner, defaultText: String) {
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedItem = spinner.selectedItem.toString()
                        val textView = parent?.getChildAt(0) as? TextView
                        if (textView != null) {
                            if (selectedItem != defaultText) {
                                textView.setTextColor(context.resources.getColor(android.R.color.holo_green_dark, null)) // Green Color
                            } else {
                                textView.setTextColor(context.resources.getColor(android.R.color.black, null)) // Reset to Black
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            // Apply color change to all Spinners
            applyGreenText(deliveryTypeSpinner, "Type of Delivery")
//            applyGreenText(motherConditionSpinner, "Mother Outcome")
//            applyGreenText(childConditionSpinner, "Baby Outcome")

            val dialog = builder.setTitle("Confirmation")
                .setMessage("Please fill the following")
                .setView(dialogView)
                .setPositiveButton("Yes (Save & Print)", null) // Set to null for custom handling
                .setNegativeButton("No") { dialog, _ ->
                    Log.d("ConfirmationDialog", "❌ User cancelled.")
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val deliveryType = deliveryTypeSpinner.selectedItem.toString()
                    if (deliveryType == "Type of Delivery") {
                        Toast.makeText(context, "Please select the type of delivery.", Toast.LENGTH_SHORT).show()
                    } else {
                        pdfUtils.sharePDF(clockTextView, graph, speechLogTable, userName, userNumber.toString())
                        onConfirm(deliveryType, "none", "none")
                        dialog.dismiss()
                    }
                }
            }

            dialog.show()
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
            mediaPlayerManager: MediaPlayerManager,
            clockManager: ClockManager,
            updateComponents: (GraphInitializer, ClockManager) -> Unit
        ) {
            // Stop any ongoing processes
            clockManager.stopClock()
            mediaPlayerManager.releaseMediaPlayer()

            // Clear the graph data
            clearGraph(graph)

            // Clear UI elements
            name.text="Name:                          "
            dilation.text="|     Dilatation:  "
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