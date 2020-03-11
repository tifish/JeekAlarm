package com.tinyfish.jeekalarm.alarm

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.core.setContent
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.UI
import com.tinyfish.jeekalarm.main.DarkColorPalette
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.ui.SimpleVectorButton

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("NotificationActivity", "onCreate")

        val alarmIndexes = intent.extras!!.getIntArray("alarmIndexes")!!

        setContent {
            UI.init()
            NotificationUI(this, alarmIndexes)
        }
    }

    override fun onDestroy() {
        ScheduleManager.stopPlaying()

        super.onDestroy()
    }

}

@Composable
fun NotificationUI(notificationActivity: NotificationActivity, alarmIndexes: IntArray) {
    MaterialTheme(colors = DarkColorPalette) {
        Surface(color = MaterialTheme.colors().background) {
            Center {
                NotificationContent(notificationActivity, alarmIndexes)
            }
        }
    }
}

@Composable
fun NotificationContent(notificationActivity: NotificationActivity, alarmIndexes: IntArray) {
    val textStyle = TextStyle(fontSize = 32.sp)

    Column {
        for (alarmIndex in alarmIndexes) {
            val schedule = ScheduleManager.scheduleList[alarmIndex]

            Text(schedule.name, style = textStyle)
            Text(schedule.timeConfig, style = textStyle)

            Spacer(LayoutHeight(16.dp))
        }

        Row {
            SimpleVectorButton(
                vectorResource(if (UI.isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                if (UI.isPlaying.value) "Pause" else "Play"
            ) {
                if (UI.isPlaying.value)
                    ScheduleManager.pausePlaying()
                else
                    ScheduleManager.resumePlaying()
            }

            Spacer(LayoutWidth(36 .dp))

            SimpleVectorButton(vectorResource(R.drawable.ic_close), "Dismiss") {
                notificationActivity.finish()
            }
        }
    }
}
