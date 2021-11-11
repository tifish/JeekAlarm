package com.tinyfish.jeekalarm

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import com.tinyfish.jeekalarm.start.App
import java.io.File

object MusicHome {
    private val mediaPlayer: MediaPlayer by lazy { MediaPlayer() }

    fun play(musicPath: String, loop: Boolean = true): MediaPlayer? {
        val file = File(Environment.getExternalStorageDirectory().path, musicPath)
        return play(file, loop)
    }

    fun play(file: File, loop: Boolean = true): MediaPlayer? {
        return play(Uri.fromFile(file), loop)
    }

    fun play(uri: Uri, loop: Boolean = true): MediaPlayer {
        mediaPlayer.apply {
            reset()

            setDataSource(App.context, uri)

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