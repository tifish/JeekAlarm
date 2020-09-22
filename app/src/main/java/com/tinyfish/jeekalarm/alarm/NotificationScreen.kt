package com.tinyfish.jeekalarm.alarm

import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope.align
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.ScheduleHome
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.SimpleVectorButton

@Composable
fun NotificationScreen() {
    ScheduleHome.pausePlaying()

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
        for (alarmIndex in App.notificationAlarmIndexes) {
            val schedule = ScheduleHome.scheduleList[alarmIndex]

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
            val text = if (App.isPlaying.value) "Pause" else "Play"
            val onClick = {
                if (App.isPlaying.value)
                    ScheduleHome.pausePlaying()
                else
                    ScheduleHome.resumePlaying()
            }
            if (App.isPlaying.value)
                SimpleVectorButton(
                    vectorResource(R.drawable.ic_pause),
                    text,
                    onClick
                )
            else
                SimpleVectorButton(
                    vectorResource(R.drawable.ic_play_arrow),
                    text,
                    onClick
                )

            Spacer(Modifier.preferredWidth(36.dp))

            SimpleVectorButton(
                vectorResource(R.drawable.ic_close),
                "Dismiss"
            ) {
                App.screen.value = App.screenBeforeNotification
                App.notificationAlarmIndexes.clear()
            }
        }
    }
}
