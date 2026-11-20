package com.example.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class DiaryMood(
    val label: String,
    val emoji: String,
    val score: Long,
    val colorHex: Long
) {
    HAPPY("Happy", "😊", 9L, 0xFFFFD166),
    EXCITED("Excited", "🤩", 10L, 0xFFFF5EBF),
    GRATEFUL("Grateful", "💖", 9L, 0xFFFF85C8),
    ROMANTIC("Romantic", "🥰", 9L, 0xFFFF42C8),
    MOTIVATED("Motivated", "🔥", 8L, 0xFFFF9F43),
    RELAXED("Relaxed", "😌", 8L, 0xFF72EFDD),
    LONELY("Lonely", "🥺", 4L, 0xFFA855F7),
    ANXIOUS("Anxious", "😰", 4L, 0xFF818CF8),
    SAD("Sad", "😢", 3L, 0xFF60A5FA),
    ANGRY("Angry", "😡", 2L, 0xFFFF595E);

    val color: Color get() = Color(colorHex)

    companion object {
        fun fromRaw(raw: String): DiaryMood =
            entries.firstOrNull { it.name == raw } ?: HAPPY
    }
}

enum class DreamCategory(val label: String, val emoji: String, val colorHex: Long) {
    BEAUTIFUL("Beautiful Dreams", "🌸", 0xFFFF85C8),
    NIGHTMARE("Nightmares", "🌩️", 0xFF818CF8),
    ROMANTIC("Romantic Dreams", "💘", 0xFFFF42C8),
    FANTASY("Fantasy Dreams", "🦄", 0xFFC084FC),
    WEIRD("Weird Dreams", "🌀", 0xFF38BDF8),
    LUCID("Lucid Dreams", "✨", 0xFFFDE047);

    val color: Color get() = Color(colorHex)

    companion object {
        fun fromRaw(raw: String): DreamCategory =
            entries.firstOrNull { it.name == raw } ?: BEAUTIFUL
    }
}

enum class EntrySortOrder(val label: String) {
    LATEST_FIRST("Latest First"),
    OLDEST_FIRST("Oldest First"),
    RECENTLY_UPDATED("Recently Updated")
}

data class BuiltInCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val coverStyle: String
)

object DefaultDiaryCategories {
    val builtIn: List<BuiltInCategory> = listOf(
        BuiltInCategory("personal", "My Personal Diary", "💗", "#FF42C8", "pink_glow"),
        BuiltInCategory("love", "Love Diary", "🥰", "#FF66B2", "rose_silk"),
        BuiltInCategory("secret", "Secret Diary", "🔐", "#A855F7", "midnight_violet"),
        BuiltInCategory("travel", "Travel Diary", "✈️", "#38BDF8", "sky_dream"),
        BuiltInCategory("family", "Family Memories", "🏡", "#F59E0B", "warm_sunset"),
        BuiltInCategory("friendship", "Friendship Diary", "🫶", "#EC4899", "candy_pink"),
        BuiltInCategory("daily", "Daily Life", "☀️", "#FACC15", "golden_hour"),
        BuiltInCategory("goals", "Goals & Dreams", "🌟", "#C084FC", "starry_purple"),
        BuiltInCategory("gratitude", "Gratitude Journal", "🙏", "#34D399", "mint_calm"),
        BuiltInCategory("study", "Study Diary", "📚", "#60A5FA", "pastel_blue"),
        BuiltInCategory("work", "Work Diary", "💼", "#818CF8", "lavender_pro")
    )

    val stickers: List<String> = listOf(
        "💗", "✨", "🌙", "🌸", "🦋", "👑", "🐱", "🎀", "☁️", "🍓", "🌈", "💫", "💌", "🧸", "🎶"
    )

    val entryBackgrounds: List<Pair<String, String>> = listOf(
        "midnight_pink" to "Midnight Pink",
        "lavender_glow" to "Lavender Glow",
        "starry_night" to "Starry Night",
        "rose_silk" to "Rose Silk",
        "pastel_clouds" to "Pastel Clouds",
        "moonlight_ink" to "Moonlight Ink"
    )

    val cuteCatMessages: List<String> = listOf(
        "Every day is special... ♡",
        "Your feelings matter so much! 💗",
        "Take a deep breath, Princess ✨",
        "I'll guard your secrets safely! 🐾",
        "Write one small happy moment today 🌸",
        "You are doing wonderfully! 👑",
        "Dream big, live softly... 🌙"
    )

