package com.tinyfish.jeekalarm.alarm

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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

    // 横屏高度不够竖排图标+文字+按钮，改成左图标、右文字和按钮。
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // targetSdk 35 起强制 edge-to-edge，浮层要自己避开系统栏/刘海，否则按钮会被导航栏盖住。
    Surface(Modifier.fillMaxSize()) {
        if (landscape) {
            Row(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulsingAlarmIcon(96.dp)
                WidthSpacer(48.dp)
                AlarmInfoAndActions(Modifier.weight(1f, fill = false), gapBeforeButtons = 24.dp)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PulsingAlarmIcon(120.dp)
                HeightSpacer(40.dp)
                AlarmInfoAndActions(gapBeforeButtons = 48.dp)
            }
        }
    }
}

@Composable
private fun AlarmInfoAndActions(modifier: Modifier = Modifier, gapBeforeButtons: Dp) {
    // 同时响的闹钟多了也可能放不下，允许滚动，保证按钮总能够到。
    Column(
        modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        HeightSpacer(gapBeforeButtons)

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

@Composable
private fun PulsingAlarmIcon(size: Dp) {
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
            .size(size)
            .scale(scale),
        tint = MaterialTheme.colorScheme.primary,
    )
}
