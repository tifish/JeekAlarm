package com.tinyfish.jeekalarm.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.alarm.NotificationService
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.schedule.ScheduleService


class MainActivity : AppCompatActivity() {
    // 标记本次 Activity 实例是被闹钟全屏 Intent 拉到前台的——关闭响铃时要送回后台，
    // 让用户回到响铃前所在的 app 而不是停留在主界面。
    private var launchedForAlarm = false

    // 响铃时点亮屏幕、显示在锁屏之上；通知浮层本身由 UI 层观察响铃状态自动弹出。
    private fun showCurrentAlarms() {
        if (NotificationService.currentAlarmIds.isNotEmpty())
            allowAlarmOverLockScreen()
    }

    private fun allowAlarmOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // onNewIntent 只在外部 PendingIntent 拉起时触发——若此刻有响铃，说明是从 app 外被叫醒的。
        if (NotificationService.currentAlarmIds.isNotEmpty())
            launchedForAlarm = true

        showCurrentAlarms()
    }

    override fun onResume() {
        super.onResume()

        ScheduleService.setNextAlarm()
        showCurrentAlarms()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)

        PermissionsService.checkAndRequestPermissions(permissions.toTypedArray(), this)
        PermissionsService.requestExactAlarmPermissionIfNeeded(this)

        FileSelector.init(this)

        // 进程被全屏 Intent 冷启动时，currentAlarmIds 已被响铃服务填好；此情形也算从 app 外拉起。
        if (NotificationService.currentAlarmIds.isNotEmpty())
            launchedForAlarm = true

        NotificationService.onAlarmDismissed = {
            if (launchedForAlarm) {
                launchedForAlarm = false
                moveTaskToBack(true)
            }
        }

        showCurrentAlarms()

        // 返回键交给 Compose 的 NavHost 与各屏幕的 BackHandler 处理。
        setContent {
            MainUI()
        }
    }

    override fun onDestroy() {
        NotificationService.onAlarmDismissed = null
        super.onDestroy()
    }
}
