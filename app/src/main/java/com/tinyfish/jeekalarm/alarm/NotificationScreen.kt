package com.tinyfish.jeekalarm.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.WidthSpacer

@Composable
fun NotificationScreen() {
    // 响铃浮层显示时吞掉系统返回键，必须显式 Pause/Dismiss 才能离开。
    BackHandler {}

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PulsingAlarmIcon()

            HeightSpacer(40.dp)

            for (alarmId in NotificationService.currentAlarmIds) {
                val schedule = ScheduleService.scheduleList.firstOrNull { it.id == alarmId } ?: continue

                Text(
                    schedule.name,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    schedule.timeConfig,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                HeightSpacer(12.dp)
            }

            HeightSpacer(48.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val isPlaying = App.isPlaying

                FilledTonalButton(onClick = {
                    if (isPlaying)
                        ScheduleService.pausePlaying()
                    else
                        ScheduleService.resumePlaying()
                    NotificationService.updateAlarm()
                }) {
                    Icon(
                        ImageVector.vectorResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        null,
                    )
                    WidthSpacer(8.dp)
                    Text(if (isPlaying) stringResource(R.string.action_pause) else stringResource(R.string.action_play))
                }

                Button(
                    onClick = { NotificationService.cancelAlarm() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_close), null)
                    WidthSpacer(8.dp)
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        }
    }
}

@Composable
private fun PulsingAlarmIcon() {
    val transition = rememberInfiniteTransition(label = "alarm")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Icon(
        ImageVector.vectorResource(R.drawable.ic_alarm),
        null,
        Modifier
            .size(120.dp)
            .scale(scale),
        tint = MaterialTheme.colorScheme.primary,
    )
}
