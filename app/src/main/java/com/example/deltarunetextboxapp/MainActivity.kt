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
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var spamtonPlayer: MediaPlayer
    private lateinit var tennaPlayer: MediaPlayer
    private var activePlayer: MediaPlayer? = null
    private var typingJob: Job? = null
    private var videoFiles: List<String> = emptyList()

    // These MUST be image/gif files in your "res/drawable" folder
    private val spamtonImages = listOf(R.drawable.spamton_portrait, R.drawable.spamton_boogie, R.drawable.spamton_color,
        R.drawable.spamton_dance, R.drawable.spamton_laugh, R.drawable.spamton_mrbeast)
    private val tennaImages = listOf(R.drawable.tenna_portrait, R.drawable.tenna_gangnamstyle, R.drawable.tenna_kick,
        R.drawable.tenna_awyeah, R.drawable.tenna_crashout, R.drawable.tenna_dancing, R.drawable.tenna_laugh)

    private var spamtonImageIndex = 0
    private var tennaImageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- 1. FIND ALL VIEWS FIRST ---
        val inputText = findViewById<EditText>(R.id.inputText)
        val playButton = findViewById<Button>(R.id.playButton)
        val outputText = findViewById<TextView>(R.id.outputText)
        val spamtonButton = findViewById<Button>(R.id.spamtonButton)
        val tennaButton = findViewById<Button>(R.id.tennaButton)
        val portraitImage = findViewById<ImageView>(R.id.portraitImage)
        val playMeButton = findViewById<Button>(R.id.playMeButton)
        val cycleImageButton = findViewById<Button>(R.id.cycleImageButton) // Added this line

        // --- 2. INITIALIZE PLAYERS & UI ---
        // These MUST be media files (mp3, etc.) in your "res/raw" folder
        spamtonPlayer = MediaPlayer.create(this, R.raw.spamton_voice).apply { isLooping = true }
        tennaPlayer = MediaPlayer.create(this, R.raw.tenna_voice).apply { isLooping = true }
        activePlayer = spamtonPlayer // Default to Spamton

        // Set the initial portrait *after* all views are found
        updatePortrait(portraitImage)

        // Auto-size text setup
        if (TextViewCompat.getAutoSizeTextType(outputText) == TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE) {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                outputText, 10, 20, 1, TypedValue.COMPLEX_UNIT_SP
            )
        }

        // --- 3. LOAD VIDEO FILES & SET UP LISTENERS ---
        videoFiles = try {
            assets.list("videos")
                ?.filter { it.endsWith(".mp4", true) || it.endsWith(".webm", true) || it.endsWith(".mkv", true) }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            Toast.makeText(this, "No 'videos' folder found in assets.", Toast.LENGTH_SHORT).show()
            emptyList()
        }

        spamtonButton.setOnClickListener {
            if (tennaPlayer.isPlaying) {
                tennaPlayer.pause(); tennaPlayer.seekTo(0)
            }
            activePlayer = spamtonPlayer
            updatePortrait(portraitImage)
        }

        tennaButton.setOnClickListener {
            if (spamtonPlayer.isPlaying) {
                spamtonPlayer.pause(); spamtonPlayer.seekTo(0)
            }
            activePlayer = tennaPlayer
            updatePortrait(portraitImage)
        }

        cycleImageButton.setOnClickListener {
            if (activePlayer == spamtonPlayer) {
                spamtonImageIndex = (spamtonImageIndex + 1) % spamtonImages.size
            } else {
                tennaImageIndex = (tennaImageIndex + 1) % tennaImages.size
            }
            updatePortrait(portraitImage)
        }

        playMeButton.setOnClickListener {
            activePlayer?.takeIf { it.isPlaying }?.apply { pause(); seekTo(0) }

            val fileUri: Uri? = if (videoFiles.isNotEmpty()) {
                val randomFilename = videoFiles.random()
                copyAssetToCacheAndGetUri("videos/$randomFilename")
            } else {
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
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No video found.", Toast.LENGTH_LONG).show()
            }
        }

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

    // --- HELPER AND LIFECYCLE FUNCTIONS (must be outside onCreate) ---

    private fun updatePortrait(imageView: ImageView) {
        val drawableRes = if (activePlayer == spamtonPlayer) {
            spamtonImages[spamtonImageIndex]
        } else {
            tennaImages[tennaImageIndex]
        }

        // Use Glide to load the drawable. It handles GIFs automatically.
        Glide.with(this)
            .load(drawableRes)
            .into(imageView)
    }

    private fun copyAssetToCacheAndGetUri(assetPath: String): Uri? {
        // ... (this function is correct, no changes needed)
        return try {
            val filename = assetPath.substringAfterLast('/')
            val tempFile = File(cacheDir, filename)
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

    private fun copyRawToCacheAndGetUri(rawUri: Uri, filename: String): Uri? {
        // ... (this function is correct, no changes needed)
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