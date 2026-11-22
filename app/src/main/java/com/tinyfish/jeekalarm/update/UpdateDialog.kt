package com.tinyfish.jeekalarm.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tinyfish.jeekalarm.PermissionsService
import com.tinyfish.jeekalarm.R
import com.tinyfish.ui.HeightSpacer

@Composable
fun UpdateDialog() {
    if (!UpdateService.showDialog) return

    val context = LocalContext.current
    when (val state = UpdateService.state) {
        is UpdateState.Available -> AvailableUpdateDialog(
            state = state,
            onUpdate = { UpdateService.startDownload(context, state.release) },
            onLater = { UpdateService.dismissDialog() },
            onSkip = { UpdateService.skipVersion(state.release.versionName) },
        )

        is UpdateState.Downloading -> DownloadingUpdateDialog(
            state = state,
            onHide = { UpdateService.dismissDialog() },
            onCancel = { UpdateService.cancelDownload() },
        )

        is UpdateState.ReadyToInstall -> {
            var resumeRefreshKey by remember { mutableIntStateOf(0) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                resumeRefreshKey++
            }
            val canInstall = remember(resumeRefreshKey) {
                PermissionsService.canRequestPackageInstalls(context)
            }
            ReadyToInstallDialog(
                state = state,
                canInstall = canInstall,
                onInstall = { UpdateService.installDownloadedApk(context) },
                onLater = { UpdateService.dismissDialog() },
            )
        }

        is UpdateState.Error -> UpdateErrorDialog(
            state = state,
            onRetry = {
                if (state.release != null) {
                    UpdateService.startDownload(context, state.release)
                } else {
                    UpdateService.dismissDialog()
                    UpdateService.checkForUpdateManually()
                }
            },
            onOpenBrowser = state.release?.let { release ->
                {
                    UpdateService.openReleasePage(context, release.htmlUrl)
                    UpdateService.dismissDialog()
                }
            },
            onClose = { UpdateService.dismissDialog() },
        )

        else -> Unit
    }
}

@Composable
private fun AvailableUpdateDialog(
    state: UpdateState.Available,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
) {
    val release = state.release
    val hasApk = release.apkAsset != null
    val sizeText = release.apkAsset?.sizeBytes?.takeIf { it > 0L }?.let {
        UpdateParser.formatFileSize(it)
    }

    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(stringResource(R.string.update_dialog_available_title, release.tagName))
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                val versionLine = buildString {
                    append(UpdateService.formattedCurrentVersion())
                    append(" → ")
                    append(release.tagName)
                    if (sizeText != null) {
                        append(" · ")
                        append(sizeText)
                    }
                }
                Text(
                    versionLine,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                HeightSpacer(8.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        release.releaseNotes.ifBlank {
                            stringResource(R.string.update_dialog_no_notes)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.fromAutoCheck) {
                    HeightSpacer(8.dp)
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.update_action_skip_version))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(
                    stringResource(
                        if (hasApk) R.string.update_action_download_install
                        else R.string.update_action_open_release_page
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.update_action_later))
            }
        },
    )
}

@Composable
private fun DownloadingUpdateDialog(
    state: UpdateState.Downloading,
    onHide: () -> Unit,
    onCancel: () -> Unit,
) {
    val progress = state.progress
    val downloadedText = UpdateParser.formatFileSize(state.downloadedBytes)
    val detailText = if (progress != null) {
        val percent = (progress * 100).toInt()
        val totalText = UpdateParser.formatFileSize(state.totalBytes)
        "$percent% · $downloadedText / $totalText"
    } else {
        downloadedText
    }

    AlertDialog(
        onDismissRequest = onHide,
        title = {
            Text(stringResource(R.string.update_dialog_downloading_title, state.release.tagName))
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                HeightSpacer(8.dp)
                Text(
                    detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onHide) {
                Text(stringResource(R.string.update_action_background))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ReadyToInstallDialog(
    state: UpdateState.ReadyToInstall,
    canInstall: Boolean,
    onInstall: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(stringResource(R.string.update_dialog_ready_title, state.release.tagName))
        },
        text = {
            Text(
                stringResource(
                    if (canInstall) R.string.update_dialog_ready_message
                    else R.string.update_dialog_permission_needed
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onInstall) {
                Text(
                    stringResource(
                        if (canInstall) R.string.update_action_install_now
                        else R.string.update_action_allow_and_install
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.update_action_later))
            }
        },
    )
}

@Composable
private fun UpdateErrorDialog(
    state: UpdateState.Error,
    onRetry: () -> Unit,
    onOpenBrowser: (() -> Unit)?,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.update_dialog_error_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                if (onOpenBrowser != null) {
                    HeightSpacer(8.dp)
                    TextButton(onClick = onOpenBrowser) {
                        Text(stringResource(R.string.update_action_open_release_page))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.update_action_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.action_dismiss))
            }
        },
    )
}
