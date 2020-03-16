package com.tinyfish.jeekalarm.settings

import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.res.vectorResource
import androidx.ui.unit.dp
import com.tinyfish.jeekalarm.*
import com.tinyfish.jeekalarm.edit.FileSelector
import com.tinyfish.jeekalarm.ui.MyBottomBar
import com.tinyfish.jeekalarm.ui.MyFileSelect
import com.tinyfish.jeekalarm.ui.MyTopBar
import com.tinyfish.jeekalarm.ui.SimpleVectorButton

@Composable
fun SettingsScreen() {
    Column {
        MyTopBar(R.drawable.ic_settings, "Settings")
        Surface(
            color = MaterialTheme.colors().background,
            modifier = LayoutFlexible(1f, true)
        ) {
            Editor()
        }
        BottomBar()
    }
}

@Composable
private fun Editor() {
    Column(LayoutPadding(20.dp)) {
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music File:", Config.data.defaultMusicFile,
                onSelect = {
                    FileSelector.openMusicFile {
                        Config.data.defaultMusicFile = it?.path?.substringAfter(':')!!
                        recomposeFileSelect()
                    }
                },
                onClear = {
                    Config.data.defaultMusicFile = ""
                    recomposeFileSelect()
                }
            )
        }

        HeightSpacer()
        Recompose { recomposeFileSelect ->
            MyFileSelect("Music Folder:", Config.data.defaultMusicFolder,
                onSelect = {
                    FileSelector.openFolder {
                        Config.data.defaultMusicFolder = it?.path?.substringAfter(':')!!
                        recomposeFileSelect()
                    }
                },
                onClear = {
                    Config.data.defaultMusicFolder = ""
                    recomposeFileSelect()
                }
            )
        }
    }
}

@Composable
private fun BottomBar() {
    MyBottomBar {
        SimpleVectorButton(vectorResource(R.drawable.ic_done), "OK") {
            onSettingsScreenPressOK()
        }
    }
}

fun onSettingsScreenPressOK() {
    Config.save()
    UI.screen.value = ScreenType.MAIN
}
