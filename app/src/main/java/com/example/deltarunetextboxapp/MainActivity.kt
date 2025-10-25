// File: MainActivity.kt

package com.example.deltarunetextboxapp

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // ... (other properties are unchanged) ...
    private lateinit var spamtonPlayer: MediaPlayer
    private lateinit var tennaPlayer: MediaPlayer
    private var activePlayer: MediaPlayer? = null
    private var typingJob: Job? = null
    private var videoFiles: List<String> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ... (findViewById calls are unchanged) ...
        val inputText = findViewById<EditText>(R.id.inputText)
        val playButton = findViewById<Button>(R.id.playButton)
        val outputText = findViewById<TextView>(R.id.outputText)
        if (TextViewCompat.getAutoSizeTextType(outputText) == TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE) {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                outputText, 10, 20, 1, TypedValue.COMPLEX_UNIT_SP
            )
        }
        val spamtonButton = findViewById<Button>(R.id.spamtonButton)
        val tennaButton = findViewById<Button>(R.id.tennaButton)
        val portraitImage = findViewById<ImageView>(R.id.portraitImage)
        val playMeButton = findViewById<Button>(R.id.playMeButton)

        // ... (loading videoFiles is unchanged) ...
        videoFiles = try {
            assets.list("videos")
                ?.filter { it.endsWith(".mp4", true) || it.endsWith(".webm", true) || it.endsWith(".mkv", true) }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            Toast.makeText(this, "No 'videos' folder found in assets.", Toast.LENGTH_SHORT).show()
            emptyList()
        }

        // ... (player setup and character button listeners are unchanged) ...
        spamtonPlayer =
            MediaPlayer.create(this, R.raw.spamton_voice).apply { isLooping = true }
        tennaPlayer =
            MediaPlayer.create(this, R.raw.tenna_voice).apply { isLooping = true }
        activePlayer = spamtonPlayer
        spamtonButton.setOnClickListener {
            if (tennaPlayer.isPlaying) {
                tennaPlayer.pause(); tennaPlayer.seekTo(0)
            }
            portraitImage.setImageResource(R.drawable.spamton_portrait)
            activePlayer = spamtonPlayer
        }
        tennaButton.setOnClickListener {
            if (spamtonPlayer.isPlaying) {
                spamtonPlayer.pause(); spamtonPlayer.seekTo(0)
            }
            portraitImage.setImageResource(R.drawable.tenna_portrait)
            activePlayer = tennaPlayer
        }


        // --- THE CORRECTED "Play Me" BUTTON LOGIC ---
        playMeButton.setOnClickListener {
            activePlayer?.takeIf { it.isPlaying }?.apply { pause(); seekTo(0) }

            val fileUri: Uri? = if (videoFiles.isNotEmpty()) {
                // Priority 1: Get URI from a random video in assets
                val randomFilename = videoFiles.random()
                copyAssetToCacheAndGetUri("videos/$randomFilename")
            } else {
                // Priority 2: Get URI from character clips in res/raw
                val targetName = if (activePlayer == spamtonPlayer) "spamton_clip" else "tenna_clip"
                val resId = resources.getIdentifier(targetName, "raw", packageName)
                if (resId != 0) {
                    val rawUri = Uri.parse("android.resource://$packageName/$resId")
                    copyRawToCacheAndGetUri(rawUri, "$targetName.mp4")
                } else {
                    null
                }
            }

            if (fileUri != null) {
                val intent = Intent(this, FullscreenVideoActivity::class.java)
                intent.putExtra(FullscreenVideoActivity.EXTRA_URI_STRING, fileUri.toString())
                // Grant permission for the content:// URI
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } else {
                // Priority 3: Show fallback message
                Toast.makeText(this, "No video found.", Toast.LENGTH_LONG).show()
            }
        }

        // ... (playButton click listener is unchanged) ...
        playButton.setOnClickListener {
            val text = inputText.text.toString()
            outputText.text = ""

            activePlayer?.takeIf { it.isPlaying }?.apply { pause(); seekTo(0) }
            typingJob?.cancel()

            typingJob = lifecycleScope.launch {
                try {
                    activePlayer?.start()
                    for (char in text) {
                        outputText.append(char.toString())
                        delay(50)
                    }
                } finally {
                    activePlayer?.takeIf { it.isPlaying }?.apply { pause(); seekTo(0) }
                }
            }
        }
    }

    // --- HELPER FUNCTION TO COPY ASSET AND GET URI ---
    private fun copyAssetToCacheAndGetUri(assetPath: String): Uri? {
        return try {
            val filename = assetPath.substringAfterLast('/')
            val tempFile = File(cacheDir, filename)

            // Only copy if the file doesn't exist to save time
            if (!tempFile.exists()) {
                assets.open(assetPath).use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- HELPER FUNCTION TO COPY RAW RESOURCE AND GET URI ---
    private fun copyRawToCacheAndGetUri(rawUri: Uri, filename: String): Uri? {
        return try {
            val tempFile = File(cacheDir, filename)

            if (!tempFile.exists()) {
                contentResolver.openInputStream(rawUri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        spamtonPlayer.release()
        tennaPlayer.release()
        typingJob?.cancel()
    }
}
