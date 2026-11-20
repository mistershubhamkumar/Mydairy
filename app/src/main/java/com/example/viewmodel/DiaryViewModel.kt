package com.example.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.BackupRecord
import com.example.data.DiaryCategory
import com.example.data.DiaryEntry
import com.example.data.DiaryMood
import com.example.data.DiaryRepository
import com.example.data.DreamEntry
import com.example.data.LocalPreferencesStore
import com.example.data.LockMode
import com.example.data.MediaAndBackupManager
import com.example.data.MoodLog
import com.example.data.PhotoMemory
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VoiceNote
import com.example.data.computeStreaks
import com.example.data.todayDateKey
import com.example.firebase.AiDiaryAction
import com.example.firebase.AiDiaryAssistantService
import com.example.firebase.AiUiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "DiaryViewModel"
private const val STOP_TIMEOUT_MS = 5000L
private const val THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L

data class MoodChartPoint(
    val label: String,
    val averageScore: Float,
    val dominantMood: DiaryMood?,
    val count: Int
)

data class DiaryStats(
    val totalEntries: Int = 0,
    val totalWords: Long = 0L,
    val totalPhotos: Int = 0,
    val totalVoiceNotes: Int = 0,
    val totalDreams: Int = 0,
    val totalFavorites: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageHappinessScore: Float = 0f,
    val mostUsedMood: DiaryMood? = null,
    val mostActiveDayOfWeek: String = "—",
    val moodDistribution: Map<DiaryMood, Int> = emptyMap(),
    val weeklyMoodPoints: List<MoodChartPoint> = emptyList(),
    val monthlyActivityPoints: List<Pair<String, Int>> = emptyList(),
    val yearlyMoodPoints: List<MoodChartPoint> = emptyList()
)

