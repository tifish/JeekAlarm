package com.tinyfish.jeekalarm.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.SimpleVectorButton

@Composable
fun NotificationScreen() {
    ScheduleService.pausePlaying()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Box(Modifier.wrapContentSize()) {
            NotificationContent()
        }
    }
}

@Composable
fun NotificationContent() {
    val textStyle = TextStyle(fontSize = 32.sp)

    Column {
        for (alarmId in App.notificationAlarmIds) {
            val schedule = ScheduleService.scheduleList.filter { schedule -> schedule.id == alarmId }[0]

            Text(
                schedule.name,
                style = textStyle,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                schedule.timeConfig,
                style = textStyle,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))
        }

        HeightSpacer(36.dp)

        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            val text = if (App.isPlaying) "Pause" else "Play"
            val onClick = {
                if (App.isPlaying)
                    ScheduleService.pausePlaying()
                else
                    ScheduleService.resumePlaying()
            }
            if (App.isPlaying)
                SimpleVectorButton(
                    ImageVector.vectorResource(R.drawable.ic_pause),
                    text,
                    onClick
                )
            else
                SimpleVectorButton(
                    ImageVector.vectorResource(R.drawable.ic_play_arrow),
                    text,
                    onClick
                )

            Spacer(Modifier.width(36.dp))

            SimpleVectorButton(
                ImageVector.vectorResource(R.drawable.ic_close),
                "Dismiss"
            ) {
                NotificationService.cancelAlarm()
            }
        }
    }
}
