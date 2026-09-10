package com.example.rtspclient

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var urlInput: AutoCompleteTextView
    private lateinit var rtStatusDot: TextView
    private lateinit var rtStatusText: TextView
    private lateinit var historyAdapter: ArrayAdapter<String>

    private val defaultRtspUrl = "rtsp://192.168.144.25:8554/main.264"

    private val colorGreen = Color.parseColor("#4CAF50")
    private val colorAmber = Color.parseColor("#FFC107")
    private val colorRed = Color.parseColor("#F44336")
    private val colorGray = Color.parseColor("#888888")

    private val prefs by lazy { getSharedPreferences("rtsp_client_prefs", Context.MODE_PRIVATE) }
    private val maxHistorySize = 10

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        urlInput = findViewById(R.id.rtsp_url_input)
        rtStatusDot = findViewById(R.id.rt_status_dot)
        rtStatusText = findViewById(R.id.rt_status_text)
        val connectButton = findViewById<Button>(R.id.connect_button)

        val history = loadHistory()
        historyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, history)
        urlInput.setAdapter(historyAdapter)
        urlInput.threshold = 1
        urlInput.setOnClickListener { urlInput.showDropDown() }
        urlInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) urlInput.showDropDown() }

        val startUrl = history.firstOrNull() ?: defaultRtspUrl
        urlInput.setText(startUrl)
        connectButton.setOnClickListener { connect(urlInput.text.toString()) }

        connect(startUrl)
    }

    private fun loadHistory(): MutableList<String> {
        val raw = prefs.getString("url_history", "") ?: ""
        return if (raw.isBlank()) mutableListOf() else raw.split("\n").toMutableList()
    }

    private fun rememberUrl(url: String) {
        val history = loadHistory()
        history.remove(url)
        history.add(0, url)
        while (history.size > maxHistorySize) history.removeAt(history.size - 1)

        prefs.edit().putString("url_history", history.joinToString("\n")).apply()

        historyAdapter.clear()
        historyAdapter.addAll(history)
        historyAdapter.notifyDataSetChanged()
    }

    private fun setRtStatus(text: String, color: Int) {
        rtStatusDot.setTextColor(color)
        rtStatusText.text = text
        rtStatusText.setTextColor(color)
    }

    @OptIn(UnstableApi::class)
    private fun connect(rtspUrl: String) {
        if (rtspUrl.isBlank()) return

        rememberUrl(rtspUrl)

        player?.release()
        setRtStatus("Connecting…", colorAmber)

        val mediaSourceFactory = RtspMediaSource.Factory()

        // Minimal buffering to stay close to the live edge; time thresholds override the byte-size heuristic.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(50, 50, 0, 0)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()

        // Skip audio decoding/A-V sync entirely — not needed for a camera feed and it removes
        // one more source of buffering delay.
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> setRtStatus("Connecting…", colorAmber)
                    Player.STATE_READY -> setRtStatus("Live — ready for RT processing", colorGreen)
                    Player.STATE_ENDED -> setRtStatus("Stream ended", colorGray)
                    Player.STATE_IDLE -> setRtStatus("Idle", colorGray)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                setRtStatus("Error: ${error.errorCodeName}", colorRed)
            }
        })

        playerView.player = exoPlayer
        player = exoPlayer

        exoPlayer.setMediaItem(MediaItem.fromUri(rtspUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
