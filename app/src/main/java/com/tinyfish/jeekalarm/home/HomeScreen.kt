package com.tinyfish.jeekalarm.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.SettingsService
import com.tinyfish.jeekalarm.alarm.NotificationScreen
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.edit.EditScreen
import com.tinyfish.jeekalarm.edit.EditViewModel
import com.tinyfish.jeekalarm.ifly.IFly
import com.tinyfish.jeekalarm.recyclebin.RecycleBinScreen
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.schedule.ScheduleService
import com.tinyfish.jeekalarm.settings.SettingsScreen
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.jeekalarm.start.EditRoute
import com.tinyfish.jeekalarm.start.HomeRoute
import com.tinyfish.jeekalarm.start.RecycleBinRoute
import com.tinyfish.jeekalarm.start.SettingsRoute
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.MyTopBar
import com.tinyfish.ui.WidthSpacer
import com.tinyfish.ui.theme.JeekAlarmTheme
import kotlinx.coroutines.launch
import java.util.Calendar

/** 底部导航的两个标签页（仅用于高亮当前页）。 */
enum class BottomTab { HOME, SETTINGS }

@Composable
fun MainUI() {
    JeekAlarmTheme(SettingsService.theme) {
        val navController = rememberNavController()

        // 进入编辑页：先准备好编辑副本，再导航。
        val openEdit: (Int) -> Unit = { id ->
            EditViewModel.startEditing(id)
            navController.navigate(EditRoute)
        }
        // 底部导航在 Home/Settings 两个标签间切换，保持单实例、不堆栈。
        val goHome: () -> Unit = {
            navController.popBackStack(HomeRoute, inclusive = false)
        }
        val goSettings: () -> Unit = {
            navController.navigate(SettingsRoute) {
                launchSingleTop = true
                popUpTo(HomeRoute)
            }
        }
        val openRecycleBin: () -> Unit = {
            navController.navigate(RecycleBinRoute)
        }

        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // 横向滑动：前进时新页从右滑入、旧页左推；返回时反向。
            enterTransition = { slideInHorizontally(tween(220)) { it } },
            exitTransition = { slideOutHorizontally(tween(220)) { -it } },
            popEnterTransition = { slideInHorizontally(tween(220)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(220)) { it } },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenEdit = openEdit,
                    onAdd = { openEdit(-1) },
                    onNavigateHome = goHome,
                    onNavigateSettings = goSettings,
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onAdd = { openEdit(-1) },
                    onNavigateHome = goHome,
                    onNavigateSettings = goSettings,
                    onOpenRecycleBin = openRecycleBin,
                )
            }
            composable<EditRoute> {
                EditScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable<RecycleBinRoute> {
                RecycleBinScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        // 闹钟响铃时的全屏浮层：纯由 currentAlarmIds 状态驱动，覆盖在导航之上。
        // 因此后台 Service/广播只需更新闹钟状态，完全不必参与导航。
        if (NotificationService.currentAlarmIds.isNotEmpty()) {
            NotificationScreen()
        }
    }
}

