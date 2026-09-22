package com.tinyfish.jeekalarm.edit

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tinyfish.jeekalarm.home.MainActivity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class FileSelectorTest {
    @Test
    fun musicFileWorksAfterRepeatedActivityRecreation() {
        verifyPickerAfterRecreation(Intent.ACTION_OPEN_DOCUMENT, FileSelector::openMusicFile)
    }

    @Test
    fun folderWorksAfterRepeatedActivityRecreation() {
        verifyPickerAfterRecreation(Intent.ACTION_OPEN_DOCUMENT_TREE, FileSelector::openFolder)
    }

    private fun verifyPickerAfterRecreation(
        action: String,
        openPicker: ((Uri) -> Unit) -> Unit,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val launchedIntent = AtomicReference<Intent>()
        var result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        // Exercise the real launcher without opening a provider or modifying user files.
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action != action) return null
                launchedIntent.set(intent)
                return result
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                repeat(4) { iteration ->
                    if (iteration > 0) scenario.recreate()

                    val selected = AtomicReference<Uri>()
                    val delivered = CountDownLatch(1)
                    fun launch() {
                        launchedIntent.set(null)
                        scenario.onActivity {
                            openPicker {
                                selected.set(it)
                                delivered.countDown()
                            }
                        }
                        assertEquals(action, launchedIntent.get()?.action)
                        if (action == Intent.ACTION_OPEN_DOCUMENT) {
                            assertArrayEquals(
                                arrayOf("audio/*"),
                                launchedIntent.get().getStringArrayExtra(Intent.EXTRA_MIME_TYPES),
                            )
                        }
                    }

                    result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
                    launch()
                    instrumentation.waitForIdleSync()
                    assertEquals("Cancellation must not invoke the callback", 1L, delivered.count)

                    val uri = Uri.parse("content://com.tinyfish.jeekalarm.test/selection/$iteration")
                    result = Instrumentation.ActivityResult(
                        Activity.RESULT_OK, Intent().setData(uri)
                    )
                    launch()
                    assertTrue("Selection callback was not delivered", delivered.await(5, TimeUnit.SECONDS))
                    assertEquals(uri, selected.get())
                }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
