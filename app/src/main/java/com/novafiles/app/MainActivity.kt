package com.novafiles.app

import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novafiles.core.designsystem.theme.NovaTheme
import com.novafiles.core.model.AccentColor
import com.novafiles.core.model.FileItem
import com.novafiles.core.model.FileType
import com.novafiles.core.model.ThemeConfig
import com.novafiles.core.model.ThemeMode
import com.novafiles.core.navigation.NovaScaffold
import com.novafiles.core.storage.FileSystemDataSource
import com.novafiles.core.storage.StorageVolumeManager
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaCategoryTile
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaFolderCard
import com.novafiles.core.ui.components.NovaNavigationBar
import com.novafiles.core.ui.components.NovaNavTab
import com.novafiles.core.ui.components.NovaStorageSegmentBar
import com.novafiles.core.ui.components.StorageSegment
import androidx.compose.foundation.clickable
import com.novafiles.core.media.MusicPlaybackManager
import com.novafiles.data.files.FileRepositoryImpl
import com.novafiles.feature.analyzer.StorageAnalyzerScreen
import com.novafiles.feature.cleaner.StorageCleanerScreen
import com.novafiles.feature.dualpane.DualPaneScreen
import com.novafiles.feature.dualpane.DualPaneViewModel
import com.novafiles.feature.files.FilesScreen
import com.novafiles.feature.files.FilesViewModel
import com.novafiles.feature.gallery.GalleryScreen
import com.novafiles.feature.music.MusicPlayerScreen
import com.novafiles.feature.music.MusicPlayerViewModel
import com.novafiles.feature.music.NovaMiniMusicPlayer
import com.novafiles.feature.vault.VaultScreen
import com.novafiles.feature.video.VideoPlayerScreen
import java.io.File

sealed interface ActiveScreen {
    data class Video(val filePath: String) : ActiveScreen
    data object Music : ActiveScreen
    data class Gallery(val album: String? = null) : ActiveScreen
    data object StorageAnalyzer : ActiveScreen
    data object StorageCleaner : ActiveScreen
}

class MainActivity : ComponentActivity() {