@Composable
fun HomeScreen(
    onOpenEdit: (Int) -> Unit,
    onAdd: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 左滑删除：移入回收站（不再提供 undo），弹个简短提示。回收站里可恢复或彻底删除。
    val onDelete: (Schedule) -> Unit = { schedule ->
        ScheduleService.recycle(schedule)
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Moved \"${schedule.name}\" to recycle bin",
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_alarm, "JeekAlarm") },
        bottomBar = {
            NavigationBottomBar(
                selected = BottomTab.HOME,
                onNavigateHome = onNavigateHome,
                onNavigateSettings = onNavigateSettings,
                onAdd = onAdd,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ScheduleList(
            Modifier.padding(padding),
            onOpenEdit = onOpenEdit,
            onAdd = onAdd,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun ScheduleList(
    modifier: Modifier = Modifier,
    onOpenEdit: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Schedule) -> Unit,
) {
    val schedules = ScheduleService.scheduleList

    if (schedules.isEmpty()) {
        EmptyState(modifier, onAdd)
        return
    }

    // 记录当前滑开露出删除按钮的条目，保证同时只展开一个：展开新条目会收起旧的。
    var openedId by remember { mutableStateOf<Int?>(null) }

    val now = Calendar.getInstance()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(schedules, key = { it.id }) { schedule ->
            ScheduleItem(
                schedule = schedule,
                now = now,
                openedId = openedId,
                onOpenedChange = { openedId = it },
                onOpenEdit = onOpenEdit,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            ImageVector.vectorResource(R.drawable.ic_alarm),
            null,
            Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
        HeightSpacer(20.dp)
        Text("No alarms yet", style = MaterialTheme.typography.titleLarge)
        HeightSpacer(4.dp)
        Text(
            "Tap + to add your first alarm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        HeightSpacer(24.dp)
        Button(onClick = onAdd) {
            Icon(ImageVector.vectorResource(R.drawable.ic_add), null)
            WidthSpacer(8.dp)
            Text("Add alarm")
        }
    }
}

private enum class SwipeAnchor { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleItem(
    schedule: Schedule,
    now: Calendar,
    openedId: Int?,
    onOpenedChange: (Int?) -> Unit,
    onOpenEdit: (Int) -> Unit,
    onDelete: (Schedule) -> Unit,
) {
    val isNext = schedule.id in App.nextAlarmIds

    // 左滑不再一滑即删，而是把卡片停靠到左侧、露出删除按钮，点按钮才真正删除，避免误触。
    val actionWidth = 88.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val dragState = remember { AnchoredDraggableState(initialValue = SwipeAnchor.Closed) }
    SideEffect {
        dragState.updateAnchors(
            DraggableAnchors {
                SwipeAnchor.Closed at 0f
                SwipeAnchor.Open at -actionWidthPx
            }
        )
    }
    // 本条滑开时登记为当前展开项；别的条目成为展开项时，收起自己。
    LaunchedEffect(dragState.settledValue) {
        if (dragState.settledValue == SwipeAnchor.Open) onOpenedChange(schedule.id)
    }
    LaunchedEffect(openedId) {
        if (openedId != schedule.id && dragState.currentValue != SwipeAnchor.Closed) {
            dragState.animateTo(SwipeAnchor.Closed)
        }
    }

    Box(Modifier.fillMaxWidth()) {
        // 背景层：停靠在右侧的删除按钮，随卡片滑开而露出。
        Row(
            Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable {
                        onOpenedChange(null)
                        onDelete(schedule)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_remove),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    HeightSpacer(2.dp)
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val x = dragState.offset
                    IntOffset(if (x.isNaN()) 0 else x.roundToInt(), 0)
                }
                .anchoredDraggable(dragState, Orientation.Horizontal)
                .clickable {
                    // 已滑开时，点一下先收回；否则进入编辑。
                    if (dragState.currentValue == SwipeAnchor.Open) onOpenedChange(null)
                    else onOpenEdit(schedule.id)
                }
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            schedule.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (schedule.enabled)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isNext) {
                            WidthSpacer(8.dp)
                            NextBadge()
                        }
                    }
                    HeightSpacer(2.dp)
                    Text(
                        App.format(schedule.getNextTriggerTime(now)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        schedule.timeConfig,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { checked -> ScheduleService.setEnabled(schedule.id, checked) },
                )
            }
        }
    }
}

@Composable
private fun NextBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            "NEXT",
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun NavigationBottomBar(
    selected: BottomTab,
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
    onAdd: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == BottomTab.HOME,
            onClick = onNavigateHome,
            label = { Text("Home") },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_home), "Home") },
        )

        AddNavItem(onAdd)

        NavigationBarItem(
            selected = selected == BottomTab.SETTINGS,
            onClick = onNavigateSettings,
            label = { Text("Settings") },
            icon = { Icon(ImageVector.vectorResource(R.drawable.ic_settings), "Settings") },
        )
    }
}

// 单击直接进入编辑页手动添加，按住则先进编辑页再弹出语音识别。
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.AddNavItem(onAdd: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Box(
        Modifier
            .weight(1f)
            .height(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .combinedClickable(
                    onClick = onAdd,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAdd()
                        IFly.showDialog(context) { recognizedName ->
                            EditViewModel.update { it.copy(name = recognizedName) }
                            EditViewModel.guessFromName()
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_add),
                "Add alarm",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    if (ScheduleService.scheduleList.isEmpty()) {
        ScheduleService.scheduleList.addAll(
            listOf(Schedule(name = "Alarm1"), Schedule(name = "Alarm2"))
        )
    }
    JeekAlarmTheme("Dark") {
        HomeScreen(
            onOpenEdit = {},
            onAdd = {},
            onNavigateHome = {},
            onNavigateSettings = {},
        )
    }
}
