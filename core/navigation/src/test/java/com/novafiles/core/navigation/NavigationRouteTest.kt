package com.novafiles.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationRouteTest {

    @Test
    fun testRouteDeclarations() {
        val home = NovaRoute.Home
        assertNotNull(home)

        val files = NovaRoute.Files(initialPath = "/storage/emulated/0/Download")
        assertEquals("/storage/emulated/0/Download", files.initialPath)

        val video = NovaRoute.Video(filePath = "/storage/emulated/0/Movies/sample.mp4")
        assertEquals("/storage/emulated/0/Movies/sample.mp4", video.filePath)

        val gallery = NovaRoute.Gallery(album = "Camera")
        assertEquals("Camera", gallery.album)
    }
}
