package com.tinyfish.jeekalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.start.App
import java.io.File
import java.util.concurrent.Executors

object MusicService {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 播放相关的慢操作（扫描音乐文件夹、MediaPlayer.prepare）都排到这条单线程上。
     * 放在主线程会卡住输入分发，系统判 ANR 直接杀进程——闹钟看起来就是"顿几秒然后界面消失、声音没了"。
     * 用单线程还顺带保证了 play / stop / pause 的先后顺序，且 MediaPlayer 只被一个线程碰。
     */
    private val playbackExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AlarmPlayback")
    }

    fun runSerially(block: () -> Unit) {
        playbackExecutor.execute(block)
    }

    private fun syncPlayingState(playing: Boolean) {
        mainHandler.post {
            App.isPlaying = playing
            NotificationService.updateAlarm()
        }
    }

    // 媒体服务进程挂掉（MEDIA_ERROR_SERVER_DIED）时这个实例就废了，只能释放重建，所以不能用 by lazy。
    // 只在播放线程上访问。
    private var mediaPlayerOrNull: MediaPlayer? = null

    private val mediaPlayer: MediaPlayer
        get() = mediaPlayerOrNull ?: MediaPlayer().apply {
            setOnErrorListener { _, what, extra ->
                Log.e(this@MusicService.javaClass.name, "MediaPlayer error what=$what extra=$extra")
                // 回调落在主线程，绕回播放线程再收拾。
                runSerially { handlePlaybackError(what) }
                true
            }
            mediaPlayerOrNull = this
        }
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
            // 必须显式给 Handler：这个请求是在没有 Looper 的播放线程上懒加载的，
            // 不带 Handler 的重载会去取当前线程的 Looper，直接抛 IllegalStateException。
            .setOnAudioFocusChangeListener({ handleAudioFocusChange(it) }, mainHandler)
            .build()
    }
    private var prepared = false
    private var hasAudioFocus = false
    private var pausedByFocusLoss = false

    fun play(musicPath: String, loop: Boolean = true) {
        val uri =
            if (musicPath.startsWith("content://") || musicPath.startsWith("file://"))
                Uri.parse(musicPath)
            else
                Uri.fromFile(File(Environment.getExternalStorageDirectory().path, musicPath))
        play(uri, loop)
    }

    fun play(file: File, loop: Boolean = true) {
        play(Uri.fromFile(file), loop)
    }

    fun play(uri: Uri, loop: Boolean = true) {
        if (!requestAudioFocus())
            throw IllegalStateException("Audio focus request failed")

        try {
            mediaPlayer.apply {
                reset()
                prepared = false

                setDataSource(App.context, uri)

                setAudioAttributes(alarmAudioAttributes)
                setVolume(1f, 1f)

                isLooping = loop

                prepare()
                prepared = true
                pausedByFocusLoss = false
                start()
            }
        } catch (ex: Exception) {
            // prepare 失败是同步抛异常，走不到 OnErrorListener，这里自己复位回 Idle，
            // 否则播放器卡在错误状态，后面的 stop / isPlaying 都会再抛 IllegalStateException。
            runCatching { mediaPlayerOrNull?.reset() }
            prepared = false
            pausedByFocusLoss = false
            abandonAudioFocus()
            throw ex
        }
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
            mediaPlayer.setVolume(1f, 1f)
            mediaPlayer.start()
        } else {
            syncPlayingState(false)
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

    /**
     * 播放中途出错（文件损坏、解码失败、媒体服务挂掉）时的收尾。
     * 出错后 MediaPlayer 停在 Error 状态，isPlaying / stop 都会抛 IllegalStateException，
     * 必须复位；同时把响铃状态置回未播放，免得界面一直显示在播、关不掉。
     */
    private fun handlePlaybackError(what: Int) {
        prepared = false
        pausedByFocusLoss = false

        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // 媒体服务进程没了，这个实例救不回来，释放掉等下次播放时重建。
            runCatching { mediaPlayerOrNull?.release() }
            mediaPlayerOrNull = null
        } else {
            runCatching { mediaPlayerOrNull?.reset() }
        }

        abandonAudioFocus()
        syncPlayingState(false)
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        // 回调落在主线程，绕回播放线程再动 MediaPlayer。
        runSerially { applyAudioFocusChange(focusChange) }
    }

    private fun applyAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (prepared) {
                    mediaPlayerOrNull?.setVolume(1f, 1f)
                    if (pausedByFocusLoss) {
                        pausedByFocusLoss = false
                        mediaPlayerOrNull?.start()
                        syncPlayingState(true)
                    }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                if (prepared && mediaPlayerOrNull?.isPlaying == true)
                    mediaPlayerOrNull?.pause()
                pausedByFocusLoss = false
                hasAudioFocus = false
                syncPlayingState(false)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (prepared && mediaPlayerOrNull?.isPlaying == true) {
                    mediaPlayerOrNull?.pause()
                    pausedByFocusLoss = true
                    syncPlayingState(false)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (prepared)
                    mediaPlayerOrNull?.setVolume(0.3f, 0.3f)
            }
        }
    }
}