    val dailyMotivations: List<String> = listOf(
        "Every page you write is a love letter to your future self. 💗",
        "Small gentle steps still move you closer to your brightest dreams. ✨",
        "Your heart deserves a quiet, safe place to breathe and heal. 🌙",
        "Even cloudy days hold soft moments worth remembering forever. 🌸",
        "You have survived 100% of your hardest days — shine gently today! 👑"
    )
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Princess",
    val email: String = "",
    val bio: String = "Write • Feel • Heal • Remember Forever",
    val avatarKey: String = "princess",
    val selectedThemeId: String = "pink_princess",
    val selectedFontId: String = "quicksand",
    val mascotId: String = "white_cat",
    val animationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DiaryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = DiaryMood.HAPPY.name,
    val moodScore: Long = 9L,
    val categoryId: String = "personal",
    val dateKey: String = todayDateKey(),
    val entryTimeMillis: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val locationLabel: String = "",
    val stickerIds: List<String> = emptyList(),
    val backgroundStyle: String = "midnight_pink",
    val fontStyle: String = "quicksand",
    val textColorHex: String = "#FFFFFF",
    val textAlign: String = "START",
    val fontSizeSp: Long = 16L,
    val boldEnabled: Boolean = false,
    val italicEnabled: Boolean = false,
    val underlineEnabled: Boolean = false,
    val highlightEnabled: Boolean = false,
    val favorite: Boolean = false,
    val privateLocked: Boolean = false,
    val deleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    val wordCount: Long = 0L,
    val charCount: Long = 0L,
    val reminderTimeMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val moodEnum: DiaryMood get() = DiaryMood.fromRaw(mood)
}

@Keep
@IgnoreExtraProperties
data class MoodLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mood: String = DiaryMood.HAPPY.name,
    val score: Long = 9L,
    val note: String = "",
    val dateKey: String = todayDateKey(),
    val loggedAtMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val moodEnum: DiaryMood get() = DiaryMood.fromRaw(mood)
}

@Keep
@IgnoreExtraProperties
data class DreamEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = DreamCategory.BEAUTIFUL.name,
    val mood: String = DiaryMood.HAPPY.name,
    val dateKey: String = todayDateKey(),
    val tags: List<String> = emptyList(),
    val imageUris: List<String> = emptyList(),
    val favorite: Boolean = false,
    val deleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: DreamCategory get() = DreamCategory.fromRaw(category)
    val moodEnum: DiaryMood get() = DiaryMood.fromRaw(mood)
}

@Keep
@IgnoreExtraProperties
data class VoiceNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val filePath: String = "",
    val durationSeconds: Long = 0L,
    val dateKey: String = todayDateKey(),
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val deleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PhotoMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mediaUri: String = "",
    val mediaType: String = "PHOTO",
    val albumName: String = "My Memories",
    val caption: String = "",
    val dateKey: String = todayDateKey(),
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val deleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DiaryCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "💗",
    val colorHex: String = "#FF42C8",
    val coverStyle: String = "pink_glow",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BackupRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val format: String = "CLOUD",
    val entryCount: Long = 0L,
    val dreamCount: Long = 0L,
    val moodCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun todayDateKey(timeMillis: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date(timeMillis))
}

fun formatPrettyDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatPrettyDateTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatTimeOnly(timeMillis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun dateKeyToPretty(dateKey: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(dateKey) ?: return dateKey
        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        dateKey
    }
}

fun computeStreaks(dateKeys: Set<String>): Pair<Int, Int> {
    if (dateKeys.isEmpty()) return 0 to 0
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sortedDays = dateKeys.mapNotNull { key ->
        try {
            val d = parser.parse(key) ?: return@mapNotNull null
            val cal = Calendar.getInstance().apply {
                time = d
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis / 86_400_000L
        } catch (_: Exception) {
            null
        }
    }.distinct().sorted()

    if (sortedDays.isEmpty()) return 0 to 0

    var longest = 1
    var run = 1
    for (i in 1 until sortedDays.size) {
        if (sortedDays[i] - sortedDays[i - 1] == 1L) {
            run++
            if (run > longest) longest = run
        } else {
            run = 1
        }
    }

    val todayEpochDay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 86_400_000L

    val lastDay = sortedDays.last()
    val current = if (todayEpochDay - lastDay <= 1L) {
        var c = 1
        var idx = sortedDays.lastIndex
        while (idx > 0 && sortedDays[idx] - sortedDays[idx - 1] == 1L) {
            c++
            idx--
        }
        c
    } else {
        0
    }
    return current to longest
}
