package com.octo.bttest

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.octo.bttest.ui.theme.BTTestTheme


fun LOGD(msg: String) {
    Log.d("BTTest", "[BT] $msg")
}

fun LOGE(msg: String, th: Throwable? = null) {
    Log.e("BTTest", "[BT] $msg", th)
}

class MainActivity : ComponentActivity(), AudioManager.OnAudioFocusChangeListener {
    private lateinit var mediaPlayer: MediaPlayer
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    private lateinit var mediaSession3: MediaSession
    private lateinit var exoPlayer: ExoPlayer

    private val isPlaying = mutableStateOf(false)

    private val lastCommand = mutableStateOf("---")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMedia3Session()
        startPlayer()
        setContent {
            BTTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = {
                                playPause()
                            }) {
                                Text(text = if (isPlaying.value) "STOP" else "START")
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(text = "Last command: ${lastCommand.value}")
                        }
                    }
                }
            }
        }
    }

    private fun startPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer.isLooping = true
        play()

    }

    private fun play() {
        mediaPlayer.start()
        isPlaying.value = true
    }

    private fun pause() {
        mediaPlayer.pause()
        isPlaying.value = false
    }

    private fun playPause() {
        if (mediaPlayer.isPlaying) {
            pause()
        } else {
            play()
        }

        isPlaying.value = mediaPlayer.isPlaying
    }

    private fun setupMedia3Session() {
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession3 = MediaSession.Builder(this, exoPlayer)
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    handleMediaCommand(session, controller, playerCommand)
                    return SessionResult.RESULT_SUCCESS
                }
            })
            .build()
    }

    private fun handleMediaCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int
    ) {
        val str = playerCommand.toCommandString()
        LOGD("MEDIA COMMAND $str")
        lastCommand.value = str

        when (playerCommand) {
            Player.COMMAND_PLAY_PAUSE -> playPause()
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        LOGD("FOCUS CHANGE: $focusChange")
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this).build()
            )
        } else {
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    companion object {
        private fun Int.toCommandString() = when(this) {
            Player.COMMAND_INVALID -> "INVALID"
            Player.COMMAND_PLAY_PAUSE -> "PLAY_PAUSE"
            Player.COMMAND_PREPARE -> "PREPARE"
            Player.COMMAND_SET_VOLUME -> "SET_VOLUME"
            Player.COMMAND_STOP -> "STOP"
            else -> "UNKNOWN"
        }
    }
}