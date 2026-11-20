package com.example.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val TAG = "MediaAndBackupManager"

class MediaAndBackupManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    // --- ZERO-PERMISSION PHOTO PICKER LOCAL COPY ---

    fun persistPickedMedia(sourceUri: Uri, isVideo: Boolean): String? {
        return try {
            val dir = File(context.filesDir, "memories").apply { mkdirs() }
            val ext = if (isVideo) "mp4" else "jpg"
            val outFile = File(dir, "mem_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(outFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist picked media", e)
            null
        }
    }

    fun computeLocalStorageBytes(): Long {
        val memDir = File(context.filesDir, "memories")
        val voiceDir = File(context.filesDir, "voice_notes")
        val memBytes = memDir.listFiles()?.sumOf { it.length() } ?: 0L
        val voiceBytes = voiceDir.listFiles()?.sumOf { it.length() } ?: 0L
        return memBytes + voiceBytes
    }

    // --- VOICE RECORDER ---

    fun startRecording(): String? {
        stopRecordingCleanup()
        return try {
            val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentRecordingFile = outFile
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            stopRecordingCleanup()
            null
        }
    }

    fun pauseRecording(): Boolean {
        return try {
            mediaRecorder?.pause()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
            false
        }
    }

    fun resumeRecording(): Boolean {
        return try {
            mediaRecorder?.resume()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
            false
        }
    }

    fun stopRecording(): String? {
        val path = currentRecordingFile?.absolutePath
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            path
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            stopRecordingCleanup()
            null
        }
    }

    fun cancelRecording() {
        val file = currentRecordingFile
        stopRecordingCleanup()
        try {
            file?.delete()
        } catch (_: Exception) {
        }
    }

    private fun stopRecordingCleanup() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        currentRecordingFile = null
    }

    // --- VOICE PLAYBACK ---

    fun playAudio(filePath: String, speed: Float, onCompletion: () -> Unit): Boolean {
        stopPlayback()
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                try {
                    playbackParams = playbackParams.setSpeed(speed.coerceIn(0.5f, 2.0f))
                } catch (_: Exception) {
                }
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                start()
            }
            mediaPlayer = player
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice note", e)
            stopPlayback()
            false
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.playbackParams = player.playbackParams.setSpeed(speed.coerceIn(0.5f, 2.0f))
                }
            }
        } catch (_: Exception) {
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    // --- BACKUP & EXPORT (JSON / TXT / PDF) ---

    fun buildExportJson(
        entries: List<DiaryEntry>,
        dreams: List<DreamEntry>,
        moods: List<MoodLog>
    ): String {
        val root = JSONObject()
        root.put("app", "My Diary - Secret Feelings")
        root.put("exportedAtMillis", System.currentTimeMillis())

        val entriesArr = JSONArray()
        entries.forEach { e ->
            entriesArr.put(
                JSONObject().apply {
                    put("title", e.title)
                    put("content", e.content)
                    put("mood", e.mood)
                    put("moodScore", e.moodScore)
                    put("categoryId", e.categoryId)
                    put("dateKey", e.dateKey)
                    put("entryTimeMillis", e.entryTimeMillis)
                    put("tags", JSONArray(e.tags))
                    put("locationLabel", e.locationLabel)
                    put("stickerIds", JSONArray(e.stickerIds))
                    put("favorite", e.favorite)
                    put("privateLocked", e.privateLocked)
                }
            )
        }
        root.put("entries", entriesArr)

        val dreamsArr = JSONArray()
        dreams.forEach { d ->
            dreamsArr.put(
                JSONObject().apply {
                    put("title", d.title)
                    put("description", d.description)
                    put("category", d.category)
                    put("mood", d.mood)
                    put("dateKey", d.dateKey)
                    put("tags", JSONArray(d.tags))
                    put("favorite", d.favorite)
                }
            )
        }
        root.put("dreams", dreamsArr)

        val moodsArr = JSONArray()
        moods.forEach { m ->
            moodsArr.put(
                JSONObject().apply {
                    put("mood", m.mood)
                    put("score", m.score)
                    put("note", m.note)
                    put("dateKey", m.dateKey)
                    put("loggedAtMillis", m.loggedAtMillis)
                }
            )
        }
        root.put("moods", moodsArr)
        return root.toString(2)
    }

    fun buildExportTxt(entries: List<DiaryEntry>, dreams: List<DreamEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("==============================================")
        sb.appendLine("       MY DIARY - SECRET FEELINGS")
        sb.appendLine("    Write • Feel • Heal • Remember Forever")
        sb.appendLine("==============================================")
        sb.appendLine()
        entries.forEach { e ->
            sb.appendLine("----------------------------------------------")
            sb.appendLine("Title: ${e.title.ifBlank { "Untitled Entry" }}")
            sb.appendLine("Date: ${dateKeyToPretty(e.dateKey)} (${formatTimeOnly(e.entryTimeMillis)})")
            sb.appendLine("Mood: ${e.moodEnum.emoji} ${e.moodEnum.label}")
            if (e.locationLabel.isNotBlank()) sb.appendLine("Location: ${e.locationLabel}")
            if (e.tags.isNotEmpty()) sb.appendLine("Tags: ${e.tags.joinToString(", ") { "#$it" }}")
            sb.appendLine()
            sb.appendLine(e.content)
            sb.appendLine()
        }
        if (dreams.isNotEmpty()) {
            sb.appendLine("============== DREAM JOURNAL ==============")
            dreams.forEach { d ->
                sb.appendLine("Dream: ${d.title} (${d.categoryEnum.label} • ${dateKeyToPretty(d.dateKey)})")
                sb.appendLine(d.description)
                sb.appendLine()
            }
        }
        return sb.toString()
    }

    fun writeTextToUri(uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write export file", e)
            false
        }
    }

    fun writePdfToUri(uri: Uri, entries: List<DiaryEntry>, dreams: List<DreamEntry>): Boolean {
        return try {
            val pdf = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(168, 85, 247)
                textSize = 20f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(255, 66, 200)
                textSize = 14f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
            }

            var y = 54f
            canvas.drawText("My Diary - Secret Feelings", 40f, y, titlePaint)
            y += 22f
            canvas.drawText("Exported ${entries.size} diary entries & ${dreams.size} dreams", 40f, y, bodyPaint)
            y += 28f

            fun ensureSpace(needed: Float) {
                if (y + needed > pageHeight - 48f) {
                    pdf.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdf.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
            }

            entries.forEach { entry ->
                ensureSpace(64f)
                val title = entry.title.ifBlank { "Untitled Entry" }.take(60)
                canvas.drawText("$title  [${entry.moodEnum.label}]", 40f, y, headerPaint)
                y += 16f
                canvas.drawText("${dateKeyToPretty(entry.dateKey)} • ${entry.wordCount} words", 40f, y, bodyPaint)
                y += 16f

                val lines = entry.content.chunked(78)
                lines.forEach { line ->
                    ensureSpace(16f)
                    canvas.drawText(line.replace("\n", " "), 40f, y, bodyPaint)
                    y += 15f
                }
                y += 14f
            }

            pdf.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.writeTo(out)
            }
            pdf.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate PDF export", e)
            false
        }
    }

    fun parseImportJson(uri: Uri): Pair<List<DiaryEntry>, List<DreamEntry>>? {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return null
            val root = JSONObject(raw)
            val entries = mutableListOf<DiaryEntry>()
            val entriesArr = root.optJSONArray("entries")
            if (entriesArr != null) {
                for (i in 0 until entriesArr.length()) {
                    val o = entriesArr.getJSONObject(i)
                    val tagsArr = o.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (t in 0 until tagsArr.length()) tags.add(tagsArr.optString(t))
                    }
                    val moodEnum = DiaryMood.fromRaw(o.optString("mood", DiaryMood.HAPPY.name))
                    entries.add(
                        DiaryEntry(
                            title = o.optString("title", ""),
                            content = o.optString("content", ""),
                            mood = moodEnum.name,
                            moodScore = o.optLong("moodScore", moodEnum.score),
                            categoryId = o.optString("categoryId", "personal"),
                            dateKey = o.optString("dateKey", todayDateKey()),
                            entryTimeMillis = o.optLong("entryTimeMillis", System.currentTimeMillis()),
                            tags = tags,
                            locationLabel = o.optString("locationLabel", ""),
                            favorite = o.optBoolean("favorite", false),
                            privateLocked = o.optBoolean("privateLocked", false)
                        )
                    )
                }
            }
            val dreams = mutableListOf<DreamEntry>()
            val dreamsArr = root.optJSONArray("dreams")
            if (dreamsArr != null) {
                for (i in 0 until dreamsArr.length()) {
                    val o = dreamsArr.getJSONObject(i)
                    dreams.add(
                        DreamEntry(
                            title = o.optString("title", "Imported Dream"),
                            description = o.optString("description", ""),
                            category = DreamCategory.fromRaw(o.optString("category", DreamCategory.BEAUTIFUL.name)).name,
                            mood = DiaryMood.fromRaw(o.optString("mood", DiaryMood.HAPPY.name)).name,
                            dateKey = o.optString("dateKey", todayDateKey()),
                            favorite = o.optBoolean("favorite", false)
                        )
                    )
                }
            }
            entries to dreams
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse import JSON", e)
            null
        }
    }
}
