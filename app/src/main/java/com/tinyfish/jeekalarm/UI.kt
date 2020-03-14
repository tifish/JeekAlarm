package com.tinyfish.jeekalarm

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Spacer
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.schedule.ScheduleManager

enum class ScreenType {
    MAIN,
    EDIT,
    SETTINGS,
}

object UI {
    lateinit var screen: MutableState<ScreenType>
    lateinit var nextAlarmIndexes: MutableState<List<Int>>
    lateinit var scheduleChangeTrigger: MutableState<Int>
    lateinit var isPlaying: MutableState<Boolean>
    lateinit var isRemoving: MutableState<Boolean>

    var initialized = false

    @Composable
    fun init() {
        if (initialized)
            return

        screen = state { ScreenType.MAIN }
        nextAlarmIndexes = state { ScheduleManager.nextAlarmIndexes.toList() }
        scheduleChangeTrigger = state { 0 }
        isPlaying = state { false }
        isRemoving = state { false }

        initialized = true
    }
}

@Composable
fun HeightSpacer(height: Dp = 10.dp) {
    Spacer(LayoutHeight(height))
}

@Composable
fun WidthSpacer(width: Dp = 10.dp) {
    Spacer(LayoutWidth(width))
}
