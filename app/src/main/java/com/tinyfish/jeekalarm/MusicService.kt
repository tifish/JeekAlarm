package com.tinyfish.jeekalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import com.tinyfish.jeekalarm.start.App
import java.io.File

object MusicService {
    private val mediaPlayer: MediaPlayer by lazy { MediaPlayer() }
    private val audioManager: AudioManager by lazy {
        App.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val alarmAudioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAudioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { handleAudioFocusChange(it) }
            .build()
    }
    private var prepared = false
    private var hasAudioFocus = false
    private var pausedByFocusLoss = false

    fun play(musicPath: String, loop: Boolean = true): MediaPlayer {
        val uri =
            if (musicPath.startsWith("content://") || musicPath.startsWith("file://"))
                Uri.parse(musicPath)
            else
                Uri.fromFile(File(Environment.getExternalStorageDirectory().path, musicPath))
        return play(uri, loop)
    }

    fun play(file: File, loop: Boolean = true): MediaPlayer {
        return play(Uri.fromFile(file), loop)
    }

    fun play(uri: Uri, loop: Boolean = true): MediaPlayer {
        if (!requestAudioFocus())
            throw IllegalStateException("Audio focus request failed")

        try {
            mediaPlayer.apply {
                reset()
                prepared = false

                setDataSource(App.context, uri)

                setAudioAttributes(alarmAudioAttributes)

                isLooping = loop

                prepare()
                prepared = true
                pausedByFocusLoss = false
                start()
            }
        } catch (ex: Exception) {
            prepared = false
            pausedByFocusLoss = false
            abandonAudioFocus()
            throw ex
        }

        return mediaPlayer
    }

    fun stop() {
        if (!prepared)
            return

        if (mediaPlayer.isPlaying)
            mediaPlayer.stop()
        mediaPlayer.reset()
        prepared = false
        pausedByFocusLoss = false
        abandonAudioFocus()
    }

    fun pause() {
        if (prepared && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            abandonAudioFocus()
        }
    }

    fun resume() {
        if (prepared && requestAudioFocus()) {
            pausedByFocusLoss = false
            mediaPlayer.start()
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus)
            return true

        val result = audioManager.requestAudioFocus(audioFocusRequest)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus)
            return

        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                mediaPlayer.setVolume(1f, 1f)
                if (prepared && pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    mediaPlayer.start()
                    App.isPlaying = true
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                if (prepared && mediaPlayer.isPlaying)
                    mediaPlayer.pause()
                pausedByFocusLoss = false
                hasAudioFocus = false
                App.isPlaying = false
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (prepared && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    pausedByFocusLoss = true
                    App.isPlaying = false
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (prepared)
                    mediaPlayer.setVolume(0.3f, 0.3f)
            }
        }
    }
}
