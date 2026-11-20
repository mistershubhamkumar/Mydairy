package com.example.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiDiaryAssistant"

enum class AiDiaryAction(val title: String, val subtitle: String) {
    DAILY_QUOTE("Daily Quote", "Warm aesthetic inspiration for today"),
    SUGGEST_TITLES("Suggest Titles", "3 poetic titles for your entry"),
    IMPROVE_WRITING("Improve Writing", "Polish flow while keeping your voice"),
    FIX_GRAMMAR("Grammar Check", "Gentle spelling and grammar polish"),
    SUMMARIZE("Summarize Entry", "A concise 2-3 sentence reflection"),
    MOOD_INSIGHT("Mood Insights", "Compassionate patterns from shared moods"),
    WRITING_PROMPTS("Writing Prompts", "3 thoughtful journaling questions"),
    AFFIRMATIONS("Positive Affirmations", "3 uplifting self-kindness affirmations")
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String, val action: AiDiaryAction) : AiUiState
    data class Success(val text: String, val action: AiDiaryAction) : AiUiState
    data class Error(val message: String) : AiUiState
}

/**
 * Gemini AI Diary Assistant powered by Firebase AI Logic (no API key required).
 * Privacy-first: ONLY invoked when the user explicitly opts in and shares selected text.
 */
class AiDiaryAssistantService {

    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun streamAction(
        action: AiDiaryAction,
        sharedInput: String
    ): Flow<AiUiState> = flow {
        emit(AiUiState.Loading)
        val prompt = buildPrompt(action, sharedInput)
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiUiState.Streaming(accumulated.toString(), action))
                    }
                }
            }
            val resultText = accumulated.toString().trim()
            if (resultText.isNotBlank()) {
                emit(AiUiState.Success(resultText, action))
            } else {
                emit(AiUiState.Error("The AI assistant returned an empty response. Please try again."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            emit(AiUiState.Error("Request timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            emit(AiUiState.Error(e.localizedMessage ?: "Unable to reach AI Diary Assistant right now."))
        }
    }

    private fun buildPrompt(action: AiDiaryAction, input: String): String {
        val safeInput = input.trim().take(4000)
        return when (action) {
            AiDiaryAction.DAILY_QUOTE ->
                "Write one short, heartwarming, poetic daily motivational quote for a personal diary app. Keep it under 24 words. No quotation marks around the whole response, no extra explanation."
            AiDiaryAction.SUGGEST_TITLES ->
                "Suggest 3 short, aesthetic, emotionally resonant diary entry titles (max 6 words each) as a simple numbered list based on this diary text:\n\n$safeInput"
            AiDiaryAction.IMPROVE_WRITING ->
                "Gently polish the clarity, flow, and warmth of this personal diary entry while preserving the writer's authentic voice and meaning. Return only the improved diary text:\n\n$safeInput"
            AiDiaryAction.FIX_GRAMMAR ->
                "Fix spelling, punctuation, and grammar in this personal diary entry without changing its tone or personal voice. Return only the corrected text:\n\n$safeInput"
            AiDiaryAction.SUMMARIZE ->
                "Summarize this personal diary entry warmly in 2-3 gentle sentences highlighting the key feelings and memories:\n\n$safeInput"
            AiDiaryAction.MOOD_INSIGHT ->
                "Based on this anonymized mood summary shared by the user, write 3 short, empathetic, encouraging wellness observations and one gentle self-care suggestion (keep under 120 words):\n\n$safeInput"
            AiDiaryAction.WRITING_PROMPTS ->
                "Suggest 3 gentle, inspiring personal diary writing prompts for tonight's reflection (mood context: ${safeInput.ifBlank { "general reflection" }}). Present as a simple numbered list."
            AiDiaryAction.AFFIRMATIONS ->
                "Write 3 warm, comforting, positive affirmations tailored to someone feeling ${safeInput.ifBlank { "reflective" }}. Present as a simple numbered list."
        }
    }
}
