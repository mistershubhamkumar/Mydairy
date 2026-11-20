package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppPreferences
import com.example.data.DiaryEntry
import com.example.data.DiaryRepository
import com.example.data.LocalPreferencesStore
import com.example.data.MediaAndBackupManager
import com.example.data.UiState
import com.example.firebase.AiDiaryAction
import com.example.firebase.AiDiaryAssistantService
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutGoogle
import com.example.notifications.scheduleDailyReminder
import com.example.ui.components.MidnightSparkleBackground
import com.example.ui.components.StreakConfettiOverlay
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DiaryEditorScreen
import com.example.ui.screens.DreamJournalScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MoodTrackerScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhotoGalleryScreen
import com.example.ui.screens.PrivacySecurityScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SearchAndFilterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.ThemeStoreScreen
import com.example.ui.screens.VoiceDiaryScreen
import com.example.ui.theme.LocalDiaryThemeSpec
import com.example.ui.theme.MyDiaryTheme
import com.example.viewmodel.DiaryViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class MainTab { HOME, CALENDAR, STATS, PROFILE }

private sealed interface SubDestination {
    data class Editor(val entry: DiaryEntry? = null, val initialDateKey: String? = null) : SubDestination
    data object MoodTracker : SubDestination
    data object DreamJournal : SubDestination
    data object VoiceDiary : SubDestination
    data object PhotoGallery : SubDestination
    data object Categories : SubDestination
    data object Search : SubDestination
    data object Settings : SubDestination
    data object PrivacySecurity : SubDestination
    data object BackupRestore : SubDestination
    data object RecycleBin : SubDestination
    data object ThemeStore : SubDestination
    data object Reminders : SubDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRITICAL: Configure Firebase App Check before setContent, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MyDiaryRootApp()
        }
    }
}

