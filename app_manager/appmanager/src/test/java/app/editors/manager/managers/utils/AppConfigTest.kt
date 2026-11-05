package app.editors.manager.managers.utils

import android.content.Context
import android.content.res.AssetManager
import app.editors.manager.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException

class AppConfigTest {

    // A context whose assets have sdk.version with [version], or none when it is null
    private fun context(version: String?): Context {
        val assets = mockk<AssetManager>()
        if (version == null) {
            every { assets.open("sdk.version") } throws FileNotFoundException()
        } else {
            every { assets.open("sdk.version") } answers { version.byteInputStream() }
        }
        return mockk { every { this@mockk.assets } returns assets }
    }

    @Test
    fun `co-edits with a portal of the same major and minor version`() {
        assertTrue(AppConfig.isCoauthoring(context("9.3.7"), "9.3.1"))
    }

    @Test
    fun `does not co-edit with another minor or major version`() {
        assertFalse(AppConfig.isCoauthoring(context("9.3.7"), "9.2.7"))
        assertFalse(AppConfig.isCoauthoring(context("9.3.7"), "8.3.7"))
    }

    @Test
    fun `does not co-edit without both versions`() {
        assertFalse(AppConfig.isCoauthoring(context(null), "9.3.1"))
        assertFalse(AppConfig.isCoauthoring(context("9.3.7"), "9"))
        assertFalse(AppConfig.isCoauthoring(context("9.3.7"), ""))
    }

    @Test
    fun `services are the terms and the privacy policy`() {
        val context = mockk<Context> {
            every { getString(R.string.app_url_terms) } returns "terms"
            every { getString(R.string.app_url_policy) } returns "policy"
        }
        assertArrayEquals(arrayOf("terms", "policy"), AppConfig.servicesUrls(context))
    }
}
