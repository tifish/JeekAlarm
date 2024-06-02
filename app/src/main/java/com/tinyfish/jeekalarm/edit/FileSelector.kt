package com.tinyfish.jeekalarm.edit

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object FileSelector {
    private var selectFileResultLauncher: ActivityResultLauncher<String>? = null
    private var selectFolderResultLauncher: ActivityResultLauncher<Uri?>? = null

    fun init(activity: ComponentActivity) {
        if (selectFileResultLauncher != null)
            return

        val callback: (Uri?) -> Unit = {
            if (it != null)
                onSelected(it)
        }

        selectFileResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent(), callback
        )

        selectFolderResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(), callback
        )
    }

    private var onSelected: (uri: Uri) -> Unit = {}

    fun openMusicFile(onFileSelected: (uri: Uri) -> Unit) {
        this.onSelected = onFileSelected
        selectFileResultLauncher?.launch("audio/*")
    }

    fun openFolder(onFolderSelected: (uri: Uri) -> Unit) {
        this.onSelected = onFolderSelected
        selectFolderResultLauncher?.launch(null)
    }
}