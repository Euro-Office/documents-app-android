package lib.toolkit.base.managers.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `splits a path at the last dot`() {
        assertEquals(".docx", StringUtils.getExtensionFromPath("dir.old/report.v2.docx"))
        assertEquals("dir.old/report.v2", StringUtils.getNameWithoutExtension("dir.old/report.v2.docx"))
    }

    @Test
    fun `a path without a dot has no extension`() {
        assertEquals("", StringUtils.getExtensionFromPath("report"))
        assertEquals("report", StringUtils.getNameWithoutExtension("report"))
    }

    @Test
    fun `keeps a name up to its first forbidden symbol`() {
        assertEquals("bad", StringUtils.getAllowedString("bad:name?"))
        assertNull(StringUtils.getAllowedString("good name"))
    }

    @Test
    fun `takes the avatar's letters from the first two names, else the first name`() {
        assertEquals("AL", StringUtils.getAvatarName("Ada Lovelace King"))
        assertEquals("AD", StringUtils.getAvatarName("ada"))
        assertEquals("A", StringUtils.getAvatarName("a"))
    }

    @Test
    fun `an empty name has the default avatar`() {
        assertEquals("U", StringUtils.getAvatarName(""))
    }

    @Test
    fun `writes the md5 in lower case, with nothing hashed for null`() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", StringUtils.getMd5("abc"))
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", StringUtils.getMd5(null))
    }
}
