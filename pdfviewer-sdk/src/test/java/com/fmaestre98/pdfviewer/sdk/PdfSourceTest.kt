package com.fmaestre98.pdfviewer.sdk

import com.fmaestre98.pdfviewer.sdk.model.PdfSource
import com.fmaestre98.pdfviewer.sdk.model.PdfViewerConfig
import com.fmaestre98.pdfviewer.sdk.core.model.PdfViewerOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class PdfSourceTest {

    @Test
    fun fromFile_usesAbsolutePathAsDocumentId() {
        val file = File("/tmp/sample.pdf")
        val source = PdfSource.FromFile(file)
        assertEquals(file.absolutePath, source.getDocumentId())
    }

    @Test
    fun fromAsset_usesAssetSchemeAsDocumentId() {
        val source = PdfSource.FromAsset("docs/manual.pdf")
        assertEquals("asset://docs/manual.pdf", source.getDocumentId())
    }

    @Test
    fun defaultConfig_matchesMainParityDefaults() {
        val config = PdfViewerConfig()
        assertEquals(PdfViewerOrientation.Horizontal, config.initialOrientation)
        assertNull(config.initialPage)
        assertEquals(true, config.enableAnnotations)
        assertEquals(true, config.enableBookmarks)
        assertEquals(true, config.enableSearch)
        assertEquals(true, config.enableThumbnailDrawer)
    }
}
