package com.tinyfish.jeekalarm

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Spacer
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.schedule.ScheduleManager

enum class ScreenType {
    MAIN,
    EDIT,
}

var uiInitialized = false

object UI {
    var screen by state { ScreenType.MAIN }
    var nextAlarmIndexes by state { ScheduleManager.nextAlarmIndexes.toList() }
    var scheduleChangeTrigger by state { 0 }
    var isPlaying by state { false }
    var isDeleting by state { false }

    fun init() {
        uiInitialized = true
    }
}

@Composable
fun HeightSpacer() {
    Spacer(LayoutHeight(10.dp))
}

@Composable
fun WidthSpacer() {
    Spacer(LayoutWidth(10.dp))
}
