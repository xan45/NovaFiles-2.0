package com.novafiles.app

import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.novafiles.core.common.FileFormatters
import com.novafiles.core.storage.StoragePermissionHelper
import com.novafiles.core.model.StorageStats
import com.novafiles.feature.settings.SettingsPreferencesRepository
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.novafiles.core.ui.branding.AppLogo
import com.novafiles.core.ui.branding.NovaSplashScreen
import com.novafiles.core.ui.components.NovaButton
import com.novafiles.core.ui.components.NovaButtonVariant
import com.novafiles.core.ui.components.NovaCard
import com.novafiles.core.ui.components.NovaCategoryTile
import com.novafiles.core.ui.icons.NovaIcons
import com.novafiles.core.ui.components.NovaFileRow
import com.novafiles.core.ui.components.NovaFolderCard
import com.novafiles.core.ui.components.NovaNavigationBar
import com.novafiles.core.ui.components.NovaNavTab
import com.novafiles.core.ui.components.NovaStorageRing
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
import com.novafiles.feature.files.PdfViewerScreen
import com.novafiles.feature.files.TextEditorScreen
import com.novafiles.feature.ftp.FtpServerScreen
import com.novafiles.feature.gallery.GalleryScreen
import com.novafiles.feature.music.MusicPlayerScreen
import com.novafiles.feature.music.MusicPlayerViewModel
import com.novafiles.feature.music.NovaMiniMusicPlayer
import com.novafiles.feature.settings.AboutScreen
import com.novafiles.feature.settings.SettingsScreen
import com.novafiles.feature.vault.VaultScreen
import com.novafiles.feature.video.VideoPlayerScreen
import java.io.File

sealed interface ActiveScreen : java.io.Serializable {
    data class Video(val filePath: String) : ActiveScreen
    data object Music : ActiveScreen
    data class Gallery(val album: String? = null) : ActiveScreen
    data object StorageAnalyzer : ActiveScreen
    data object StorageCleaner : ActiveScreen
    data object FtpServer : ActiveScreen
    data class TextEditor(val filePath: String) : ActiveScreen
    data class PdfViewer(val filePath: String) : ActiveScreen
    data object About : ActiveScreen
}

private fun serializeActiveScreen(screen: ActiveScreen): List<String> = when (screen) {
    is ActiveScreen.Video -> listOf("Video", screen.filePath)
    is ActiveScreen.Music -> listOf("Music")
    is ActiveScreen.Gallery -> listOf("Gallery", screen.album ?: "")
    is ActiveScreen.StorageAnalyzer -> listOf("StorageAnalyzer")
    is ActiveScreen.StorageCleaner -> listOf("StorageCleaner")
    is ActiveScreen.FtpServer -> listOf("FtpServer")
    is ActiveScreen.TextEditor -> listOf("TextEditor", screen.filePath)
    is ActiveScreen.PdfViewer -> listOf("PdfViewer", screen.filePath)
    is ActiveScreen.About -> listOf("About")
}

private fun deserializeActiveScreen(list: List<*>): ActiveScreen? {
    val type = list.getOrNull(0) as? String ?: return null
    return when (type) {
        "Video" -> ActiveScreen.Video(list.getOrNull(1) as? String ?: "")
        "Music" -> ActiveScreen.Music
        "Gallery" -> ActiveScreen.Gallery((list.getOrNull(1) as? String)?.takeIf { it.isNotEmpty() })
        "StorageAnalyzer" -> ActiveScreen.StorageAnalyzer
        "StorageCleaner" -> ActiveScreen.StorageCleaner
        "FtpServer" -> ActiveScreen.FtpServer
        "TextEditor" -> ActiveScreen.TextEditor(list.getOrNull(1) as? String ?: "")
        "PdfViewer" -> ActiveScreen.PdfViewer(list.getOrNull(1) as? String ?: "")
        "About" -> ActiveScreen.About
        else -> null
    }
}