    private val fileRepository by lazy {
        FileRepositoryImpl(
            fileSystemDataSource = FileSystemDataSource(),
            storageVolumeManager = StorageVolumeManager(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeConfig by remember {
                mutableStateOf(ThemeConfig(mode = ThemeMode.DARK, accent = AccentColor.INDIGO))
            }
            val filesViewModel = remember { FilesViewModel(fileRepository) }
            val dualPaneViewModel = remember { DualPaneViewModel(fileRepository) }
            val musicViewModel = remember { MusicPlayerViewModel(fileRepository) }

            NovaTheme(config = themeConfig) {
                NovaFilesMainApp(
                    filesViewModel = filesViewModel,
                    dualPaneViewModel = dualPaneViewModel,
                    musicViewModel = musicViewModel,
                    themeConfig = themeConfig,
                    onThemeConfigChange = { themeConfig = it }
                )
            }
        }
    }
}

@Composable
fun NovaFilesMainApp(
    filesViewModel: FilesViewModel,
    dualPaneViewModel: DualPaneViewModel,
    musicViewModel: MusicPlayerViewModel,
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NovaNavTab.HOME) }
    var activeScreen by remember { mutableStateOf<ActiveScreen?>(null) }
    val filesUiState by filesViewModel.uiState.collectAsState()
    val dualPaneUiState by dualPaneViewModel.uiState.collectAsState()
    val playbackState by MusicPlaybackManager.state.collectAsState()

    if (activeScreen != null) {
        when (val screen = activeScreen) {
            is ActiveScreen.Video -> {
                VideoPlayerScreen(
                    videoFilePath = screen.filePath,
                    onNavigateBack = { activeScreen = null }
                )
            }
            is ActiveScreen.Music -> {
                MusicPlayerScreen(
                    viewModel = musicViewModel,
                    onNavigateBack = { activeScreen = null }
                )
            }
            is ActiveScreen.Gallery -> {
                GalleryScreen(
                    onNavigateBack = { activeScreen = null }
                )
            }
            is ActiveScreen.StorageAnalyzer -> {
                StorageAnalyzerScreen(
                    onNavigateBack = { activeScreen = null },
                    onOpenFile = { fileItem ->
                        when (fileItem.fileType) {
                            FileType.AUDIO -> {
                                musicViewModel.playTrack(fileItem)
                                activeScreen = ActiveScreen.Music
                            }
                            FileType.VIDEO -> {
                                activeScreen = ActiveScreen.Video(fileItem.path)
                            }
                            FileType.IMAGE -> {
                                activeScreen = ActiveScreen.Gallery()
                            }
                            else -> {}
                        }
                    }
                )
            }
            is ActiveScreen.StorageCleaner -> {
                StorageCleanerScreen(
                    onNavigateBack = { activeScreen = null }
                )
            }
            null -> {}
        }
        return
    }

    NovaScaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                if (playbackState.currentTrack != null) {
                    NovaMiniMusicPlayer(
                        musicState = playbackState,
                        onPlayPause = { MusicPlaybackManager.togglePlayPause() },
                        onNext = { MusicPlaybackManager.skipToNext() },
                        onClick = { activeScreen = ActiveScreen.Music }
                    )
                }
                NovaNavigationBar(
                    currentTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { contentModifier ->
        when (selectedTab) {
            NovaNavTab.HOME -> {
                NovaHomeDashboard(
                    themeConfig = themeConfig,
                    onThemeConfigChange = onThemeConfigChange,
                    onStorageOverviewClick = { activeScreen = ActiveScreen.StorageAnalyzer },
                    onCleanUpClick = { activeScreen = ActiveScreen.StorageCleaner },
                    onCategoryClick = { categoryName ->
                        when (categoryName) {
                            "Photos" -> {
                                activeScreen = ActiveScreen.Gallery()
                            }
                            "Audio" -> {
                                activeScreen = ActiveScreen.Music
                            }
                            "Videos" -> {
                                val targetDir = File(Environment.getExternalStorageDirectory(), "Movies").absolutePath
                                filesViewModel.loadDirectory(targetDir)
                                selectedTab = NovaNavTab.FILES
                            }
                            "Documents" -> {
                                val targetDir = File(Environment.getExternalStorageDirectory(), "Documents").absolutePath
                                filesViewModel.loadDirectory(targetDir)
                                selectedTab = NovaNavTab.FILES
                            }
                            else -> {
                                filesViewModel.loadDirectory(Environment.getExternalStorageDirectory().absolutePath)
                                selectedTab = NovaNavTab.FILES
                            }
                        }
                    },
                    modifier = contentModifier
                )
            }
            NovaNavTab.FILES -> {
                FilesScreen(
                    uiState = filesUiState,
                    onFolderClick = { folderPath -> filesViewModel.navigateToFolder(folderPath) },
                    onFileClick = { fileItem ->
                        when (fileItem.fileType) {
                            FileType.AUDIO -> {
                                musicViewModel.playTrack(fileItem)
                                activeScreen = ActiveScreen.Music
                            }
                            FileType.VIDEO -> {
                                activeScreen = ActiveScreen.Video(fileItem.path)
                            }
                            FileType.IMAGE -> {
                                activeScreen = ActiveScreen.Gallery()
                            }
                            else -> {}
                        }
                    },
                    onNavigateBack = { filesViewModel.navigateUp() },
                    onToggleSelect = { id -> filesViewModel.toggleSelection(id) },
                    onSelectAll = { filesViewModel.selectAll() },
                    onClearSelection = { filesViewModel.clearSelection() },
                    onSortChange = { sortBy -> filesViewModel.setSortBy(sortBy) },
                    onToggleLayout = { filesViewModel.toggleLayoutMode() },
                    onRetry = { filesViewModel.loadDirectory(filesUiState.currentPath) },
                    modifier = contentModifier
                )
            }
            NovaNavTab.DUAL -> {
                DualPaneScreen(
                    uiState = dualPaneUiState,
                    onSetActivePane = { dualPaneViewModel.setActivePane(it) },
                    onFolderClickA = { dualPaneViewModel.loadPaneA(it) },
                    onFolderClickB = { dualPaneViewModel.loadPaneB(it) },
                    onToggleSelectA = { dualPaneViewModel.toggleSelectionA(it) },
                    onToggleSelectB = { dualPaneViewModel.toggleSelectionB(it) },
                    onCopyAcross = { dualPaneViewModel.copySelectedToOpposingPane() },
                    onMoveAcross = { dualPaneViewModel.moveSelectedToOpposingPane() },
                    onSyncPanes = { dualPaneViewModel.syncPanes() },
                    onSwapPanes = { dualPaneViewModel.swapPanes() },
                    modifier = contentModifier
                )
            }
            NovaNavTab.VAULT -> {
                VaultScreen(
                    onNavigateBack = { selectedTab = NovaNavTab.HOME },
                    modifier = contentModifier
                )
            }
            NovaNavTab.SETTINGS -> {
                /* Handled in Phase 6: Settings */
                NovaHomeDashboard(
                    themeConfig = themeConfig,
                    onThemeConfigChange = onThemeConfigChange,
                    onStorageOverviewClick = { activeScreen = ActiveScreen.StorageAnalyzer },
                    onCleanUpClick = { activeScreen = ActiveScreen.StorageCleaner },
                    onCategoryClick = {},
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
fun NovaHomeDashboard(
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onStorageOverviewClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Header / Greeting
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good Morning,",
                        style = NovaTheme.typography.captionMedium,
                        color = NovaTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Alexedra",
                        style = NovaTheme.typography.title,
                        color = NovaTheme.colors.textPrimary
                    )
                }

                Row {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        // Interactive Theme Selector (for Phase 1 testing across all 18 combinations)
        item {
            NovaThemeSelectorCard(
                themeConfig = themeConfig,
                onThemeConfigChange = onThemeConfigChange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Storage Overview Card (Reference: media_1794034804827_b8bf4514.jpg)
        item {
            NovaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onStorageOverviewClick),
                shape = NovaTheme.shapes.extraLarge,
                backgroundColor = NovaTheme.colors.surfaceElevated
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Device Storage",
                                style = NovaTheme.typography.subtitle,
                                color = NovaTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "69.8 GB / 512 GB",
                                style = NovaTheme.typography.captionMedium,
                                color = NovaTheme.colors.textSecondary
                            )
                        }

                        NovaButton(
                            text = "Clean up",
                            onClick = onCleanUpClick,
                            variant = NovaButtonVariant.GRADIENT,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented bar
                    NovaStorageSegmentBar(
                        segments = listOf(
                            StorageSegment(NovaTheme.colors.categoryPhotos, 20.8f),
                            StorageSegment(NovaTheme.colors.categoryVideos, 29.0f),
                            StorageSegment(NovaTheme.colors.categoryDocuments, 8.0f),
                            StorageSegment(NovaTheme.colors.categoryAudio, 12.0f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Category Section (Reference: 2x2 Grid)
        item {
            Text(
                text = "Category",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                NovaCategoryTile(
                    title = "Photos",
                    sizeText = "20.8 GB",
                    icon = Icons.Default.Image,
                    iconTint = NovaTheme.colors.categoryPhotos,
                    iconBackground = NovaTheme.colors.categoryPhotos.copy(alpha = 0.2f),
                    onClick = { onCategoryClick("Photos") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                NovaCategoryTile(
                    title = "Videos",
                    sizeText = "29 GB",
                    icon = Icons.Default.Movie,
                    iconTint = NovaTheme.colors.categoryVideos,
                    iconBackground = NovaTheme.colors.categoryVideos.copy(alpha = 0.2f),
                    onClick = { onCategoryClick("Videos") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                NovaCategoryTile(
                    title = "Documents",
                    sizeText = "8 GB",
                    icon = Icons.Default.Description,
                    iconTint = NovaTheme.colors.categoryDocuments,
                    iconBackground = NovaTheme.colors.categoryDocuments.copy(alpha = 0.2f),
                    onClick = { onCategoryClick("Documents") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                NovaCategoryTile(
                    title = "Audio",
                    sizeText = "12 GB",
                    icon = Icons.Default.MusicNote,
                    iconTint = NovaTheme.colors.categoryAudio,
                    iconBackground = NovaTheme.colors.categoryAudio.copy(alpha = 0.2f),
                    onClick = { onCategoryClick("Audio") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Folders Horizontal List (Reference screen 2)
        item {
            Text(
                text = "Folders",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovaFolderCard(
                    name = "Office Trip",
                    itemCount = 42,
                    dateText = "12/01/23",
                    cardColor = NovaTheme.colors.categoryVideos.copy(alpha = 0.15f),
                    folderIconColor = NovaTheme.colors.categoryVideos,
                    isSelected = true,
                    onClick = {}
                )
                NovaFolderCard(
                    name = "Downloads",
                    itemCount = 15,
                    dateText = "06/01/23",
                    cardColor = NovaTheme.colors.categoryApks.copy(alpha = 0.15f),
                    folderIconColor = NovaTheme.colors.categoryApks,
                    isSelected = false,
                    onClick = {}
                )
                NovaFolderCard(
                    name = "Music Library",
                    itemCount = 249,
                    dateText = "18/08/26",
                    cardColor = NovaTheme.colors.categoryAudio.copy(alpha = 0.15f),
                    folderIconColor = NovaTheme.colors.categoryAudio,
                    isSelected = false,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Files Section
        item {
            Text(
                text = "Recent",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val recentSampleFiles = listOf(
            FileItem(
                id = "1",
                name = "Design_guidelines.pdf",
                path = "/storage/emulated/0/Documents/Design_guidelines.pdf",
                sizeBytes = 2_900_000L,
                lastModifiedMs = 1679702400000L,
                fileType = FileType.DOCUMENT
            ),
            FileItem(
                id = "2",
                name = "IMG_20200408_125418.jpg",
                path = "/storage/emulated/0/DCIM/Camera/IMG_20200408_125418.jpg",
                sizeBytes = 2_900_000L,
                lastModifiedMs = 1679702400000L,
                fileType = FileType.IMAGE
            ),
            FileItem(
                id = "3",
                name = "Good Luck, Babe!.mp3",
                path = "/storage/emulated/0/Music/Good Luck, Babe!.mp3",
                sizeBytes = 8_400_000L,
                lastModifiedMs = 1724000000000L,
                fileType = FileType.AUDIO
            )
        )

        items(recentSampleFiles.size) { index ->
            val file = recentSampleFiles[index]
            NovaFileRow(
                item = file,
                formattedSize = "2.9 MB",
                formattedDate = "25/03/23",
                onClick = {},
                onMenuClick = {}
            )
        }
    }
}

@Composable
fun NovaThemeSelectorCard(
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    NovaCard(
        modifier = modifier.fillMaxWidth(),
        shape = NovaTheme.shapes.large,
        backgroundColor = NovaTheme.colors.surface
    ) {
        Column {
            Text(
                text = "Theme & Customization (18 Variants)",
                style = NovaTheme.typography.subtitle,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Base mode selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = themeConfig.mode == mode
                    NovaButton(
                        text = mode.name,
                        onClick = { onThemeConfigChange(themeConfig.copy(mode = mode)) },
                        variant = if (isSelected) NovaButtonVariant.PRIMARY else NovaButtonVariant.SECONDARY,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Accent color selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccentColor.entries.forEach { accent ->
                    val isSelected = themeConfig.accent == accent
                    NovaButton(
                        text = accent.name,
                        onClick = { onThemeConfigChange(themeConfig.copy(accent = accent)) },
                        variant = if (isSelected) NovaButtonVariant.PRIMARY else NovaButtonVariant.SECONDARY,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
