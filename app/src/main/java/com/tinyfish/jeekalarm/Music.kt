package com.tinyfish.jeekalarm

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import java.io.File

object Music {
    private val mediaPlayer: MediaPlayer by lazy { MediaPlayer() }

    fun play(musicPath: String, loop: Boolean = true): MediaPlayer? {
        val file = File(Environment.getExternalStorageDirectory().path, musicPath)
        return play(file, loop)
    }

    fun play(file: File, loop: Boolean = true): MediaPlayer? {
        mediaPlayer.apply {
            reset()

            setDataSource(
                App.context,
                Uri.fromFile(file)
            )

            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )

            isLooping = loop

            setOnPreparedListener {
                start()
            }

            prepare()
        }

        return mediaPlayer
    }

    fun stop() {
        if (mediaPlayer.isPlaying)
            mediaPlayer.stop()
    }

    fun pause() {
        mediaPlayer.pause()
    }

    fun resume() {
        mediaPlayer.start()
    }
}