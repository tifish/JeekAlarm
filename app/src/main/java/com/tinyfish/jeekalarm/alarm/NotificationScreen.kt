package com.tinyfish.jeekalarm.alarm

import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.layout.*
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.res.vectorResource
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.ui.HeightSpacer
import com.tinyfish.jeekalarm.ui.SimpleVectorButton

@Composable
fun NotificationScreen() {
    ScheduleHome.pausePlaying()

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
            val schedule = ScheduleHome.scheduleList[alarmIndex]

            Text(schedule.name, style = textStyle, modifier = LayoutGravity.Center)
            Text(schedule.timeConfig, style = textStyle, modifier = LayoutGravity.Center)

            Spacer(LayoutHeight(16.dp))
        }

        HeightSpacer(36.dp)

        Row(modifier = LayoutGravity.Center) {
            val text = if (App.isPlaying.value) "Pause" else "Play"
            val onClick = {
                if (App.isPlaying.value)
                    ScheduleHome.pausePlaying()
                else
                    ScheduleHome.resumePlaying()
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
