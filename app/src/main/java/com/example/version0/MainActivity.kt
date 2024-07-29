package com.example.version0

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.version0.ui.theme.Version0Theme
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class MainActivity : AppCompatActivity() {

    private lateinit var yourButton: Button
    private lateinit var shareButton: ImageButton
    private lateinit var card1: LinearLayout

    private lateinit var resultTextView: TextView
    private lateinit var clockTextView: TextView
    private lateinit var speechLogTable: TableLayout

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize GraphView and LineGraphSeries
        val graph = findViewById<GraphView>(R.id.graph)
        GraphInitializer(graph)

        // Setup PDF sharing
        val pdfUtils = PDFUtils(this)
        shareButton = findViewById(R.id.shareButton)
        shareButton.setOnClickListener {
            pdfUtils.sharePDF(clockTextView, graph, speechLogTable)
        }

        // Initialize clockTextView
        clockTextView = findViewById(R.id.clockTextView)
        speechLogTable = findViewById(R.id.speechLogTable)
    }}