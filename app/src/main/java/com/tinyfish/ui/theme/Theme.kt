package com.tinyfish.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 关闭动态取色，统一使用品牌配色，保证整体观感一致。
// 想跟随系统壁纸取色（Android 12+），把此处改为 true 即可。
private const val USE_DYNAMIC_COLOR = false

@Composable
fun JeekAlarmTheme(
    themeSetting: String,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        USE_DYNAMIC_COLOR && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
