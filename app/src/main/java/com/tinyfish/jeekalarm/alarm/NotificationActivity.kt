package com.tinyfish.jeekalarm.alarm

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.core.setContent
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.tinyfish.jeekalarm.UI
import com.tinyfish.jeekalarm.schedule.ScheduleManager

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("NotificationActivity", "onCreate")

        val alarmIndexes = intent.extras!!.getIntArray("alarmIndexes")!!

        setContent {
            UI.init()
            NotificationUI(this, alarmIndexes)
        }
    }

    override fun onDestroy() {
        ScheduleManager.stopPlaying()

        super.onDestroy()
    }

}

@Composable
fun NotificationUI(notificationActivity: NotificationActivity, alarmIndexes: IntArray) {
    Center {
        val textStyle = TextStyle(fontSize = 32.sp)

        Column {
            for (alarmIndex in alarmIndexes) {
                val schedule = ScheduleManager.scheduleList[alarmIndex]

                Text(schedule.name, style = textStyle)
                Text(schedule.timeConfig, style = textStyle)

                Spacer(LayoutHeight(16.dp))
            }

            Row {
                if (UI.isPlaying) {
                    Button(onClick = {
                        ScheduleManager.pausePlaying()
                    }) {
                        Text("Pause")
                    }
                } else {
                    Button(onClick = {
                        ScheduleManager.resumePlaying()
                    }) {
                        Text("Play")
                    }
                }

                Spacer(LayoutWidth(10.dp))

                Button(onClick = {
                    notificationActivity.finish()
                }) {
                    Text("Close")
                }
            }
        }
    }
}