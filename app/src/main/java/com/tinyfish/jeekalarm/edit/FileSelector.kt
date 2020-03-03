package com.tinyfish.jeekalarm.edit

import android.content.Intent
import android.net.Uri
import com.tinyfish.jeekalarm.main.MainActivity

object FileSelector {
    // Request code for selecting a PDF document.
    const val PICK_MP3_FILE = 1
    const val PICK_FOLDER = 2

    fun openMusicFile(onFileSelect: (uri: Uri?) -> Unit) {
        MainActivity.onFileSelect = onFileSelect

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }

        MainActivity.instance?.startActivityForResult(intent, PICK_MP3_FILE)
    }

    fun openFolder(onFileSelect: (uri: Uri?) -> Unit) {
        MainActivity.onFileSelect = onFileSelect

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        MainActivity.instance?.startActivityForResult(intent, PICK_FOLDER)
    }
}