val ActiveScreenStackSaver = Saver<List<ActiveScreen>, List<List<String>>>(
    save = { stack -> stack.map { serializeActiveScreen(it) } },
    restore = { savedList -> savedList.mapNotNull { deserializeActiveScreen(it) } }
)

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private val settingsRepo by lazy { SettingsPreferencesRepository(applicationContext) }

    private val fileRepository by lazy {
        FileRepositoryImpl(
            fileSystemDataSource = FileSystemDataSource(),
            storageVolumeManager = StorageVolumeManager(applicationContext)
        )
    }

    private val filesViewModel: FilesViewModel by viewModels {
        viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                FilesViewModel(
                    fileRepository = fileRepository,
                    context = applicationContext,
                    savedStateHandle = handle
                )
            }
        }
    }

    private val dualPaneViewModel: DualPaneViewModel by viewModels {
        viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                DualPaneViewModel(fileRepository, handle)
            }
        }
    }

    private val musicViewModel: MusicPlayerViewModel by viewModels {
        viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                MusicPlayerViewModel(
                    fileRepository = fileRepository,
                    context = applicationContext,
                    savedStateHandle = handle
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MusicPlaybackManager.initialize(applicationContext)

        setContent {
            val appSettings by settingsRepo.settings.collectAsState()
            val themeConfig = appSettings.themeConfig
            var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }

            NovaTheme(config = themeConfig) {
                if (showSplash) {
                    NovaSplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    NovaFilesMainApp(
                        filesViewModel = filesViewModel,
                        dualPaneViewModel = dualPaneViewModel,
                        musicViewModel = musicViewModel,
                        themeConfig = themeConfig,
                        onThemeConfigChange = { settingsRepo.updateThemeConfig(it) },
                        fileRepository = fileRepository
                    )
                }
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
    fileRepository: FileRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by rememberSaveable {
        mutableStateOf(StoragePermissionHelper.hasStoragePermission(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermissionHelper.hasStoragePermission(context)
                if (granted != hasPermission) {
                    hasPermission = granted
                    if (granted) {
                        filesViewModel.loadDirectory(filesViewModel.uiState.value.currentPath)
                        musicViewModel.loadMusicLibrary(forceRefresh = true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaTheme.colors.background)
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppLogo(iconSize = 80.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Storage Access Required",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NovaFiles requires All Files Access permission to browse, organize, encrypt, and manage storage on your device.",
                    style = NovaTheme.typography.body,
                    color = NovaTheme.colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                NovaButton(
                    text = "Grant Storage Access",
                    onClick = {
                        val intent = StoragePermissionHelper.createManageStorageIntent(context)
                        context.startActivity(intent)
                    },
                    variant = NovaButtonVariant.PRIMARY
                )
            }
        }
        return
    }

    var selectedTab by rememberSaveable { mutableStateOf(NovaNavTab.HOME) }
    var activeScreenStack by rememberSaveable(stateSaver = ActiveScreenStackSaver) {
        mutableStateOf<List<ActiveScreen>>(emptyList())
    }

    fun navigateTo(screen: ActiveScreen) {
        if (activeScreenStack.lastOrNull() != screen) {
            activeScreenStack = activeScreenStack + screen
        }
    }

    fun navigateBack() {
        if (activeScreenStack.isNotEmpty()) {
            activeScreenStack = activeScreenStack.dropLast(1)
        }
    }

    val filesUiState by filesViewModel.uiState.collectAsState()
    val dualPaneUiState by dualPaneViewModel.uiState.collectAsState()
    val playbackState by MusicPlaybackManager.state.collectAsState()

    val currentScreen = activeScreenStack.lastOrNull()
    if (currentScreen != null) {
        when (currentScreen) {
            is ActiveScreen.Video -> {
                VideoPlayerScreen(
                    videoFilePath = currentScreen.filePath,
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.Music -> {
                MusicPlayerScreen(
                    viewModel = musicViewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.Gallery -> {
                GalleryScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.StorageAnalyzer -> {
                StorageAnalyzerScreen(
                    onNavigateBack = { navigateBack() },
                    onNavigateToCleaner = { navigateTo(ActiveScreen.StorageCleaner) },
                    onOpenFile = { fileItem ->
                        when (fileItem.fileType) {
                            FileType.AUDIO -> {
                                musicViewModel.playTrack(fileItem)
                                navigateTo(ActiveScreen.Music)
                            }
                            FileType.VIDEO -> {
                                navigateTo(ActiveScreen.Video(fileItem.path))
                            }
                            FileType.IMAGE -> {
                                navigateTo(ActiveScreen.Gallery())
                            }
                            FileType.DOCUMENT -> {
                                if (fileItem.name.endsWith(".pdf", ignoreCase = true)) {
                                    navigateTo(ActiveScreen.PdfViewer(fileItem.path))
                                } else {
                                    navigateTo(ActiveScreen.TextEditor(fileItem.path))
                                }
                            }
                            else -> navigateTo(ActiveScreen.TextEditor(fileItem.path))
                        }
                    }
                )
            }
            is ActiveScreen.StorageCleaner -> {
                StorageCleanerScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.FtpServer -> {
                FtpServerScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.TextEditor -> {
                TextEditorScreen(
                    filePath = currentScreen.filePath,
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.PdfViewer -> {
                PdfViewerScreen(
                    filePath = currentScreen.filePath,
                    onNavigateBack = { navigateBack() }
                )
            }
            is ActiveScreen.About -> {
                AboutScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
        }
        return
    }

    // System back navigation at bottom navigation tab level.
    // When no active overlay/nested screen is open and the user is on a non-HOME tab,
    // back navigation returns to the HOME dashboard.
    // When on HOME dashboard, this BackHandler is disabled, allowing system back to minimize/exit the app.
    BackHandler(enabled = activeScreenStack.isEmpty() && selectedTab != NovaNavTab.HOME) {
        selectedTab = NovaNavTab.HOME
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
                        onClick = {
                            musicViewModel.expandNowPlaying()
                            navigateTo(ActiveScreen.Music)
                        }
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
                val storageStats by fileRepository.getStorageStats().collectAsState(
                    initial = StorageStats("Internal Storage", 0L, 0L)
                )
                NovaHomeDashboard(
                    storageStats = storageStats,
                    folders = filesUiState.folders,
                    recentFiles = filesUiState.files,
                    onFolderClick = { folderPath ->
                        filesViewModel.loadDirectory(folderPath)
                        selectedTab = NovaNavTab.FILES
                    },
                    onFileClick = { fileItem ->
                        when (fileItem.fileType) {
                            FileType.AUDIO -> {
                                musicViewModel.playTrack(fileItem)
                                navigateTo(ActiveScreen.Music)
                            }
                            FileType.VIDEO -> navigateTo(ActiveScreen.Video(fileItem.path))
                            FileType.IMAGE -> navigateTo(ActiveScreen.Gallery())
                            FileType.DOCUMENT -> {
                                if (fileItem.name.endsWith(".pdf", ignoreCase = true)) {
                                    navigateTo(ActiveScreen.PdfViewer(fileItem.path))
                                } else {
                                    navigateTo(ActiveScreen.TextEditor(fileItem.path))
                                }
                            }
                            else -> navigateTo(ActiveScreen.TextEditor(fileItem.path))
                        }
                    },
                    onStorageOverviewClick = { navigateTo(ActiveScreen.StorageAnalyzer) },
                    onCleanUpClick = { navigateTo(ActiveScreen.StorageCleaner) },
                    onSettingsClick = { selectedTab = NovaNavTab.SETTINGS },
                    onFtpClick = { navigateTo(ActiveScreen.FtpServer) },
                    onAboutClick = { navigateTo(ActiveScreen.About) },
                    onCategoryClick = { categoryName ->
                        when (categoryName) {
                            "Photos" -> navigateTo(ActiveScreen.Gallery())
                            "Audio" -> {
                                musicViewModel.collapseNowPlaying()
                                musicViewModel.loadMusicLibrary()
                                navigateTo(ActiveScreen.Music)
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
                                navigateTo(ActiveScreen.Music)
                            }
                            FileType.VIDEO -> navigateTo(ActiveScreen.Video(fileItem.path))
                            FileType.IMAGE -> navigateTo(ActiveScreen.Gallery())
                            FileType.DOCUMENT -> {
                                if (fileItem.name.endsWith(".pdf", ignoreCase = true)) {
                                    navigateTo(ActiveScreen.PdfViewer(fileItem.path))
                                } else {
                                    navigateTo(ActiveScreen.TextEditor(fileItem.path))
                                }
                            }
                            else -> navigateTo(ActiveScreen.TextEditor(fileItem.path))
                        }
                    },
                    onNavigateBack = {
                        if (filesViewModel.canNavigateUp()) {
                            filesViewModel.navigateUp()
                        } else {
                            selectedTab = NovaNavTab.HOME
                        }
                    },
                    onToggleSelect = { id -> filesViewModel.toggleSelection(id) },
                    onSelectAll = { filesViewModel.selectAll() },
                    onClearSelection = { filesViewModel.clearSelection() },
                    onSortChange = { sortBy -> filesViewModel.setSortBy(sortBy) },
                    onToggleLayout = { filesViewModel.toggleLayoutMode() },
                    onRetry = { filesViewModel.loadDirectory(filesUiState.currentPath) },
                    onCopy = { filesViewModel.copySelected() },
                    onMove = { filesViewModel.moveSelected() },
                    onPaste = { filesViewModel.pasteClipboard() },
                    onDelete = { filesViewModel.deleteSelectedFiles() },
                    onCreateFolder = { name -> filesViewModel.createNewFolder(name) },
                    onRenameFile = { item, newName -> filesViewModel.renameFile(item, newName) },
                    onSearchQueryChange = { query -> filesViewModel.setSearchQuery(query) },
                    onClearClipboard = { filesViewModel.clearClipboard() },
                    onFilterChange = { filter -> filesViewModel.setFileFilter(filter) },
                    onToggleSortDirection = { filesViewModel.toggleSortDirection() },
                    onCompressSelected = { zipName -> filesViewModel.compressSelectedToZip(zipName) },
                    onExtractArchive = { item -> filesViewModel.extractArchiveFile(item) },
                    onExtractArchiveEntry = { item, entry -> filesViewModel.extractSingleArchiveEntry(item, entry) },
                    onBatchRename = { pre, suf, find, rep, cnt -> filesViewModel.batchRenameSelected(pre, suf, find, rep, cnt) },
                    onMoveToTrash = { trashDir -> filesViewModel.moveToTrashSelected(trashDir) },
                    onToggleBookmark = { path, name -> filesViewModel.toggleBookmark(path, name) },
                    onComputeSha256 = { path -> filesViewModel.computeSha256(path) },
                    onToggleShowHidden = { filesViewModel.toggleShowHiddenFiles() },
                    onRefresh = { filesViewModel.refresh() },
                    onCopySingleFile = { filesViewModel.copyFile(it) },
                    onCutSingleFile = { filesViewModel.cutFile(it) },
                    onDeleteSingleFile = { filesViewModel.deleteSingleFile(it) },
                    onPasteIntoFolder = { filesViewModel.pasteIntoFolder(it) },
                    onCompressSingleFile = { item, name -> filesViewModel.compressSingleFile(item, name) },
                    onCancelOperation = { filesViewModel.cancelOperation() },
                    modifier = contentModifier
                )
            }
            NovaNavTab.DUAL -> {
                DualPaneScreen(
                    uiState = dualPaneUiState,
                    canNavigateUp = dualPaneViewModel.canNavigateUp(),
                    onNavigateBack = {
                        if (dualPaneViewModel.canNavigateUp()) {
                            dualPaneViewModel.navigateUp()
                        } else {
                            selectedTab = NovaNavTab.HOME
                        }
                    },
                    onSetActivePane = { dualPaneViewModel.setActivePane(it) },
                    onFolderClickA = { dualPaneViewModel.loadPaneA(it) },
                    onFolderClickB = { dualPaneViewModel.loadPaneB(it) },
                    onToggleSelectA = { dualPaneViewModel.toggleSelectionA(it) },
                    onToggleSelectB = { dualPaneViewModel.toggleSelectionB(it) },
                    onCopyAcross = { dualPaneViewModel.copySelectedToOpposingPane() },
                    onMoveAcross = { dualPaneViewModel.moveSelectedToOpposingPane() },
                    onSyncPanes = { dualPaneViewModel.syncPanes() },
                    onSwapPanes = { dualPaneViewModel.swapPanes() },
                    onFileClick = { fileItem ->
                        when (fileItem.fileType) {
                            FileType.AUDIO -> {
                                musicViewModel.playTrack(fileItem)
                                navigateTo(ActiveScreen.Music)
                            }
                            FileType.VIDEO -> navigateTo(ActiveScreen.Video(fileItem.path))
                            FileType.IMAGE -> navigateTo(ActiveScreen.Gallery())
                            FileType.DOCUMENT -> {
                                if (fileItem.name.endsWith(".pdf", ignoreCase = true)) {
                                    navigateTo(ActiveScreen.PdfViewer(fileItem.path))
                                } else {
                                    navigateTo(ActiveScreen.TextEditor(fileItem.path))
                                }
                            }
                            else -> navigateTo(ActiveScreen.TextEditor(fileItem.path))
                        }
                    },
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
                SettingsScreen(
                    onNavigateToFtp = { navigateTo(ActiveScreen.FtpServer) },
                    onNavigateToAnalyzer = { navigateTo(ActiveScreen.StorageAnalyzer) },
                    onNavigateToCleaner = { navigateTo(ActiveScreen.StorageCleaner) },
                    onNavigateToAbout = { navigateTo(ActiveScreen.About) },
                    onThemeConfigChange = onThemeConfigChange,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
fun NovaHomeDashboard(
    storageStats: StorageStats,
    folders: List<FileItem>,
    recentFiles: List<FileItem>,
    onFolderClick: (String) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onStorageOverviewClick: () -> Unit,
    onCleanUpClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onFtpClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Header / Official Application Branding
        item {
            var showMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogo(
                    iconSize = 44.dp,
                    showWordmark = true,
                    showBadge = false,
                    showTagline = true,
                    tagline = "Storage & File Management Suite"
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onStorageOverviewClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Storage Analyzer",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NovaTheme.colors.textPrimary
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = NovaTheme.colors.textPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Wi-Fi FTP Server") },
                                onClick = {
                                    showMenu = false
                                    onFtpClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Storage Analyzer") },
                                onClick = {
                                    showMenu = false
                                    onStorageOverviewClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    onSettingsClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About NovaFiles") },
                                onClick = {
                                    showMenu = false
                                    onAboutClick()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Enhanced Storage Overview Banner (Live Storage Data + Ring Gauge)
        item {
            val usedPercent = if (storageStats.totalBytes > 0L) {
                ((storageStats.usedBytes.toFloat() / storageStats.totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
            } else 0f
            val freeBytes = (storageStats.totalBytes - storageStats.usedBytes).coerceAtLeast(0L)

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            NovaStorageRing(
                                percentage = usedPercent / 100f,
                                size = 64.dp,
                                strokeWidth = 7.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Internal Storage",
                                    style = NovaTheme.typography.title,
                                    color = NovaTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (storageStats.totalBytes > 0L) {
                                        "${FileFormatters.formatBytes(storageStats.usedBytes)} used of ${FileFormatters.formatBytes(storageStats.totalBytes)}"
                                    } else {
                                        "Storage Access Active"
                                    },
                                    style = NovaTheme.typography.captionMedium,
                                    color = NovaTheme.colors.textSecondary
                                )
                                if (storageStats.totalBytes > 0L) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${FileFormatters.formatBytes(freeBytes)} free",
                                        style = NovaTheme.typography.caption,
                                        color = NovaTheme.colors.primary
                                    )
                                }
                            }
                        }

                        NovaButton(
                            text = "Clean up",
                            onClick = onCleanUpClick,
                            variant = NovaButtonVariant.GRADIENT,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NovaStorageSegmentBar(
                        segments = listOf(
                            StorageSegment(NovaTheme.colors.primary, usedPercent.coerceIn(4f, 96f)),
                            StorageSegment(NovaTheme.colors.cardBorder, (100f - usedPercent).coerceIn(4f, 96f))
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Category Section
        item {
            Text(
                text = "Category",
                style = NovaTheme.typography.title,
                color = NovaTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth > 600.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NovaCategoryTile(
                            title = "Photos",
                            sizeText = "Gallery",
                            icon = NovaIcons.Photos,
                            iconTint = NovaTheme.colors.categoryPhotos,
                            iconBackground = NovaTheme.colors.categoryPhotos.copy(alpha = 0.2f),
                            onClick = { onCategoryClick("Photos") },
                            modifier = Modifier.weight(1f)
                        )
                        NovaCategoryTile(
                            title = "Videos",
                            sizeText = "Cinema",
                            icon = NovaIcons.Videos,
                            iconTint = NovaTheme.colors.categoryVideos,
                            iconBackground = NovaTheme.colors.categoryVideos.copy(alpha = 0.2f),
                            onClick = { onCategoryClick("Videos") },
                            modifier = Modifier.weight(1f)
                        )
                        NovaCategoryTile(
                            title = "Documents",
                            sizeText = "PDF & Code",
                            icon = NovaIcons.Documents,
                            iconTint = NovaTheme.colors.categoryDocuments,
                            iconBackground = NovaTheme.colors.categoryDocuments.copy(alpha = 0.2f),
                            onClick = { onCategoryClick("Documents") },
                            modifier = Modifier.weight(1f)
                        )
                        NovaCategoryTile(
                            title = "Audio",
                            sizeText = "Studio",
                            icon = NovaIcons.AudioWave,
                            iconTint = NovaTheme.colors.categoryAudio,
                            iconBackground = NovaTheme.colors.categoryAudio.copy(alpha = 0.2f),
                            onClick = { onCategoryClick("Audio") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            NovaCategoryTile(
                                title = "Photos",
                                sizeText = "Gallery",
                                icon = NovaIcons.Photos,
                                iconTint = NovaTheme.colors.categoryPhotos,
                                iconBackground = NovaTheme.colors.categoryPhotos.copy(alpha = 0.2f),
                                onClick = { onCategoryClick("Photos") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            NovaCategoryTile(
                                title = "Videos",
                                sizeText = "Cinema",
                                icon = NovaIcons.Videos,
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
                                sizeText = "PDF & Code",
                                icon = NovaIcons.Documents,
                                iconTint = NovaTheme.colors.categoryDocuments,
                                iconBackground = NovaTheme.colors.categoryDocuments.copy(alpha = 0.2f),
                                onClick = { onCategoryClick("Documents") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            NovaCategoryTile(
                                title = "Audio",
                                sizeText = "Studio",
                                icon = NovaIcons.AudioWave,
                                iconTint = NovaTheme.colors.categoryAudio,
                                iconBackground = NovaTheme.colors.categoryAudio.copy(alpha = 0.2f),
                                onClick = { onCategoryClick("Audio") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Folders Horizontal List (Live Folders)
        if (folders.isNotEmpty()) {
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
                    folders.take(8).forEach { folder ->
                        NovaFolderCard(
                            name = folder.name,
                            itemCount = folder.itemCount ?: 0,
                            dateText = FileFormatters.formatDate(folder.lastModifiedMs),
                            cardColor = NovaTheme.colors.surfaceElevated,
                            folderIconColor = NovaTheme.colors.primary,
                            isSelected = false,
                            onClick = { onFolderClick(folder.path) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recent Files Section (Live Files)
        if (recentFiles.isNotEmpty()) {
            item {
                Text(
                    text = "Files",
                    style = NovaTheme.typography.title,
                    color = NovaTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(recentFiles.take(6).size) { index ->
                val file = recentFiles[index]
                NovaFileRow(
                    item = file,
                    formattedSize = FileFormatters.formatBytes(file.sizeBytes),
                    formattedDate = FileFormatters.formatDate(file.lastModifiedMs),
                    onClick = { onFileClick(file) },
                    onMenuClick = { onFileClick(file) }
                )
            }
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
