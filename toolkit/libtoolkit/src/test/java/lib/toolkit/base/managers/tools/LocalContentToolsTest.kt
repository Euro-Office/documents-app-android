package lib.toolkit.base.managers.tools

import android.content.Context
import android.os.Environment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import lib.toolkit.base.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class LocalContentToolsTest {

    private val storage = File("/storage/emulated/0")

    private fun context(title: String): Context = mockk {
        every { getString(R.string.app_title) } returns title
        every { filesDir } returns File("/data/user/0/app/files")
    }

    @Before
    fun mockStorage() {
        mockkStatic(Environment::class)
        every { Environment.getExternalStorageDirectory() } returns storage
    }

    @After
    fun unmockStorage() {
        unmockkStatic(Environment::class)
    }

    @Test
    fun `names the folder in the shared storage after the brand`() {
        assertEquals(File(storage, "Euro-Office"), LocalContentTools.getExternalDir(context("Euro-Office")))
        assertEquals(File(storage, "Nextcloud Office"), LocalContentTools.getExternalDir(context("Nextcloud Office")))
    }

    @Test
    fun `keeps the documents in the shared storage folder before Android 11`() {
        // Build.VERSION.SDK_INT is 0 in unit tests
        assertEquals("/storage/emulated/0/Nextcloud Office", LocalContentTools.getDir(context("Nextcloud Office")))
    }
}
