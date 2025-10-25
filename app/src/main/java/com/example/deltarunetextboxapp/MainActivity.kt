package com.example.deltarunetextboxapp

import android.util.TypedValue
import androidx.core.widget.TextViewCompat
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var spamtonPlayer: MediaPlayer
    private lateinit var tennaPlayer: MediaPlayer
    private var activePlayer: MediaPlayer? = null

    private var typingJob: Job? = null

    // We don't need the enum anymore, we can check the player directly
    // private enum class Character { SPAMTON, TENNA }
    // private var selectedCharacter = Character.SPAMTON

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText = findViewById<EditText>(R.id.inputText)
        val playButton = findViewById<Button>(R.id.playButton)
        val outputText = findViewById<TextView>(R.id.outputText)
// after val outputText = findViewById<TextView>(R.id.outputText)
        if (TextViewCompat.getAutoSizeTextType(outputText) == TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE) {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                outputText,
                10, // min size in sp
                20, // max size in sp
                1,  // step granularity in sp
                TypedValue.COMPLEX_UNIT_SP
            )
        }
        val spamtonButton = findViewById<Button>(R.id.spamtonButton)
        val tennaButton = findViewById<Button>(R.id.tennaButton)
        val portraitImage = findViewById<ImageView>(R.id.portraitImage)

        spamtonPlayer = MediaPlayer.create(this, R.raw.spamton_voice).apply {
            isLooping = true
        }
        tennaPlayer = MediaPlayer.create(this, R.raw.tenna_voice).apply {
            isLooping = true
        }
        activePlayer = spamtonPlayer // Default to Spamton

        // --- CORRECTED Character Selection Logic ---
        spamtonButton.setOnClickListener {
            // Stop the other player if it's running
            if (tennaPlayer.isPlaying) {
                tennaPlayer.pause()
                tennaPlayer.seekTo(0)
            }
            portraitImage.setImageResource(R.drawable.spamton_portrait)
            activePlayer = spamtonPlayer
        }

        tennaButton.setOnClickListener {
            // Stop the other player if it's running
            if (spamtonPlayer.isPlaying) {
                spamtonPlayer.pause()
                spamtonPlayer.seekTo(0)
            }
            portraitImage.setImageResource(R.drawable.tenna_portrait)
            activePlayer = tennaPlayer
        }

        // --- CORRECTED Main "Speak" Button Logic ---
        playButton.setOnClickListener {
            val text = inputText.text.toString()
            outputText.text = ""

            // --- FIX: Only pause/seek if the player is actually playing ---
            if (activePlayer?.isPlaying == true) {
                activePlayer?.pause()
                activePlayer?.seekTo(0)
            }
            typingJob?.cancel()

            typingJob = lifecycleScope.launch {
                try {
                    activePlayer?.start()
                    for (char in text) {
                        outputText.append(char.toString())
                        delay(50) // Typing speed
                    }
                } finally {
                    // This is guaranteed to run after the loop is done or cancelled
                    if (activePlayer?.isPlaying == true) {
                        activePlayer?.pause()
                        activePlayer?.seekTo(0)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        spamtonPlayer.release()
        tennaPlayer.release()
        typingJob?.cancel()
    }
}