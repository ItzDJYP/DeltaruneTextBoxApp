// File: FullscreenVideoActivity.kt

package com.example.deltarunetextboxapp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class FullscreenVideoActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null

    companion object {
        const val EXTRA_URI_STRING = "extra_uri_string"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)
        playerView = findViewById(R.id.playerView)

        // Make the activity fullscreen
        hideSystemUI()

        val uriString = intent.getStringExtra(EXTRA_URI_STRING)

        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No video URI provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializePlayer(Uri.parse(uriString))
    }

    private fun initializePlayer(videoUri: Uri) {
        try {
            exoPlayer = ExoPlayer.Builder(this).build().also { player ->
                playerView?.player = player
                val mediaItem = MediaItem.fromUri(videoUri)
                player.setMediaItem(mediaItem)
                player.repeatMode = Player.REPEAT_MODE_OFF // Play once
                player.playWhenReady = true
                player.prepare()

                // Add a listener to finish the activity when the video ends
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            finish()
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing video: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}
