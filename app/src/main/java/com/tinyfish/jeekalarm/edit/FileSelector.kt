package com.tinyfish.jeekalarm.edit

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object FileSelector {
    private var activity: ComponentActivity? = null
    private var selectFileResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var selectFolderResultLauncher: ActivityResultLauncher<Uri?>? = null

    fun init(activity: ComponentActivity) {
        if (selectFileResultLauncher != null)
            return

        this.activity = activity

        val callback: (Uri?) -> Unit = {
            if (it != null) {
                persistReadPermission(it)
                onSelected(it)
            }
        }

        selectFileResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument(), callback
        )

        selectFolderResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(), callback
        )
    }

    private var onSelected: (uri: Uri) -> Unit = {}

    fun openMusicFile(onFileSelected: (uri: Uri) -> Unit) {
        this.onSelected = onFileSelected
        selectFileResultLauncher?.launch(arrayOf("audio/*"))
    }

    fun openFolder(onFolderSelected: (uri: Uri) -> Unit) {
        this.onSelected = onFolderSelected
        selectFolderResultLauncher?.launch(null)
    }

    private fun persistReadPermission(uri: Uri) {
        val contentResolver = activity?.contentResolver ?: return
        val readFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val readWriteFlags = readFlags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, readWriteFlags)
        } catch (_: SecurityException) {
            try {
                contentResolver.takePersistableUriPermission(uri, readFlags)
            } catch (_: SecurityException) {
            }
        }
    }
}
