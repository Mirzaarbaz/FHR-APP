package com.example.version0

// GraphInitializer.kt

import android.content.Context
import android.graphics.Color
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.jjoe64.graphview.DefaultLabelFormatter
import androidx.core.content.ContextCompat

class GraphInitializer(private val context: Context, private val graph: GraphView) {
    private val series: LineGraphSeries<DataPoint> = LineGraphSeries()

    init {
        initializeGraph()
    }

    private fun initializeGraph() {
        graph.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    // Format X-axis labels as integers
                    value.toInt().toString()
                } else {
                    super.formatLabel(value, isValueX)
                }
            }
        }

        // Add two horizontal lines
        val lineAt120 =
            LineGraphSeries(arrayOf(DataPoint(0.0, 120.0), DataPoint(8.0, 120.0)))
        lineAt120.color = ContextCompat.getColor(context, R.color.blue)

        val lineAt160 =
            LineGraphSeries(arrayOf(DataPoint(0.0, 160.0), DataPoint(8.0, 160.0)))
//        lineAt160.color = Color.MAGENTA
        lineAt160.color = ContextCompat.getColor(context, R.color.blue)

        // Add the series to the graph
        graph.addSeries(series)
        graph.addSeries(lineAt120)
        graph.addSeries(lineAt160)


        // Add the series and lines to the graph
        graph.addSeries(series)
        graph.addSeries(lineAt120)
        graph.addSeries(lineAt160)

        // Set initial viewport range
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinY(100.0)
        graph.viewport.setMaxY(180.0)
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(8.0)

        // Set the number of divisions on each axis
        graph.gridLabelRenderer.numHorizontalLabels = 9 // 8 divisions plus one extra for the end point
        graph.gridLabelRenderer.numVerticalLabels = 9 // 8 divisions plus one extra for the end point
        graph.gridLabelRenderer.setHumanRounding(false) // Disable rounding for exact division


        // Append initial data point to the series
        series.appendData(DataPoint(0.0, 120.0), true, 8)
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(8.0)

        // Notify the graph that the data has changed
        graph.onDataChanged(true, true)

        // Force a re-draw of the graph
        graph.post {
            graph.viewport.scrollToEnd()  // Scroll to the end to make sure lines are visible
            graph.invalidate()
        }
    }

    fun addDataPoint(value: Double) {
        // Append data point
        series.appendData(DataPoint(series.highestValueX + 1, value), true, 8)

        val color = if (value < 120.0 || value > 160.0) Color.RED else ContextCompat.getColor(context, R.color.dark_green)

        series.color = color
    }

}
