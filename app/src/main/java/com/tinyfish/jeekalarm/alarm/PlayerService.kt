package com.tinyfish.jeekalarm.alarm

import android.app.Service
import android.content.Intent
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class PlayerService : Service() {
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSession(this, "PlayerService")
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setState(
                    PlaybackState.STATE_PLAYING,
                    0,
                    1f
                ) //you simulate a player which plays something.
                .build()
        )
        //this will only work on Lollipop and up, see https://code.google.com/p/android/issues/detail?id=224134
        val myVolumeProvider: VolumeProvider =
            object : VolumeProvider(
                VOLUME_CONTROL_RELATIVE,  /*max volume*/
                10,  /*initial volume level*/
                5
            ) {
                override fun onAdjustVolume(direction: Int) {
                    if (App.isPlaying.value)
                        ScheduleManager.pausePlaying()
                    /*
                    -1 -- volume down
                    1 -- volume up
                    0 -- volume button released
                    */
                }
            }
        mediaSession.setPlaybackToRemote(myVolumeProvider)
        mediaSession.isActive = true
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}