package com.example.ui

import androidx.compose.runtime.Composable
import com.example.data.InTheMomentStyle
import com.example.data.Quote
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.example.data.VoiceTranscriptionState
import com.example.data.VoiceTranscriptionTarget

@Composable
fun SymposiumChatScreen(
    messages: List<ChatMessage>,
    savedQuotes: List<Quote> = emptyList(),
    attachedQuote: QuoteItem?,
    isGeneratingMomentQuote: Boolean = false,
    onSelectSavedQuote: (QuoteItem) -> Unit = {},
    onClearAttachedQuote: () -> Unit,
    onSendMessage: (String, QuoteItem?) -> Unit,
    onGenerateInTheMomentQuote: (InTheMomentStyle, List<String>, String, QuoteItem?) -> Unit = { _, _, _, _ -> },
    onSaveGeneratedQuote: (QuoteItem) -> Unit = {},
    onOpenDossier: (QuoteItem) -> Unit = {},
    audioState: QuoteAudioSceneState = QuoteAudioSceneState(),
    transcriptionState: VoiceTranscriptionState = VoiceTranscriptionState(),
    onReadAloud: (QuoteItem) -> Unit = {},
    onGenerate30sMusicScene: (QuoteItem) -> Unit = {},
    onStopAudio: () -> Unit = {},
    onToggleVoiceDictation: (VoiceTranscriptionTarget, (String) -> Unit) -> Unit = { _, _ -> }
) {
    PhilosophicalChatScreen(
        messages = messages,
        savedQuotes = savedQuotes,
        attachedQuote = attachedQuote,
        isGeneratingMomentQuote = isGeneratingMomentQuote,
        onSelectSavedQuote = onSelectSavedQuote,
        onClearAttachedQuote = onClearAttachedQuote,
        onSendMessage = onSendMessage,
        onGenerateInTheMomentQuote = onGenerateInTheMomentQuote,
        onSaveGeneratedQuote = onSaveGeneratedQuote,
        onOpenDossier = onOpenDossier,
        audioState = audioState,
        transcriptionState = transcriptionState,
        onReadAloud = onReadAloud,
        onGenerate30sMusicScene = onGenerate30sMusicScene,
        onStopAudio = onStopAudio,
        onToggleVoiceDictation = onToggleVoiceDictation
    )
}

