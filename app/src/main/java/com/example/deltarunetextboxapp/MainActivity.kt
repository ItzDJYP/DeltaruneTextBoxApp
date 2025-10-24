package com.example.deltarunetextbox

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var typingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText = findViewById<EditText>(R.id.inputText)
        val playButton = findViewById<Button>(R.id.playButton)
        val outputText = findViewById<TextView>(R.id.outputText)

        mediaPlayer = MediaPlayer.create(this, R.raw.spamton_voice)

        playButton.setOnClickListener {
            val text = inputText.text.toString()
            outputText.text = ""
            mediaPlayer.start()

            // Cancel any previous typing animation
            typingJob?.cancel()

            typingJob = GlobalScope.launch(Dispatchers.Main) {
                for (char in text) {
                    outputText.append(char.toString())
                    delay(50) // speed of typing (adjust)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}