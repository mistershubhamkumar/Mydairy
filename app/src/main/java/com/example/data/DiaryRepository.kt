package com.example.data

import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DiaryRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    private fun requireUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun <T> observeSubcollection(
        userId: String,
        subcollection: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUid(userId)
        val path = "users/$uid/$subcollection"
        emitAll(
            db.collection("users").document(uid).collection(subcollection)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUid(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeEntries(userId: String): Flow<List<DiaryEntry>> =
        observeSubcollection(userId, "entries", DiaryEntry::class.java)

    fun observeMoods(userId: String): Flow<List<MoodLog>> =
        observeSubcollection(userId, "moods", MoodLog::class.java)

    fun observeDreams(userId: String): Flow<List<DreamEntry>> =
        observeSubcollection(userId, "dreams", DreamEntry::class.java)

    fun observeVoiceNotes(userId: String): Flow<List<VoiceNote>> =
        observeSubcollection(userId, "voiceNotes", VoiceNote::class.java)

    fun observeMemories(userId: String): Flow<List<PhotoMemory>> =
        observeSubcollection(userId, "memories", PhotoMemory::class.java)

    fun observeCategories(userId: String): Flow<List<DiaryCategory>> =
        observeSubcollection(userId, "categories", DiaryCategory::class.java)

    fun observeBackups(userId: String): Flow<List<BackupRecord>> =
        observeSubcollection(userId, "backups", BackupRecord::class.java)

    // --- USER PROFILE ---

    suspend fun ensureUserProfile(
        userId: String,
        defaultName: String,
        email: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = defaultName.trim().ifBlank { "Princess" }.take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(120),
                    "bio" to "Write • Feel • Heal • Remember Forever",
                    "avatarKey" to "princess",
                    "selectedThemeId" to "pink_princess",
                    "selectedFontId" to "quicksand",
                    "mascotId" to "white_cat",
                    "animationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String? = null,
        bio: String? = null,
        avatarKey: String? = null,
        selectedThemeId: String? = null,
        selectedFontId: String? = null,
        mascotId: String? = null,
        animationsEnabled: Boolean? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUid(userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["displayName"] = it.take(60) }
            bio?.let { updates["bio"] = it.trim().take(300) }
            avatarKey?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["avatarKey"] = it.take(300) }
            selectedThemeId?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["selectedThemeId"] = it.take(40) }
            selectedFontId?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["selectedFontId"] = it.take(40) }
            mascotId?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["mascotId"] = it.take(40) }
            animationsEnabled?.let { updates["animationsEnabled"] = it }

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- DIARY ENTRIES ---

    suspend fun saveEntry(userId: String, entry: DiaryEntry): Result<String> {
        val isCreate = entry.id.isBlank()
        var docPath = "users/$userId/entries/${entry.id}"
        return try {
            val uid = requireUid(userId)
            val colRef = db.collection("users").document(uid).collection("entries")
            val docRef = if (isCreate) colRef.document() else colRef.document(entry.id)
            docPath = docRef.path

            val safeTitle = entry.title.trim().take(150)
            val safeContent = entry.content.take(50000)
            val words = safeContent.trim().split(Regex("\\s+")).count { it.isNotBlank() }.toLong().coerceIn(0L, 100000L)
            val chars = safeContent.length.toLong().coerceIn(0L, 500000L)
            val moodEnum = DiaryMood.fromRaw(entry.mood)
            val safeDateKey = if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(entry.dateKey)) entry.dateKey else todayDateKey()
            val safeAlign = if (entry.textAlign in listOf("START", "CENTER", "END")) entry.textAlign else "START"
            val safeTags = entry.tags.map { it.trim().take(30) }.filter { it.isNotEmpty() }.distinct().take(15)
            val safePhotos = entry.photoUris.map { it.trim().take(500) }.filter { it.isNotEmpty() }.take(20)
            val safeStickers = entry.stickerIds.map { it.trim().take(20) }.filter { it.isNotEmpty() }.take(15)

            if (isCreate) {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to safeTitle,
                    "content" to safeContent,
                    "mood" to moodEnum.name,
                    "moodScore" to moodEnum.score.coerceIn(1L, 10L),
                    "categoryId" to entry.categoryId.trim().ifBlank { "personal" }.take(60),
                    "dateKey" to safeDateKey,
                    "entryTimeMillis" to entry.entryTimeMillis.coerceAtLeast(0L),
                    "tags" to safeTags,
                    "photoUris" to safePhotos,
                    "locationLabel" to entry.locationLabel.trim().take(120),
                    "stickerIds" to safeStickers,
                    "backgroundStyle" to entry.backgroundStyle.trim().ifBlank { "midnight_pink" }.take(40),
                    "fontStyle" to entry.fontStyle.trim().ifBlank { "quicksand" }.take(40),
                    "textColorHex" to entry.textColorHex.trim().ifBlank { "#FFFFFF" }.take(12),
                    "textAlign" to safeAlign,
                    "fontSizeSp" to entry.fontSizeSp.coerceIn(12L, 28L),
                    "boldEnabled" to entry.boldEnabled,
                    "italicEnabled" to entry.italicEnabled,
                    "underlineEnabled" to entry.underlineEnabled,
                    "highlightEnabled" to entry.highlightEnabled,
                    "favorite" to entry.favorite,
                    "privateLocked" to entry.privateLocked,
                    "deleted" to false,
                    "deletedAtMillis" to 0L,
                    "wordCount" to words,
                    "charCount" to chars,
                    "reminderTimeMillis" to entry.reminderTimeMillis.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "title" to safeTitle,
                    "content" to safeContent,
                    "mood" to moodEnum.name,
                    "moodScore" to moodEnum.score.coerceIn(1L, 10L),
                    "categoryId" to entry.categoryId.trim().ifBlank { "personal" }.take(60),
                    "dateKey" to safeDateKey,
                    "entryTimeMillis" to entry.entryTimeMillis.coerceAtLeast(0L),
                    "tags" to safeTags,
                    "photoUris" to safePhotos,
                    "locationLabel" to entry.locationLabel.trim().take(120),
                    "stickerIds" to safeStickers,
                    "backgroundStyle" to entry.backgroundStyle.trim().ifBlank { "midnight_pink" }.take(40),
                    "fontStyle" to entry.fontStyle.trim().ifBlank { "quicksand" }.take(40),
                    "textColorHex" to entry.textColorHex.trim().ifBlank { "#FFFFFF" }.take(12),
                    "textAlign" to safeAlign,
                    "fontSizeSp" to entry.fontSizeSp.coerceIn(12L, 28L),
                    "boldEnabled" to entry.boldEnabled,
                    "italicEnabled" to entry.italicEnabled,
                    "underlineEnabled" to entry.underlineEnabled,
                    "highlightEnabled" to entry.highlightEnabled,
                    "favorite" to entry.favorite,
                    "privateLocked" to entry.privateLocked,
                    "wordCount" to words,
                    "charCount" to chars,
                    "reminderTimeMillis" to entry.reminderTimeMillis.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleEntryFavorite(userId: String, entryId: String, favorite: Boolean): Result<Unit> =
        updateSubdoc(userId, "entries", entryId, mapOf("favorite" to favorite, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun toggleEntryPrivate(userId: String, entryId: String, privateLocked: Boolean): Result<Unit> =
        updateSubdoc(userId, "entries", entryId, mapOf("privateLocked" to privateLocked, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun softDeleteEntry(userId: String, entryId: String): Result<Unit> =
        updateSubdoc(
            userId, "entries", entryId,
            mapOf("deleted" to true, "deletedAtMillis" to System.currentTimeMillis(), "updatedAt" to FieldValue.serverTimestamp())
        )

    suspend fun restoreEntry(userId: String, entryId: String): Result<Unit> =
        updateSubdoc(
            userId, "entries", entryId,
            mapOf("deleted" to false, "deletedAtMillis" to 0L, "updatedAt" to FieldValue.serverTimestamp())
        )

    suspend fun permanentlyDeleteEntry(userId: String, entryId: String): Result<Unit> =
        deleteSubdoc(userId, "entries", entryId)

    // --- MOOD LOGS ---

    suspend fun addMoodLog(userId: String, mood: DiaryMood, note: String, dateKey: String = todayDateKey()): Result<Unit> {
        val path = "users/$userId/moods"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid).collection("moods").document()
            val safeDateKey = if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(dateKey)) dateKey else todayDateKey()
            val payload = mapOf(
                "userId" to uid,
                "mood" to mood.name,
                "score" to mood.score.coerceIn(1L, 10L),
                "note" to note.trim().take(500),
                "dateKey" to safeDateKey,
                "loggedAtMillis" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMoodLog(userId: String, moodId: String): Result<Unit> =
        deleteSubdoc(userId, "moods", moodId)

    // --- DREAMS ---

    suspend fun saveDream(userId: String, dream: DreamEntry): Result<Unit> {
        val isCreate = dream.id.isBlank()
        var docPath = "users/$userId/dreams/${dream.id}"
        return try {
            val uid = requireUid(userId)
            val colRef = db.collection("users").document(uid).collection("dreams")
            val docRef = if (isCreate) colRef.document() else colRef.document(dream.id)
            docPath = docRef.path
            val safeTitle = dream.title.trim().ifBlank { "Starry Dream" }.take(150)
            val safeDateKey = if (Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(dream.dateKey)) dream.dateKey else todayDateKey()
            val catEnum = DreamCategory.fromRaw(dream.category)
            val moodEnum = DiaryMood.fromRaw(dream.mood)
            val safeTags = dream.tags.map { it.trim().take(30) }.filter { it.isNotEmpty() }.distinct().take(15)
            val safeImgs = dream.imageUris.map { it.trim().take(500) }.filter { it.isNotEmpty() }.take(10)

            if (isCreate) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to safeTitle,
                        "description" to dream.description.trim().take(20000),
                        "category" to catEnum.name,
                        "mood" to moodEnum.name,
                        "dateKey" to safeDateKey,
                        "tags" to safeTags,
                        "imageUris" to safeImgs,
                        "favorite" to dream.favorite,
                        "deleted" to false,
                        "deletedAtMillis" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "description" to dream.description.trim().take(20000),
                        "category" to catEnum.name,
                        "mood" to moodEnum.name,
                        "dateKey" to safeDateKey,
                        "tags" to safeTags,
                        "imageUris" to safeImgs,
                        "favorite" to dream.favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleDreamFavorite(userId: String, dreamId: String, favorite: Boolean): Result<Unit> =
        updateSubdoc(userId, "dreams", dreamId, mapOf("favorite" to favorite, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun softDeleteDream(userId: String, dreamId: String): Result<Unit> =
        updateSubdoc(userId, "dreams", dreamId, mapOf("deleted" to true, "deletedAtMillis" to System.currentTimeMillis(), "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun restoreDream(userId: String, dreamId: String): Result<Unit> =
        updateSubdoc(userId, "dreams", dreamId, mapOf("deleted" to false, "deletedAtMillis" to 0L, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun permanentlyDeleteDream(userId: String, dreamId: String): Result<Unit> =
        deleteSubdoc(userId, "dreams", dreamId)

    // --- VOICE NOTES ---

    suspend fun addVoiceNote(userId: String, title: String, description: String, filePath: String, durationSeconds: Long): Result<Unit> {
        val path = "users/$userId/voiceNotes"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid).collection("voiceNotes").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifBlank { "Voice Note" }.take(120),
                    "description" to description.trim().take(1000),
                    "filePath" to filePath.trim().take(500),
                    "durationSeconds" to durationSeconds.coerceIn(0L, 36000L),
                    "dateKey" to todayDateKey(),
                    "recordedAtMillis" to System.currentTimeMillis(),
                    "favorite" to false,
                    "deleted" to false,
                    "deletedAtMillis" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVoiceNote(userId: String, voiceId: String, title: String, description: String, favorite: Boolean): Result<Unit> =
        updateSubdoc(
            userId, "voiceNotes", voiceId,
            mapOf(
                "title" to title.trim().ifBlank { "Voice Note" }.take(120),
                "description" to description.trim().take(1000),
                "favorite" to favorite,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

    suspend fun softDeleteVoiceNote(userId: String, voiceId: String): Result<Unit> =
        updateSubdoc(userId, "voiceNotes", voiceId, mapOf("deleted" to true, "deletedAtMillis" to System.currentTimeMillis(), "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun restoreVoiceNote(userId: String, voiceId: String): Result<Unit> =
        updateSubdoc(userId, "voiceNotes", voiceId, mapOf("deleted" to false, "deletedAtMillis" to 0L, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun permanentlyDeleteVoiceNote(userId: String, voiceId: String): Result<Unit> =
        deleteSubdoc(userId, "voiceNotes", voiceId)

    // --- PHOTO & VIDEO MEMORIES ---

    suspend fun addMemory(
        userId: String,
        mediaUri: String,
        mediaType: String,
        albumName: String,
        caption: String,
        tags: List<String> = emptyList()
    ): Result<Unit> {
        val path = "users/$userId/memories"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid).collection("memories").document()
            val safeType = if (mediaType == "VIDEO") "VIDEO" else "PHOTO"
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "mediaUri" to mediaUri.trim().take(600),
                    "mediaType" to safeType,
                    "albumName" to albumName.trim().ifBlank { "My Memories" }.take(80),
                    "caption" to caption.trim().take(500),
                    "dateKey" to todayDateKey(),
                    "tags" to tags.map { it.trim().take(30) }.filter { it.isNotEmpty() }.take(15),
                    "favorite" to false,
                    "deleted" to false,
                    "deletedAtMillis" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemory(userId: String, memoryId: String, albumName: String, caption: String, tags: List<String>, favorite: Boolean): Result<Unit> =
        updateSubdoc(
            userId, "memories", memoryId,
            mapOf(
                "albumName" to albumName.trim().ifBlank { "My Memories" }.take(80),
                "caption" to caption.trim().take(500),
                "tags" to tags.map { it.trim().take(30) }.filter { it.isNotEmpty() }.take(15),
                "favorite" to favorite,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

    suspend fun toggleMemoryFavorite(userId: String, memoryId: String, favorite: Boolean): Result<Unit> =
        updateSubdoc(userId, "memories", memoryId, mapOf("favorite" to favorite, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun softDeleteMemory(userId: String, memoryId: String): Result<Unit> =
        updateSubdoc(userId, "memories", memoryId, mapOf("deleted" to true, "deletedAtMillis" to System.currentTimeMillis(), "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun restoreMemory(userId: String, memoryId: String): Result<Unit> =
        updateSubdoc(userId, "memories", memoryId, mapOf("deleted" to false, "deletedAtMillis" to 0L, "updatedAt" to FieldValue.serverTimestamp()))

    suspend fun permanentlyDeleteMemory(userId: String, memoryId: String): Result<Unit> =
        deleteSubdoc(userId, "memories", memoryId)

    // --- CUSTOM CATEGORIES ---

    suspend fun addCategory(userId: String, name: String, iconKey: String, colorHex: String, coverStyle: String): Result<Unit> {
        val path = "users/$userId/categories"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid).collection("categories").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to name.trim().ifBlank { "Custom Diary" }.take(60),
                    "iconKey" to iconKey.trim().ifBlank { "💗" }.take(40),
                    "colorHex" to colorHex.trim().ifBlank { "#FF42C8" }.take(10),
                    "coverStyle" to coverStyle.trim().ifBlank { "pink_glow" }.take(40),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> =
        deleteSubdoc(userId, "categories", categoryId)

    // --- BACKUP RECORDS ---

    suspend fun recordBackup(userId: String, label: String, format: String, entryCount: Long, dreamCount: Long, moodCount: Long): Result<Unit> {
        val path = "users/$userId/backups"
        return try {
            val uid = requireUid(userId)
            val docRef = db.collection("users").document(uid).collection("backups").document()
            val safeFmt = if (format in listOf("CLOUD", "JSON", "TXT", "PDF")) format else "CLOUD"
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "label" to label.trim().ifBlank { "Backup" }.take(120),
                    "format" to safeFmt,
                    "entryCount" to entryCount.coerceIn(0L, 100000L),
                    "dreamCount" to dreamCount.coerceIn(0L, 100000L),
                    "moodCount" to moodCount.coerceIn(0L, 100000L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- CLEAR ALL USER DATA ---

    suspend fun clearAllUserData(userId: String, deleteProfileDoc: Boolean = false): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUid(userId)
            val subcollections = listOf("entries", "moods", "dreams", "voiceNotes", "memories", "categories", "backups")
            for (sub in subcollections) {
                val docs = db.collection("users").document(uid).collection(sub).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            if (deleteProfileDoc) {
                db.collection("users").document(uid).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun updateSubdoc(userId: String, subcollection: String, docId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId/$subcollection/$docId"
        return try {
            val uid = requireUid(userId)
            require(docId.isNotBlank()) { "Document ID must not be blank" }
            db.collection("users").document(uid).collection(subcollection).document(docId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteSubdoc(userId: String, subcollection: String, docId: String): Result<Unit> {
        val path = "users/$userId/$subcollection/$docId"
        return try {
            val uid = requireUid(userId)
            require(docId.isNotBlank()) { "Document ID must not be blank" }
            db.collection("users").document(uid).collection(subcollection).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
