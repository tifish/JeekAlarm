package com.tinyfish.jeekalarm.alarm

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.App
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.ScheduleManager
import com.tinyfish.jeekalarm.ui.SimpleVectorButton

@Composable
fun NotificationScreen() {
    ScheduleManager.pausePlaying()

    Surface(color = MaterialTheme.colors().background) {
        Center {
            NotificationContent()
        }
    }
}

@Composable
fun NotificationContent() {
    val textStyle = TextStyle(fontSize = 32.sp)

    Column {
        for (alarmIndex in App.notificationAlarmIndexes) {
            val schedule = ScheduleManager.scheduleList[alarmIndex]

            Text(schedule.name, style = textStyle)
            Text(schedule.timeConfig, style = textStyle)

            Spacer(LayoutHeight(16.dp))
        }

        Row {
            val text = if (App.isPlaying.value) "Pause" else "Play"
            val onClick = {
                if (App.isPlaying.value)
                    ScheduleManager.pausePlaying()
                else
                    ScheduleManager.resumePlaying()
            }
            if (App.isPlaying.value)
                SimpleVectorButton(vectorResource(R.drawable.ic_pause), text, onClick)
            else
                SimpleVectorButton(vectorResource(R.drawable.ic_play_arrow), text, onClick)

            Spacer(LayoutWidth(36.dp))

            SimpleVectorButton(vectorResource(R.drawable.ic_close), "Dismiss") {
                App.screen.value = App.screenBeforeNotification
                App.notificationAlarmIndexes.clear()
            }
        }
    }
}
