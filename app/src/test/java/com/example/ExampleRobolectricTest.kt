package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.ui.common.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StreamFetch", appName)
  }

  @Test
  fun `format bytes formatting test`() {
    assertEquals("0 B", FormatUtils.formatBytes(0))
    assertEquals("500 B", FormatUtils.formatBytes(500))
    assertEquals("500 KB", FormatUtils.formatBytes(500 * 1024))
    assertEquals("1.0 MB", FormatUtils.formatBytes(1024 * 1024))
    assertEquals("15.0 MB", FormatUtils.formatBytes(15 * 1024 * 1024))
    assertEquals("1.50 GB", FormatUtils.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
  }

  @Test
  fun `download task progress calculation`() {
    val task = DownloadTask(
      url = "https://example.com/video.mp4",
      title = "Test Video",
      filePath = "/tmp/video.mp4",
      totalBytes = 1000L,
      downloadedBytes = 500L,
      status = DownloadStatus.DOWNLOADING
    )
    assertEquals(0.5f, task.progressFraction, 0.001f)
    assertEquals(50, task.progressPercent)
  }

  @Test
  fun `format duration test`() {
    assertEquals("01:15", FormatUtils.formatDuration(75_000))
    assertEquals("1:00:00", FormatUtils.formatDuration(3600_000))
  }
}
