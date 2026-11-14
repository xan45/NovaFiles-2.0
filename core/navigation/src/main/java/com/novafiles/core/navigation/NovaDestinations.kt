package com.novafiles.core.navigation

import kotlinx.serialization.Serializable

sealed interface NovaRoute {
    @Serializable
    data object Home : NovaRoute

    @Serializable
    data class Files(val initialPath: String? = null) : NovaRoute

    @Serializable
    data object DualPane : NovaRoute

    @Serializable
    data object Analyzer : NovaRoute

    @Serializable
    data object Cleaner : NovaRoute

    @Serializable
    data object Settings : NovaRoute

    @Serializable
    data class Music(val initialPath: String? = null) : NovaRoute

    @Serializable
    data class Video(val filePath: String) : NovaRoute

    @Serializable
    data class Gallery(val album: String? = null) : NovaRoute

    @Serializable
    data class MediaViewer(val filePath: String, val mimeType: String? = null) : NovaRoute
}