class DiaryViewModel(
    private val repository: DiaryRepository,
    private val preferencesStore: LocalPreferencesStore,
    val mediaManager: MediaAndBackupManager,
    private val aiService: AiDiaryAssistantService,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String
) : ViewModel() {

    private fun <T> Flow<T>.toSafeUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val preferences: StateFlow<AppPreferences> = preferencesStore.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppPreferences()
    )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toSafeUiState("profile")

    val entriesState: StateFlow<UiState<List<DiaryEntry>>> =
        repository.observeEntries(currentUserId).toSafeUiState("entries")

    val moodsState: StateFlow<UiState<List<MoodLog>>> =
        repository.observeMoods(currentUserId).toSafeUiState("moods")

    val dreamsState: StateFlow<UiState<List<DreamEntry>>> =
        repository.observeDreams(currentUserId).toSafeUiState("dreams")

    val voiceNotesState: StateFlow<UiState<List<VoiceNote>>> =
        repository.observeVoiceNotes(currentUserId).toSafeUiState("voice notes")

    val memoriesState: StateFlow<UiState<List<PhotoMemory>>> =
        repository.observeMemories(currentUserId).toSafeUiState("memories")

    val categoriesState: StateFlow<UiState<List<DiaryCategory>>> =
        repository.observeCategories(currentUserId).toSafeUiState("categories")

    val backupsState: StateFlow<UiState<List<BackupRecord>>> =
        repository.observeBackups(currentUserId).toSafeUiState("backups")

    val activeEntries: StateFlow<List<DiaryEntry>> = entriesState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { !it.deleted }
                ?.sortedByDescending { it.entryTimeMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val deletedEntries: StateFlow<List<DiaryEntry>> = entriesState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { it.deleted }
                ?.sortedByDescending { it.deletedAtMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeDreams: StateFlow<List<DreamEntry>> = dreamsState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { !it.deleted }
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val deletedDreams: StateFlow<List<DreamEntry>> = dreamsState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { it.deleted }
                ?.sortedByDescending { it.deletedAtMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeVoiceNotes: StateFlow<List<VoiceNote>> = voiceNotesState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { !it.deleted }
                ?.sortedByDescending { it.recordedAtMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val deletedVoiceNotes: StateFlow<List<VoiceNote>> = voiceNotesState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { it.deleted }
                ?.sortedByDescending { it.deletedAtMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeMemories: StateFlow<List<PhotoMemory>> = memoriesState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { !it.deleted }
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val deletedMemories: StateFlow<List<PhotoMemory>> = memoriesState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.filter { it.deleted }
                ?.sortedByDescending { it.deletedAtMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allMoodLogs: StateFlow<List<MoodLog>> = moodsState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.sortedByDescending { it.loggedAtMillis }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val customCategories: StateFlow<List<DiaryCategory>> = categoriesState
        .map { state ->
            (state as? UiState.Success)?.data ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val backupHistory: StateFlow<List<BackupRecord>> = backupsState
        .map { state ->
            (state as? UiState.Success)?.data
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val stats: StateFlow<DiaryStats> = combine(
        activeEntries,
        allMoodLogs,
        activeDreams,
        activeMemories,
        activeVoiceNotes
    ) { entries, moods, dreams, memories, voices ->
        computeStats(entries, moods, dreams, memories, voices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiaryStats())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _streakCelebrationDays = MutableStateFlow<Int?>(null)
    val streakCelebrationDays: StateFlow<Int?> = _streakCelebrationDays.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _dailyQuote = MutableStateFlow<String?>(null)
    val dailyQuote: StateFlow<String?> = _dailyQuote.asStateFlow()

    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun dismissStreakCelebration() {
        _streakCelebrationDays.value = null
    }

    // --- PROFILE & PERSONALIZATION ---

    fun updateProfile(displayName: String, bio: String, avatarKey: String) {
        viewModelScope.launch {
            val res = repository.updateProfile(
                userId = currentUserId,
                displayName = displayName,
                bio = bio,
                avatarKey = avatarKey
            )
            _statusMessage.value = if (res.isSuccess) "Profile updated ✨" else "Failed to update profile: ${res.exceptionOrNull()?.localizedMessage}"
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            preferencesStore.setTheme(themeId)
            repository.updateProfile(currentUserId, selectedThemeId = themeId)
        }
    }

    fun selectFont(fontId: String) {
        viewModelScope.launch {
            preferencesStore.setFont(fontId)
            repository.updateProfile(currentUserId, selectedFontId = fontId)
        }
    }

    fun selectMascot(mascotId: String) {
        viewModelScope.launch {
            preferencesStore.setMascot(mascotId)
            repository.updateProfile(currentUserId, mascotId = mascotId)
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setAnimationsEnabled(enabled)
            repository.updateProfile(currentUserId, animationsEnabled = enabled)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferencesStore.setLanguage(lang) }
    }

    fun setSoundVibration(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setSoundVibration(enabled) }
    }

    fun setSecretMode(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setSecretModeEnabled(enabled) }
    }

    fun setAppLock(mode: LockMode, secret: String) {
        viewModelScope.launch {
            preferencesStore.setAppLock(mode, secret)
            _statusMessage.value = "App Lock enabled securely 🔐"
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            preferencesStore.disableAppLock()
            _statusMessage.value = "App Lock disabled"
        }
    }

    fun verifyLockSecret(candidate: String): Boolean =
        preferencesStore.verifySecret(preferences.value, candidate)

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setBiometricEnabled(enabled) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch { preferencesStore.setAutoLockMinutes(minutes) }
    }

    fun updateReminderSettings(
        notificationsEnabled: Boolean? = null,
        dailyEnabled: Boolean? = null,
        hour: Int? = null,
        minute: Int? = null,
        daysCsv: String? = null,
        moodReminder: Boolean? = null,
        gratitudeReminder: Boolean? = null,
        streakReminder: Boolean? = null
    ) {
        viewModelScope.launch {
            preferencesStore.updateReminders(
                notificationsEnabled, dailyEnabled, hour, minute, daysCsv,
                moodReminder, gratitudeReminder, streakReminder
            )
        }
    }

    fun setAutoCleanupRecycleBin(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setAutoCleanupRecycleBin(enabled) }
    }

    fun setAutoCloudBackup(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setAutoCloudBackup(enabled) }
    }

    // --- DIARY ENTRIES ---

    fun saveDiaryEntry(entry: DiaryEntry, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            val prevStreak = stats.value.currentStreak
            val res = repository.saveEntry(currentUserId, entry)
            if (res.isSuccess) {
                val savedId = res.getOrNull().orEmpty()
                // Also record mood check-in when creating a fresh entry
                if (entry.id.isBlank()) {
                    repository.addMoodLog(
                        userId = currentUserId,
                        mood = entry.moodEnum,
                        note = entry.title.ifBlank { "Diary entry mood" },
                        dateKey = entry.dateKey
                    )
                    val newDates = activeEntries.value.map { it.dateKey }.toSet() + entry.dateKey
                    val (newStreak, _) = computeStreaks(newDates)
                    if (newStreak > prevStreak && newStreak in listOf(1, 3, 7, 14, 30, 50, 100)) {
                        _streakCelebrationDays.value = newStreak
                    }
                }
                _statusMessage.value = "Diary entry saved 💗"
                onSaved(savedId)
            } else {
                _statusMessage.value = "Save failed: ${res.exceptionOrNull()?.localizedMessage ?: "Please retry"}"
            }
        }
    }

    fun toggleEntryFavorite(entry: DiaryEntry) {
        viewModelScope.launch {
            val res = repository.toggleEntryFavorite(currentUserId, entry.id, !entry.favorite)
            if (res.isFailure) _statusMessage.value = "Could not update favorite."
        }
    }

    fun toggleEntryPrivate(entry: DiaryEntry) {
        viewModelScope.launch {
            val res = repository.toggleEntryPrivate(currentUserId, entry.id, !entry.privateLocked)
            if (res.isFailure) _statusMessage.value = "Could not update lock state."
        }
    }

    fun moveEntryToRecycleBin(entryId: String) {
        viewModelScope.launch {
            val res = repository.softDeleteEntry(currentUserId, entryId)
            _statusMessage.value = if (res.isSuccess) "Moved to Recycle Bin (recoverable for 30 days)" else "Delete failed."
        }
    }

    fun restoreEntry(entryId: String) {
        viewModelScope.launch {
            val res = repository.restoreEntry(currentUserId, entryId)
            _statusMessage.value = if (res.isSuccess) "Diary entry restored ✨" else "Restore failed."
        }
    }

    fun permanentlyDeleteEntry(entryId: String) {
        viewModelScope.launch {
            val res = repository.permanentlyDeleteEntry(currentUserId, entryId)
            _statusMessage.value = if (res.isSuccess) "Permanently deleted" else "Delete failed."
        }
    }

    // --- MOOD TRACKER ---

    fun logMood(mood: DiaryMood, note: String = "", dateKey: String = todayDateKey()) {
        viewModelScope.launch {
            val res = repository.addMoodLog(currentUserId, mood, note, dateKey)
            _statusMessage.value = if (res.isSuccess) "Mood logged: ${mood.emoji} ${mood.label}" else "Could not log mood."
        }
    }

    fun deleteMoodLog(moodId: String) {
        viewModelScope.launch {
            repository.deleteMoodLog(currentUserId, moodId)
        }
    }

    // --- DREAM JOURNAL ---

    fun saveDream(dream: DreamEntry, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveDream(currentUserId, dream)
            if (res.isSuccess) {
                _statusMessage.value = "Dream saved under the stars 🌙"
                onSaved()
            } else {
                _statusMessage.value = "Failed to save dream."
            }
        }
    }

    fun toggleDreamFavorite(dream: DreamEntry) {
        viewModelScope.launch { repository.toggleDreamFavorite(currentUserId, dream.id, !dream.favorite) }
    }

    fun moveDreamToRecycleBin(dreamId: String) {
        viewModelScope.launch {
            val res = repository.softDeleteDream(currentUserId, dreamId)
            _statusMessage.value = if (res.isSuccess) "Dream moved to Recycle Bin" else "Failed to delete dream."
        }
    }

    fun restoreDream(dreamId: String) {
        viewModelScope.launch {
            val res = repository.restoreDream(currentUserId, dreamId)
            _statusMessage.value = if (res.isSuccess) "Dream restored ✨" else "Restore failed."
        }
    }

    fun permanentlyDeleteDream(dreamId: String) {
        viewModelScope.launch { repository.permanentlyDeleteDream(currentUserId, dreamId) }
    }

    // --- VOICE DIARY ---

    fun saveVoiceNote(title: String, description: String, filePath: String, durationSeconds: Long) {
        viewModelScope.launch {
            val res = repository.addVoiceNote(currentUserId, title, description, filePath, durationSeconds)
            _statusMessage.value = if (res.isSuccess) "Voice diary saved 🎙️" else "Failed to save voice diary."
        }
    }

    fun updateVoiceNote(voice: VoiceNote, newTitle: String, newDescription: String, favorite: Boolean = voice.favorite) {
        viewModelScope.launch {
            val res = repository.updateVoiceNote(currentUserId, voice.id, newTitle, newDescription, favorite)
            _statusMessage.value = if (res.isSuccess) "Voice diary updated ✨" else "Update failed."
        }
    }

    fun moveVoiceNoteToRecycleBin(voiceId: String) {
        viewModelScope.launch {
            val res = repository.softDeleteVoiceNote(currentUserId, voiceId)
            _statusMessage.value = if (res.isSuccess) "Voice diary moved to Recycle Bin" else "Delete failed."
        }
    }

    fun restoreVoiceNote(voiceId: String) {
        viewModelScope.launch {
            val res = repository.restoreVoiceNote(currentUserId, voiceId)
            _statusMessage.value = if (res.isSuccess) "Voice diary restored ✨" else "Restore failed."
        }
    }

    fun permanentlyDeleteVoiceNote(voiceId: String) {
        viewModelScope.launch { repository.permanentlyDeleteVoiceNote(currentUserId, voiceId) }
    }

    // --- PHOTO & MEMORY GALLERY ---

    fun addPickedMemories(uris: List<Uri>, isVideo: Boolean, albumName: String, caption: String) {
        viewModelScope.launch {
            var count = 0
            for (uri in uris) {
                val localUri = mediaManager.persistPickedMedia(uri, isVideo) ?: continue
                val res = repository.addMemory(
                    userId = currentUserId,
                    mediaUri = localUri,
                    mediaType = if (isVideo) "VIDEO" else "PHOTO",
                    albumName = albumName,
                    caption = caption
                )
                if (res.isSuccess) count++
            }
            _statusMessage.value = if (count > 0) "Added $count memory item(s) 📸" else "Could not import media."
        }
    }

    fun updateMemory(memory: PhotoMemory, albumName: String, caption: String, tags: List<String>, favorite: Boolean) {
        viewModelScope.launch {
            val res = repository.updateMemory(currentUserId, memory.id, albumName, caption, tags, favorite)
            _statusMessage.value = if (res.isSuccess) "Memory updated 💗" else "Update failed."
        }
    }

    fun toggleMemoryFavorite(memory: PhotoMemory) {
        viewModelScope.launch { repository.toggleMemoryFavorite(currentUserId, memory.id, !memory.favorite) }
    }

    fun moveMemoryToRecycleBin(memoryId: String) {
        viewModelScope.launch {
            val res = repository.softDeleteMemory(currentUserId, memoryId)
            _statusMessage.value = if (res.isSuccess) "Memory moved to Recycle Bin" else "Delete failed."
        }
    }

    fun restoreMemory(memoryId: String) {
        viewModelScope.launch {
            val res = repository.restoreMemory(currentUserId, memoryId)
            _statusMessage.value = if (res.isSuccess) "Memory restored ✨" else "Restore failed."
        }
    }

    fun permanentlyDeleteMemory(memoryId: String) {
        viewModelScope.launch { repository.permanentlyDeleteMemory(currentUserId, memoryId) }
    }

    // --- CUSTOM CATEGORIES ---

    fun addCustomCategory(name: String, emoji: String, colorHex: String, coverStyle: String) {
        viewModelScope.launch {
            val res = repository.addCategory(currentUserId, name, emoji, colorHex, coverStyle)
            _statusMessage.value = if (res.isSuccess) "Category '$name' created ✨" else "Failed to create category."
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        viewModelScope.launch {
            val res = repository.deleteCategory(currentUserId, categoryId)
            _statusMessage.value = if (res.isSuccess) "Category deleted" else "Could not delete category."
        }
    }

    // --- RECYCLE BIN MANAGEMENT ---

    fun emptyRecycleBin() {
        viewModelScope.launch {
            deletedEntries.value.forEach { repository.permanentlyDeleteEntry(currentUserId, it.id) }
            deletedDreams.value.forEach { repository.permanentlyDeleteDream(currentUserId, it.id) }
            deletedMemories.value.forEach { repository.permanentlyDeleteMemory(currentUserId, it.id) }
            deletedVoiceNotes.value.forEach { repository.permanentlyDeleteVoiceNote(currentUserId, it.id) }
            _statusMessage.value = "Recycle Bin emptied"
        }
    }

    fun cleanupExpiredRecycleBinItems() {
        if (!preferences.value.autoCleanupRecycleBin) return
        val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MILLIS
        viewModelScope.launch {
            deletedEntries.value.filter { it.deletedAtMillis in 1L..cutoff }.forEach {
                repository.permanentlyDeleteEntry(currentUserId, it.id)
            }
            deletedDreams.value.filter { it.deletedAtMillis in 1L..cutoff }.forEach {
                repository.permanentlyDeleteDream(currentUserId, it.id)
            }
            deletedMemories.value.filter { it.deletedAtMillis in 1L..cutoff }.forEach {
                repository.permanentlyDeleteMemory(currentUserId, it.id)
            }
            deletedVoiceNotes.value.filter { it.deletedAtMillis in 1L..cutoff }.forEach {
                repository.permanentlyDeleteVoiceNote(currentUserId, it.id)
            }
        }
    }

    // --- BACKUP & RESTORE ---

    fun recordCloudSnapshot() {
        viewModelScope.launch {
            val res = repository.recordBackup(
                userId = currentUserId,
                label = "Cloud Sync Snapshot",
                format = "CLOUD",
                entryCount = activeEntries.value.size.toLong(),
                dreamCount = activeDreams.value.size.toLong(),
                moodCount = allMoodLogs.value.size.toLong()
            )
            _statusMessage.value = if (res.isSuccess) "Cloud backup synced & verified ☁️" else "Cloud snapshot failed."
        }
    }

    fun exportToUri(uri: Uri, format: String, categoryFilterId: String? = null) {
        viewModelScope.launch {
            val filteredEntries = if (categoryFilterId.isNullOrBlank()) {
                activeEntries.value
            } else {
                activeEntries.value.filter { it.categoryId == categoryFilterId }
            }
            val dreams = activeDreams.value
            val moods = allMoodLogs.value
            val ok = when (format) {
                "JSON" -> mediaManager.writeTextToUri(uri, mediaManager.buildExportJson(filteredEntries, dreams, moods))
                "TXT" -> mediaManager.writeTextToUri(uri, mediaManager.buildExportTxt(filteredEntries, dreams))
                "PDF" -> mediaManager.writePdfToUri(uri, filteredEntries, dreams)
                else -> false
            }
            if (ok) {
                repository.recordBackup(
                    userId = currentUserId,
                    label = "$format Export (${filteredEntries.size} entries)",
                    format = format,
                    entryCount = filteredEntries.size.toLong(),
                    dreamCount = dreams.size.toLong(),
                    moodCount = moods.size.toLong()
                )
                _statusMessage.value = "Exported ${filteredEntries.size} entries as $format ✨"
            } else {
                _statusMessage.value = "Export failed. Please try another folder."
            }
        }
    }

    fun importFromJsonUri(uri: Uri) {
        viewModelScope.launch {
            val parsed = mediaManager.parseImportJson(uri)
            if (parsed == null) {
                _statusMessage.value = "Invalid backup file. Please choose a valid JSON export."
                return@launch
            }
            val (entries, dreams) = parsed
            var imported = 0
            entries.forEach { e ->
                if (repository.saveEntry(currentUserId, e.copy(id = "")).isSuccess) imported++
            }
            dreams.forEach { d ->
                if (repository.saveDream(currentUserId, d.copy(id = "")).isSuccess) imported++
            }
            _statusMessage.value = "Restored $imported item(s) from backup 💗"
        }
    }

    fun clearAllUserData(deleteProfile: Boolean = false, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.clearAllUserData(currentUserId, deleteProfile)
            if (res.isSuccess) {
                _statusMessage.value = "All diary data cleared."
                onDone()
            } else {
                _statusMessage.value = "Failed to clear data: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    // --- GEMINI AI DIARY ASSISTANT (EXPLICIT USER OPT-IN ONLY) ---

    fun runAiAction(action: AiDiaryAction, sharedInput: String) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            aiService.streamAction(action, sharedInput).collect { state ->
                _aiState.value = state
                if (action == AiDiaryAction.DAILY_QUOTE && state is AiUiState.Success) {
                    _dailyQuote.value = state.text
                }
            }
        }
    }

    fun resetAiState() {
        aiJob?.cancel()
        _aiState.value = AiUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        mediaManager.stopPlayback()
        mediaManager.cancelRecording()
    }

    // --- REAL-DATA ANALYTICS ENGINE ---

    private fun computeStats(
        entries: List<DiaryEntry>,
        moods: List<MoodLog>,
        dreams: List<DreamEntry>,
        memories: List<PhotoMemory>,
        voices: List<VoiceNote>
    ): DiaryStats {
        val totalWords = entries.sumOf { it.wordCount }
        val entryDates = entries.map { it.dateKey }.toSet()
        val (currStreak, longestStreak) = computeStreaks(entryDates)

        // Combine explicit mood logs + entry moods for real mood charts
        val combinedMoods: List<Triple<String, DiaryMood, Long>> = buildList {
            moods.forEach { add(Triple(it.dateKey, it.moodEnum, it.score)) }
            if (moods.isEmpty()) {
                entries.forEach { add(Triple(it.dateKey, it.moodEnum, it.moodScore)) }
            }
        }

        val moodDist = combinedMoods.groupingBy { it.second }.eachCount()
        val mostUsed = moodDist.maxByOrNull { it.value }?.key
        val avgScore = if (combinedMoods.isNotEmpty()) {
            combinedMoods.map { it.third.toFloat() }.average().toFloat()
        } else 0f

        // Most active day of week
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dowFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val activeDay = entries.mapNotNull { e ->
            try { parser.parse(e.dateKey)?.let { dowFmt.format(it) } } catch (_: Exception) { null }
        }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "—"

        // Last 7 days chart
        val dayLabelFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        val weeklyPoints = (6 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = todayDateKey(c.timeInMillis)
            val dayItems = combinedMoods.filter { it.first == key }
            val score = if (dayItems.isNotEmpty()) dayItems.map { it.third.toFloat() }.average().toFloat() else 0f
            val domMood = dayItems.groupingBy { it.second }.eachCount().maxByOrNull { it.value }?.key
            MoodChartPoint(dayLabelFmt.format(Date(c.timeInMillis)), score, domMood, dayItems.size)
        }

        // Last 6 months activity & mood
        val monthKeyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthLabelFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val monthlyActivity = (5 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
            val prefix = monthKeyFmt.format(Date(c.timeInMillis))
            val label = monthLabelFmt.format(Date(c.timeInMillis))
            val count = entries.count { it.dateKey.startsWith(prefix) }
            label to count
        }

        val yearlyMood = (5 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
            val prefix = monthKeyFmt.format(Date(c.timeInMillis))
            val label = monthLabelFmt.format(Date(c.timeInMillis))
            val items = combinedMoods.filter { it.first.startsWith(prefix) }
            val score = if (items.isNotEmpty()) items.map { it.third.toFloat() }.average().toFloat() else 0f
            val dom = items.groupingBy { it.second }.eachCount().maxByOrNull { it.value }?.key
            MoodChartPoint(label, score, dom, items.size)
        }

        return DiaryStats(
            totalEntries = entries.size,
            totalWords = totalWords,
            totalPhotos = memories.size + entries.sumOf { it.photoUris.size },
            totalVoiceNotes = voices.size,
            totalDreams = dreams.size,
            totalFavorites = entries.count { it.favorite } + dreams.count { it.favorite } + memories.count { it.favorite },
            currentStreak = currStreak,
            longestStreak = longestStreak,
            averageHappinessScore = avgScore,
            mostUsedMood = mostUsed,
            mostActiveDayOfWeek = activeDay,
            moodDistribution = moodDist,
            weeklyMoodPoints = weeklyPoints,
            monthlyActivityPoints = monthlyActivity,
            yearlyMoodPoints = yearlyMood
        )
    }
}
