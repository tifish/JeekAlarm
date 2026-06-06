package com.tinyfish.jeekalarm.recyclebin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tinyfish.jeekalarm.R
import com.tinyfish.jeekalarm.schedule.RecycleBinService
import com.tinyfish.jeekalarm.schedule.Schedule
import com.tinyfish.jeekalarm.start.App
import com.tinyfish.ui.HeightSpacer
import com.tinyfish.ui.MyTopBar
import java.util.Calendar

@Composable
fun RecycleBinScreen(onNavigateBack: () -> Unit) {
    val items = RecycleBinService.recycleList
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { MyTopBar(R.drawable.ic_delete, stringResource(R.string.label_recycle_bin), onBack = onNavigateBack) },
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text(stringResource(R.string.recycle_clear_all))
                    }
                }
            }
            items(items, key = { it.id }) { schedule ->
                RecycleItem(
                    schedule = schedule,
                    onRestore = { RecycleBinService.restore(schedule) },
                    onDeleteForever = { RecycleBinService.deleteForever(schedule) },
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.recycle_clear_confirm_title)) },
            text = { Text(stringResource(R.string.recycle_clear_confirm_message, items.size)) },
            confirmButton = {
                TextButton(onClick = {
                    RecycleBinService.clearAll()
                    showClearConfirm = false
                }) { Text(stringResource(R.string.action_delete_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun RecycleItem(
    schedule: Schedule,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    schedule.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HeightSpacer(2.dp)
                Text(
                    schedule.timeConfig,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HeightSpacer(2.dp)
                Text(
                    deletedStatus(schedule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_restore),
                    contentDescription = stringResource(R.string.action_restore),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDeleteForever) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete_forever),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun deletedStatus(schedule: Schedule): String {
    val deletedCal = Calendar.getInstance().apply { timeInMillis = schedule.deletedAt }
    val days = RecycleBinService.daysUntilRemoval(schedule)
    val res = App.localizedContext().resources
    val removal = if (days <= 0)
        res.getString(R.string.recycle_removes_today)
    else
        res.getQuantityString(R.plurals.recycle_removes_in_days, days, days)
    return res.getString(R.string.recycle_item_status, App.format(deletedCal), removal)
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            ImageVector.vectorResource(R.drawable.ic_delete),
            null,
            Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
        HeightSpacer(20.dp)
        Text(stringResource(R.string.recycle_empty_title), style = MaterialTheme.typography.titleLarge)
        HeightSpacer(4.dp)
        Text(
            stringResource(R.string.recycle_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