@Composable
private fun MyDiaryRootApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    val preferencesStore = remember(appContext) { LocalPreferencesStore(appContext) }
    val rootPrefs by preferencesStore.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var splashFinished by remember { mutableStateOf(false) }

    MyDiaryTheme(themeId = rootPrefs.themeId, fontId = rootPrefs.fontId) {
        when {
            !splashFinished -> {
                SplashScreen(onFinished = { splashFinished = true })
            }
            !rootPrefs.onboardingCompleted -> {
                OnboardingScreen(
                    onComplete = {
                        scope.launch { preferencesStore.setOnboardingCompleted(true) }
                    }
                )
            }
            currentUser == null -> {
                AuthScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
            }
            else -> {
                val signedInUser = currentUser!!
                key(signedInUser.uid) {
                    AuthenticatedDiaryHost(
                        user = signedInUser,
                        preferencesStore = preferencesStore
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedDiaryHost(
    user: FirebaseUser,
    preferencesStore: LocalPreferencesStore
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DiaryViewModel = viewModel(
        key = user.uid,
        factory = remember(user.uid) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                    val repository = DiaryRepository(db)
                    val mediaManager = MediaAndBackupManager(appContext)
                    val aiService = AiDiaryAssistantService()
                    return DiaryViewModel(
                        repository = repository,
                        preferencesStore = preferencesStore,
                        mediaManager = mediaManager,
                        aiService = aiService,
                        currentUserId = user.uid,
                        initialDisplayName = user.displayName?.ifBlank { "Princess" } ?: "Princess",
                        initialEmail = user.email.orEmpty()
                    ) as T
                }
            }
        }
    )

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileUi as? UiState.Success)?.data
    val activeEntries by viewModel.activeEntries.collectAsStateWithLifecycle()
    val deletedEntries by viewModel.deletedEntries.collectAsStateWithLifecycle()
    val allMoodLogs by viewModel.allMoodLogs.collectAsStateWithLifecycle()
    val activeDreams by viewModel.activeDreams.collectAsStateWithLifecycle()
    val deletedDreams by viewModel.deletedDreams.collectAsStateWithLifecycle()
    val activeVoiceNotes by viewModel.activeVoiceNotes.collectAsStateWithLifecycle()
    val deletedVoiceNotes by viewModel.deletedVoiceNotes.collectAsStateWithLifecycle()
    val activeMemories by viewModel.activeMemories.collectAsStateWithLifecycle()
    val deletedMemories by viewModel.deletedMemories.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val backupHistory by viewModel.backupHistory.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakCelebrationDays.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val dailyQuote by viewModel.dailyQuote.collectAsStateWithLifecycle()

    // Session & background auto-lock management
    var sessionUnlocked by remember { mutableStateOf(false) }
    var lastPausedAtMillis by remember { mutableLongStateOf(0L) }
    var pendingPrivateEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, preferences.appLockEnabled, preferences.autoLockMinutes) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    lastPausedAtMillis = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (preferences.appLockEnabled && lastPausedAtMillis > 0L) {
                        val elapsedMs = System.currentTimeMillis() - lastPausedAtMillis
                        if (elapsedMs >= preferences.autoLockMinutes * 60_000L) {
                            sessionUnlocked = false
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.cleanupExpiredRecycleBinItems()
    }

    // App Lock gate
    if (preferences.appLockEnabled && !sessionUnlocked) {
        AppLockScreen(
            lockMode = preferences.lockMode,
            biometricEnabled = preferences.biometricEnabled,
            title = "Unlock My Diary 💗",
            onVerify = { viewModel.verifyLockSecret(it) },
            onUnlocked = { sessionUnlocked = true }
        )
        return
    }

    // Navigation state
    var activeTab by remember { mutableStateOf(MainTab.HOME) }
    val backStack = remember { mutableStateListOf<SubDestination>() }
    val currentSub = backStack.lastOrNull()

    fun pushSub(dest: SubDestination) {
        backStack.add(dest)
    }

    fun popSub() {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
    }

    fun openDiaryEntry(entry: DiaryEntry) {
        if (entry.privateLocked && preferences.appLockEnabled) {
            pendingPrivateEntry = entry
        } else {
            pushSub(SubDestination.Editor(entry = entry))
        }
    }

    // Private-locked individual entry unlock gate
    if (pendingPrivateEntry != null) {
        val targetEntry = pendingPrivateEntry!!
        AppLockScreen(
            lockMode = preferences.lockMode,
            biometricEnabled = preferences.biometricEnabled,
            title = "Unlock Secret Entry 🔐",
            onVerify = { viewModel.verifyLockSecret(it) },
            onUnlocked = {
                pendingPrivateEntry = null
                pushSub(SubDestination.Editor(entry = targetEntry))
            }
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val theme = LocalDiaryThemeSpec.current

    MidnightSparkleBackground(animationsEnabled = preferences.animationsEnabled) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = theme.cardSurfaceAlt,
                        contentColor = theme.textPrimary,
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            },
            bottomBar = {
                if (currentSub == null) {
                    GlowingDiaryBottomBar(
                        activeTab = activeTab,
                        onSelectTab = { activeTab = it },
                        onCreateNew = { pushSub(SubDestination.Editor()) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (currentSub == null) 0.dp else innerPadding.calculateBottomPadding())
            ) {
                when (val sub = currentSub) {
                    null -> {
                        when (activeTab) {
                            MainTab.HOME -> HomeDashboardScreen(
                                profile = profile,
                                preferences = preferences,
                                entries = activeEntries,
                                moodLogs = allMoodLogs,
                                stats = stats,
                                dailyQuote = dailyQuote,
                                onOpenNewEntry = { pushSub(SubDestination.Editor()) },
                                onEditEntry = { openDiaryEntry(it) },
                                onToggleFavorite = { viewModel.toggleEntryFavorite(it) },
                                onDeleteEntry = { viewModel.moveEntryToRecycleBin(it.id) },
                                onQuickLogMood = { viewModel.logMood(it, "Dashboard quick mood") },
                                onOpenCalendar = { activeTab = MainTab.CALENDAR },
                                onOpenMoodTracker = { pushSub(SubDestination.MoodTracker) },
                                onOpenPhotos = { pushSub(SubDestination.PhotoGallery) },
                                onOpenDreams = { pushSub(SubDestination.DreamJournal) },
                                onOpenVoiceDiary = { pushSub(SubDestination.VoiceDiary) },
                                onOpenCategories = { pushSub(SubDestination.Categories) },
                                onOpenSearch = { pushSub(SubDestination.Search) },
                                onOpenSettings = { pushSub(SubDestination.Settings) },
                                onOpenReminders = { pushSub(SubDestination.Reminders) },
                                onRefreshAiQuote = { viewModel.runAiAction(AiDiaryAction.DAILY_QUOTE, "") }
                            )

                            MainTab.CALENDAR -> CalendarScreen(
                                entries = activeEntries,
                                secretModeEnabled = preferences.secretModeEnabled,
                                animationsEnabled = preferences.animationsEnabled,
                                onAddEntryForDate = { dateKey -> pushSub(SubDestination.Editor(initialDateKey = dateKey)) },
                                onEditEntry = { openDiaryEntry(it) },
                                onToggleFavorite = { viewModel.toggleEntryFavorite(it) },
                                onDeleteEntry = { viewModel.moveEntryToRecycleBin(it.id) },
                                onOpenSearch = { pushSub(SubDestination.Search) }
                            )

                            MainTab.STATS -> StatisticsScreen(
                                stats = stats,
                                animationsEnabled = preferences.animationsEnabled,
                                onOpenMoodTracker = { pushSub(SubDestination.MoodTracker) }
                            )

                            MainTab.PROFILE -> ProfileScreen(
                                profile = profile,
                                preferences = preferences,
                                stats = stats,
                                aiState = aiState,
                                onUpdateProfile = { name, bio, avatar -> viewModel.updateProfile(name, bio, avatar) },
                                onRunAi = { action, input -> viewModel.runAiAction(action, input) },
                                onResetAi = { viewModel.resetAiState() },
                                onOpenThemeStore = { pushSub(SubDestination.ThemeStore) },
                                onOpenPrivacy = { pushSub(SubDestination.PrivacySecurity) },
                                onOpenBackup = { pushSub(SubDestination.BackupRestore) },
                                onOpenRecycleBin = { pushSub(SubDestination.RecycleBin) },
                                onOpenReminders = { pushSub(SubDestination.Reminders) },
                                onOpenSettings = { pushSub(SubDestination.Settings) }
                            )
                        }
                    }

                    is SubDestination.Editor -> DiaryEditorScreen(
                        existingEntry = sub.entry,
                        initialDateKey = sub.initialDateKey,
                        customCategories = customCategories,
                        mediaManager = viewModel.mediaManager,
                        aiState = aiState,
                        onRunAi = { action, sharedText -> viewModel.runAiAction(action, sharedText) },
                        onResetAi = { viewModel.resetAiState() },
                        onSave = { entry, closeAfter ->
                            viewModel.saveDiaryEntry(entry) {
                                if (closeAfter) popSub()
                            }
                        },
                        onOpenVoiceDiary = { pushSub(SubDestination.VoiceDiary) },
                        onBack = {
                            viewModel.resetAiState()
                            popSub()
                        }
                    )

                    SubDestination.MoodTracker -> MoodTrackerScreen(
                        moodLogs = allMoodLogs,
                        stats = stats,
                        animationsEnabled = preferences.animationsEnabled,
                        aiState = aiState,
                        onLogMood = { mood, note -> viewModel.logMood(mood, note) },
                        onDeleteMoodLog = { viewModel.deleteMoodLog(it) },
                        onRequestAiMoodInsight = { summary -> viewModel.runAiAction(AiDiaryAction.MOOD_INSIGHT, summary) },
                        onBack = {
                            viewModel.resetAiState()
                            popSub()
                        }
                    )

                    SubDestination.DreamJournal -> DreamJournalScreen(
                        dreams = activeDreams,
                        animationsEnabled = preferences.animationsEnabled,
                        mediaManager = viewModel.mediaManager,
                        onSaveDream = { viewModel.saveDream(it) },
                        onToggleFavorite = { viewModel.toggleDreamFavorite(it) },
                        onDeleteDream = { viewModel.moveDreamToRecycleBin(it.id) },
                        onBack = { popSub() }
                    )

                    SubDestination.VoiceDiary -> VoiceDiaryScreen(
                        voiceNotes = activeVoiceNotes,
                        mediaManager = viewModel.mediaManager,
                        onSaveVoiceNote = { t, d, p, dur -> viewModel.saveVoiceNote(t, d, p, dur) },
                        onUpdateVoiceNote = { v, t, d -> viewModel.updateVoiceNote(v, t, d) },
                        onDeleteVoiceNote = { viewModel.moveVoiceNoteToRecycleBin(it.id) },
                        onShowMessage = { viewModel.showStatus(it) },
                        onBack = { popSub() }
                    )

                    SubDestination.PhotoGallery -> PhotoGalleryScreen(
                        memories = activeMemories,
                        onUploadMemories = { uris, isVid, album, caption ->
                            viewModel.addPickedMemories(uris, isVid, album, caption)
                        },
                        onToggleFavorite = { viewModel.toggleMemoryFavorite(it) },
                        onUpdateMemory = { m, album, cap, tags, fav ->
                            viewModel.updateMemory(m, album, cap, tags, fav)
                        },
                        onDeleteMemory = { viewModel.moveMemoryToRecycleBin(it.id) },
                        onBack = { popSub() }
                    )

                    SubDestination.Categories -> CategoriesScreen(
                        entries = activeEntries,
                        customCategories = customCategories,
                        secretModeEnabled = preferences.secretModeEnabled,
                        onCreateCategory = { n, e, c, s -> viewModel.addCustomCategory(n, e, c, s) },
                        onDeleteCategory = { viewModel.deleteCustomCategory(it) },
                        onOpenEntry = { openDiaryEntry(it) },
                        onToggleFavorite = { viewModel.toggleEntryFavorite(it) },
                        onDeleteEntry = { viewModel.moveEntryToRecycleBin(it.id) },
                        onBack = { popSub() }
                    )

                    SubDestination.Search -> SearchAndFilterScreen(
                        entries = activeEntries,
                        customCategories = customCategories,
                        secretModeEnabled = preferences.secretModeEnabled,
                        onOpenEntry = { openDiaryEntry(it) },
                        onToggleFavorite = { viewModel.toggleEntryFavorite(it) },
                        onDeleteEntry = { viewModel.moveEntryToRecycleBin(it.id) },
                        onBack = { popSub() }
                    )

                    SubDestination.ThemeStore -> ThemeStoreScreen(
                        preferences = preferences,
                        onSelectTheme = { viewModel.selectTheme(it) },
                        onSelectFont = { viewModel.selectFont(it) },
                        onSelectMascot = { viewModel.selectMascot(it) },
                        onToggleAnimations = { viewModel.setAnimationsEnabled(it) },
                        onBack = { popSub() }
                    )

                    SubDestination.Reminders -> RemindersScreen(
                        preferences = preferences,
                        onUpdateReminders = { notif, daily, h, m, days, mood, grat, streak ->
                            viewModel.updateReminderSettings(notif, daily, h, m, days, mood, grat, streak)
                        },
                        onToggleSoundVibration = { viewModel.setSoundVibration(it) },
                        onShowMessage = { viewModel.showStatus(it) },
                        onBack = { popSub() }
                    )

                    SubDestination.PrivacySecurity -> PrivacySecurityScreen(
                        preferences = preferences,
                        onSetAppLock = { mode, secret ->
                            viewModel.setAppLock(mode, secret)
                            sessionUnlocked = true
                        },
                        onDisableAppLock = { viewModel.disableAppLock() },
                        onSetBiometric = { viewModel.setBiometricEnabled(it) },
                        onSetAutoLockMinutes = { viewModel.setAutoLockMinutes(it) },
                        onSetSecretMode = { viewModel.setSecretMode(it) },
                        onOpenBackupRestore = { pushSub(SubDestination.BackupRestore) },
                        onClearAllData = { viewModel.clearAllUserData(deleteProfile = false) },
                        onBack = { popSub() }
                    )

                    SubDestination.BackupRestore -> BackupRestoreScreen(
                        preferences = preferences,
                        customCategories = customCategories,
                        backupHistory = backupHistory,
                        totalEntries = activeEntries.size,
                        totalDreams = activeDreams.size,
                        onToggleAutoBackup = { viewModel.setAutoCloudBackup(it) },
                        onCloudSnapshot = { viewModel.recordCloudSnapshot() },
                        onExportToUri = { uri, fmt, catId -> viewModel.exportToUri(uri, fmt, catId) },
                        onImportJsonUri = { uri -> viewModel.importFromJsonUri(uri) },
                        onBack = { popSub() }
                    )

                    SubDestination.RecycleBin -> RecycleBinScreen(
                        preferences = preferences,
                        deletedEntries = deletedEntries,
                        deletedDreams = deletedDreams,
                        deletedMemories = deletedMemories,
                        deletedVoiceNotes = deletedVoiceNotes,
                        onToggleAutoCleanup = { viewModel.setAutoCleanupRecycleBin(it) },
                        onRestoreEntry = { viewModel.restoreEntry(it) },
                        onPermanentDeleteEntry = { viewModel.permanentlyDeleteEntry(it) },
                        onRestoreDream = { viewModel.restoreDream(it) },
                        onPermanentDeleteDream = { viewModel.permanentlyDeleteDream(it) },
                        onRestoreMemory = { viewModel.restoreMemory(it) },
                        onPermanentDeleteMemory = { viewModel.permanentlyDeleteMemory(it) },
                        onRestoreVoice = { viewModel.restoreVoiceNote(it) },
                        onPermanentDeleteVoice = { viewModel.permanentlyDeleteVoiceNote(it) },
                        onEmptyBin = { viewModel.emptyRecycleBin() },
                        onBack = { popSub() }
                    )

                    SubDestination.Settings -> SettingsScreen(
                        profile = profile,
                        preferences = preferences,
                        localStorageBytes = remember(activeMemories.size, activeVoiceNotes.size) {
                            viewModel.mediaManager.computeLocalStorageBytes()
                        },
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        onToggleNotifications = { enabled ->
                            viewModel.updateReminderSettings(notificationsEnabled = enabled)
                            if (!enabled) {
                                scheduleDailyReminder(context, false, preferences.reminderHour, preferences.reminderMinute)
                            }
                        },
                        onToggleSoundVibration = { viewModel.setSoundVibration(it) },
                        onOpenThemeStore = { pushSub(SubDestination.ThemeStore) },
                        onOpenReminders = { pushSub(SubDestination.Reminders) },
                        onOpenPrivacy = { pushSub(SubDestination.PrivacySecurity) },
                        onOpenBackup = { pushSub(SubDestination.BackupRestore) },
                        onOpenRecycleBin = { pushSub(SubDestination.RecycleBin) },
                        onLogout = {
                            signOutGoogle(credentialManager, scope)
                        },
                        onDeleteAccount = {
                            viewModel.clearAllUserData(deleteProfile = true) {
                                scope.launch {
                                    try {
                                        Firebase.auth.currentUser?.delete()?.await()
                                    } catch (_: Exception) {
                                        // Recent login required by Firebase; still sign out cleanly after wiping data
                                    }
                                    signOutGoogle(credentialManager, scope)
                                }
                            }
                        },
                        onBack = { popSub() }
                    )
                }
            }
        }

        StreakConfettiOverlay(
            visible = streakDays != null,
            streakDays = streakDays ?: 0,
            onDismiss = { viewModel.dismissStreakCelebration() }
        )
    }
}

@Composable
private fun GlowingDiaryBottomBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onCreateNew: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = theme.cardSurfaceAlt.copy(alpha = 0.94f),
            border = BorderStroke(
                1.4.dp,
                Brush.horizontalGradient(listOf(theme.borderGlow, theme.secondary, theme.borderGlow))
            ),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = activeTab == MainTab.HOME,
                    tag = "nav_home",
                    onClick = { onSelectTab(MainTab.HOME) }
                )
                BottomNavItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "Calendar",
                    selected = activeTab == MainTab.CALENDAR,
                    tag = "nav_calendar",
                    onClick = { onSelectTab(MainTab.CALENDAR) }
                )
                Spacer(Modifier.size(60.dp))
                BottomNavItem(
                    icon = Icons.Default.BarChart,
                    label = "Stats",
                    selected = activeTab == MainTab.STATS,
                    tag = "nav_stats",
                    onClick = { onSelectTab(MainTab.STATS) }
                )
                BottomNavItem(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    selected = activeTab == MainTab.PROFILE,
                    tag = "nav_profile",
                    onClick = { onSelectTab(MainTab.PROFILE) }
                )
            }
        }

        // Glowing floating center Create (+) button
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(62.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = theme.primary,
                    ambientColor = theme.primary
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .testTag("nav_create_fab")
                .clickable(onClick = onCreateNew),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Diary Entry",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val theme = LocalDiaryThemeSpec.current
    val tint = if (selected) theme.primary else theme.textSecondary.copy(alpha = 0.82f)
    Column(
        modifier = Modifier
            .size(width = 62.dp, height = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag(tag)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
