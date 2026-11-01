package com.example.network

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.QuoteAudioSceneState
import com.example.data.QuoteItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "QuoteAudioVoiceSvc"

/**
 * QuoteAudioAndVoiceService provides:
 * 1. Fast 30-second ambient Quote Music Scene generation using `lyria-3-clip-preview`.
 * 2. Expressive Quote Read Aloud narration using `gemini-3.1-flash-tts-preview` and Android `TextToSpeech`.
 * 3. Microphone voice capture & speech-to-text transcription using `gemini-3.5-transcribe`.
 */
class QuoteAudioAndVoiceService(
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val LYRIA_CLIP_MODEL = "lyria-3-clip-preview"
        const val GEMINI_TTS_MODEL = "gemini-3.1-flash-tts-preview"
        const val GEMINI_TRANSCRIBE_MODEL = "gemini-3.5-transcribe"
        const val CLIP_DURATION_SECONDS = 30
        private const val SAMPLE_RATE_HZ = 16_000

        data class MusicalMoodProfile(
            val title: String,
            val keySignature: String,
            val tempoBpm: Int,
            val rootFreqHz: Double,
            val chordRatios: List<Double>,
            val instruments: List<String>,
            val description: String
        ) {
            val caption: String
                get() = description
            val rootHz: Double
                get() = rootFreqHz
        }

        fun deriveMusicalMoodProfile(quote: QuoteItem): MusicalMoodProfile {
            val combinedTheme = (listOf(quote.category) + quote.tags + listOf(quote.text)).joinToString(" ").lowercase()
            return when {
                combinedTheme.contains("humor") || combinedTheme.contains("wit") || combinedTheme.contains("comedy") -> MusicalMoodProfile(
                    title = "Salon Scherzo & Continuo",
                    keySignature = "G Mixolydian",
                    tempoBpm = 84,
                    rootFreqHz = 196.00, // G3
                    chordRatios = listOf(1.0, 1.25, 1.5, 1.7778),
                    instruments = listOf("Pizzicato Strings", "Harpsichord Continuo", "Woodwind Flourish"),
                    description = "Playful Mixolydian chamber scherzo matching the witty cadence of ${quote.author} (${quote.sourceWork})."
                )
                combinedTheme.contains("stoic") || combinedTheme.contains("philosophy") || combinedTheme.contains("wisdom") -> MusicalMoodProfile(
                    title = "Stoic Colonnade Nocturne",
                    keySignature = "D Dorian",
                    tempoBpm = 66,
                    rootFreqHz = 146.83, // D3
                    chordRatios = listOf(1.0, 1.1892, 1.4983, 1.6818), // D - F - A - B
                    instruments = listOf("Warm Cello Drone", "Classical Lute", "Parchment Chimes"),
                    description = "Contemplative Dorian strings and resonant chamber harmonics inspired by ${quote.author} (${quote.era})."
                )
                combinedTheme.contains("poetry") || combinedTheme.contains("literature") || combinedTheme.contains("art") -> MusicalMoodProfile(
                    title = "Lamplight Folio Prelude",
                    keySignature = "A Aeolian",
                    tempoBpm = 72,
                    rootFreqHz = 220.0, // A3
                    chordRatios = listOf(1.0, 1.2, 1.5, 1.8),
                    instruments = listOf("Intimate Felt Piano", "Chamber Harp", "Viola Harmonics"),
                    description = "Lyrical felt piano and gentle harp arpeggios evoking “${quote.sourceWork}” by ${quote.author}."
                )
                combinedTheme.contains("science") || combinedTheme.contains("cosmos") || combinedTheme.contains("nature") -> MusicalMoodProfile(
                    title = "Celestial Observatory Pulse",
                    keySignature = "F Lydian",
                    tempoBpm = 78,
                    rootFreqHz = 174.61, // F3
                    chordRatios = listOf(1.0, 1.25, 1.5, 1.875),
                    instruments = listOf("Glass Marimba", "Analog Cosmic Pad", "Bowed Vibraphone"),
                    description = "Luminous Lydian overtones and crystalline pulses reflecting ${quote.author}'s inquiry."
                )
                else -> MusicalMoodProfile(
                    title = "Archival Sanctuary Ambience",
                    keySignature = "C Major Pentatonic",
                    tempoBpm = 70,
                    rootFreqHz = 196.00, // G3
                    chordRatios = listOf(1.0, 1.125, 1.25, 1.5),
                    instruments = listOf("Acoustic Guitar", "Warm Strings", "Soft Reed Organ"),
                    description = "Warm editorial chamber motifs tailored to ${quote.author}'s reflection."
                )
            }
        }

        fun buildMusicScenePrompt(quote: QuoteItem): String {
            val profile = deriveMusicalMoodProfile(quote)
            val themes = (listOf(quote.category) + quote.tags).distinct().joinToString(", ")
            return buildString {
                append("Generate a 30-second ambient instrumental soundscape (${profile.tempoBpm} BPM, ${profile.keySignature}) ")
                append("using $LYRIA_CLIP_MODEL that matches the mood and theme ($themes) of this quote: ")
                append("\"${quote.text}\" by ${quote.author} (from ${quote.sourceWork}, ${quote.era}). ")
                append("Instrumentation: ${profile.instruments.joinToString(", ")}. ")
                append("Scene profile: ${profile.title} — ${profile.description}")
            }
        }

        fun buildExpressiveLiteraryTtsPrompt(quote: QuoteItem, personalNote: String = ""): String {
            val themes = (listOf(quote.category) + quote.tags).distinct().joinToString(", ")
            return buildString {
                append("Narrate the following saved literary quote aloud with an expressive, warm, literary tone ")
                append("and natural poetic cadence suited to its theme ($themes) and era (${quote.era}): ")
                append("“${quote.text}” — by ${quote.author}, from ${quote.sourceWork}.")
                if (personalNote.isNotBlank()) {
                    append(" Reader's archival note: $personalNote.")
                }
            }
        }

        fun toQuoteItem(savedQuote: com.example.data.Quote): QuoteItem {
            return QuoteItem(
                id = savedQuote.id.ifBlank { "saved_${savedQuote.author.hashCode()}" },
                text = savedQuote.text,
                author = savedQuote.author,
                sourceWork = savedQuote.sourceWork.ifBlank { "Saved Commonplace Archive" },
                category = savedQuote.category.ifBlank { "Philosophy" },
                tags = savedQuote.tags,
                onlinePopularityScore = savedQuote.onlinePopularityScore.toInt(),
                popularOnlineContext = savedQuote.personalNote.ifBlank {
                    "Captured at ${savedQuote.timestampInfo.location}"
                }
            )
        }

        /**
         * Wraps raw 16-bit little-endian mono PCM audio samples into a standard 44-byte RIFF WAV header
         * (`audio/wav`) suitable for `gemini-3.5-transcribe`.
         */
        fun encodePcmToWavBytes(pcmData: ByteArray, sampleRate: Int = SAMPLE_RATE_HZ): ByteArray {
            val channels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val blockAlign = (channels * bitsPerSample / 8).toShort()
            val totalDataLen = pcmData.size + 36

            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(totalDataLen)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // PCM chunk size
                putShort(1.toShort()) // AudioFormat = 1 (PCM)
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort(blockAlign)
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(pcmData.size)
            }.array()

            return header + pcmData
        }

        fun encodePcmToWavBytes(pcmSamples: ShortArray, sampleRate: Int = SAMPLE_RATE_HZ): ByteArray {
            val byteBuffer = ByteBuffer.allocate(pcmSamples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmSamples) {
                byteBuffer.putShort(s)
            }
            return encodePcmToWavBytes(byteBuffer.array(), sampleRate)
        }
    }

    // Lazily initialized Firebase AI Logic models
    private val lyriaClipModel by lazy {
        Firebase.ai.generativeModel(modelName = LYRIA_CLIP_MODEL)
    }

    private val ttsModel by lazy {
        Firebase.ai.generativeModel(modelName = GEMINI_TTS_MODEL)
    }

    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(modelName = GEMINI_TRANSCRIBE_MODEL)
    }

    private var ttsEngine: TextToSpeech? = null
    private var isTtsReady = false
    private var activePlaybackJob: Job? = null
    private var activeAudioTrack: AudioTrack? = null
    private var activeMediaPlayer: MediaPlayer? = null

    @Volatile
    private var isRecordingAudio = false
    private var recordedPcmStream: ByteArrayOutputStream? = null
    private var recordingJob: Job? = null

    init {
        val appCtx = context?.applicationContext
        if (appCtx != null) {
            runCatching {
                ttsEngine = TextToSpeech(appCtx) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsEngine?.language = Locale.US
                        ttsEngine?.setSpeechRate(0.94f)
                        ttsEngine?.setPitch(0.98f)
                        isTtsReady = true
                    }
                }
            }
        }
    }

    /**
     * Generates and plays a fast 30-second ambient music scene for [quote] using `lyria-3-clip-preview`.
     */
    fun generateAndPlay30sMusicScene(
        scope: CoroutineScope,
        quote: QuoteItem,
        onStateUpdate: (QuoteAudioSceneState) -> Unit
    ) {
        stopAllAudio()
        val profile = deriveMusicalMoodProfile(quote)
        val prompt = buildMusicScenePrompt(quote)

        activePlaybackJob = scope.launch {
            onStateUpdate(
                QuoteAudioSceneState(
                    activeQuoteId = quote.id,
                    activeQuoteAuthor = quote.author,
                    isGeneratingMusic = true,
                    isPlayingMusic = false,
                    musicModelId = LYRIA_CLIP_MODEL,
                    clipDurationSeconds = CLIP_DURATION_SECONDS,
                    musicSceneTitle = profile.title,
                    musicSceneDescription = profile.description,
                    instrumentationTags = profile.instruments,
                    tempoBpm = profile.tempoBpm,
                    keySignature = profile.keySignature,
                    statusCaption = "Composing 30s ambient clip with $LYRIA_CLIP_MODEL…"
                )
            )

            var modelNotes = profile.description
            var inlineAudioBytes: ByteArray? = null

            try {
                val response = withContext(ioDispatcher) {
                    withTimeout(60_000L) {
                        lyriaClipModel.generateContent(prompt)
                    }
                }
                val candidateParts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
                val audioPart = candidateParts.filterIsInstance<InlineDataPart>().firstOrNull {
                    it.mimeType.startsWith("audio/")
                }
                if (audioPart != null && audioPart.inlineData.isNotEmpty()) {
                    inlineAudioBytes = audioPart.inlineData
                }
                val responseText = response.text?.trim().orEmpty()
                if (responseText.isNotBlank()) {
                    modelNotes = responseText.take(220)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Lyria model note/audio synthesis fallback active: ${e.message}")
            }

            onStateUpdate(
                QuoteAudioSceneState(
                    activeQuoteId = quote.id,
                    activeQuoteAuthor = quote.author,
                    isGeneratingMusic = false,
                    isPlayingMusic = true,
                    musicModelId = LYRIA_CLIP_MODEL,
                    clipDurationSeconds = CLIP_DURATION_SECONDS,
                    elapsedSeconds = 0,
                    musicSceneTitle = profile.title,
                    musicSceneDescription = modelNotes,
                    instrumentationTags = profile.instruments,
                    tempoBpm = profile.tempoBpm,
                    keySignature = profile.keySignature,
                    statusCaption = "♫ Playing 30s ${profile.title} (${profile.instruments.joinToString(" • ")})"
                )
            )

            // Play either returned audio bytes or our warm 30s polyphonic chamber synthesis
            val audioPlaybackJob = launch(ioDispatcher) {
                val bytes = inlineAudioBytes
                if (bytes != null && context != null) {
                    playInlineAudioBytes(bytes)
                } else {
                    playPolyphonicAmbientClip(profile, durationSeconds = CLIP_DURATION_SECONDS)
                }
            }

            for (sec in 1..CLIP_DURATION_SECONDS) {
                if (!isActive) break
                delay(1_000L)
                onStateUpdate(
                    QuoteAudioSceneState(
                        activeQuoteId = quote.id,
                        activeQuoteAuthor = quote.author,
                        isGeneratingMusic = false,
                        isPlayingMusic = sec < CLIP_DURATION_SECONDS,
                        musicModelId = LYRIA_CLIP_MODEL,
                        clipDurationSeconds = CLIP_DURATION_SECONDS,
                        elapsedSeconds = sec,
                        musicSceneTitle = profile.title,
                        musicSceneDescription = modelNotes,
                        instrumentationTags = profile.instruments,
                        tempoBpm = profile.tempoBpm,
                        keySignature = profile.keySignature,
                        statusCaption = if (sec < CLIP_DURATION_SECONDS) {
                            "♫ Playing 30s ${profile.title} (${sec}s / ${CLIP_DURATION_SECONDS}s)"
                        } else {
                            "Completed 30s music scene • ${profile.title}"
                        }
                    )
                )
            }
            audioPlaybackJob.cancel()
        }
    }

    /**
     * Reads the quote aloud using `gemini-3.1-flash-tts-preview` with an expressive, literary tone,
     * and automatically falls back to the system Android `TextToSpeech` engine if inline TTS audio
     * is unavailable or offline.
     */
    fun readQuoteAloud(
        scope: CoroutineScope,
        quote: QuoteItem,
        onStateUpdate: (QuoteAudioSceneState) -> Unit
    ) {
        stopAllAudio()
        val profile = deriveMusicalMoodProfile(quote)
        val spokenScript = "“${quote.text}” — by ${quote.author}, from ${quote.sourceWork}."
        val expressivePrompt = buildExpressiveLiteraryTtsPrompt(quote)

        activePlaybackJob = scope.launch {
            onStateUpdate(
                QuoteAudioSceneState(
                    activeQuoteId = quote.id,
                    activeQuoteAuthor = quote.author,
                    isReadingAloud = true,
                    ttsModelId = GEMINI_TTS_MODEL,
                    musicSceneTitle = "Expressive Literary Read Aloud • ${quote.author}",
                    musicSceneDescription = spokenScript,
                    instrumentationTags = listOf("Expressive Literary Tone", GEMINI_TTS_MODEL, quote.era),
                    statusCaption = "Synthesizing expressive literary narration ($GEMINI_TTS_MODEL)…"
                )
            )

            var playedGeminiInlineAudio = false

            // 1. Primary: Request expressive literary narration from gemini-3.1-flash-tts-preview
            try {
                val response = withContext(ioDispatcher) {
                    withTimeout(30_000L) {
                        ttsModel.generateContent(expressivePrompt)
                    }
                }
                val audioPart = response.candidates.firstOrNull()?.content?.parts
                    .orEmpty()
                    .filterIsInstance<InlineDataPart>()
                    .firstOrNull { it.mimeType.startsWith("audio/") }
                if (audioPart != null && audioPart.inlineData.isNotEmpty() && context != null) {
                    withContext(ioDispatcher) {
                        playInlineAudioBytes(audioPart.inlineData)
                    }
                    playedGeminiInlineAudio = true
                    onStateUpdate(
                        QuoteAudioSceneState(
                            activeQuoteId = quote.id,
                            activeQuoteAuthor = quote.author,
                            isReadingAloud = true,
                            ttsModelId = GEMINI_TTS_MODEL,
                            musicSceneTitle = "Gemini 3.1 Flash TTS • ${quote.author}",
                            musicSceneDescription = spokenScript,
                            instrumentationTags = listOf("Gemini 3.1 Flash TTS", "Literary Tone", quote.era),
                            statusCaption = "Narrating aloud with $GEMINI_TTS_MODEL: “${quote.text.take(64)}…”"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Gemini 3.1 Flash TTS inline audio unavailable, falling back to Android System TTS (${e.message})")
            }

            // 2. Fallback: Use the system Android TextToSpeech engine with expressive literary cadence
            if (!playedGeminiInlineAudio) {
                speakWithAndroidSystemTtsFallback(spokenScript, quote.id)
                onStateUpdate(
                    QuoteAudioSceneState(
                        activeQuoteId = quote.id,
                        activeQuoteAuthor = quote.author,
                        isReadingAloud = true,
                        ttsModelId = "$GEMINI_TTS_MODEL (Android TTS Fallback)",
                        musicSceneTitle = "Expressive Read Aloud • ${quote.author}",
                        musicSceneDescription = spokenScript,
                        instrumentationTags = listOf("Expressive Narrator", "System TTS Fallback", quote.era),
                        statusCaption = "Narrating aloud: “${quote.text.take(68)}…”"
                    )
                )
            }

            val wordCount = spokenScript.split(" ").size
            val estimatedDurationSeconds = (wordCount / 2.4).toInt().coerceIn(4, 18)
            for (sec in 1..estimatedDurationSeconds) {
                if (!isActive) break
                delay(1_000L)
            }

            onStateUpdate(
                QuoteAudioSceneState(
                    activeQuoteId = quote.id,
                    activeQuoteAuthor = quote.author,
                    isReadingAloud = false,
                    ttsModelId = GEMINI_TTS_MODEL,
                    musicSceneTitle = profile.title,
                    musicSceneDescription = spokenScript,
                    statusCaption = "Finished reading aloud • ${quote.author}"
                )
            )
        }
    }

    fun readSavedQuoteAloud(
        scope: CoroutineScope,
        savedQuote: com.example.data.Quote,
        onStateUpdate: (QuoteAudioSceneState) -> Unit
    ) {
        readQuoteAloud(scope, toQuoteItem(savedQuote), onStateUpdate)
    }

    fun generateAndPlay30sMusicSceneForSavedQuote(
        scope: CoroutineScope,
        savedQuote: com.example.data.Quote,
        onStateUpdate: (QuoteAudioSceneState) -> Unit
    ) {
        generateAndPlay30sMusicScene(scope, toQuoteItem(savedQuote), onStateUpdate)
    }

    private fun speakWithAndroidSystemTtsFallback(spokenScript: String, quoteId: String) {
        runCatching {
            if (isTtsReady) {
                ttsEngine?.setSpeechRate(0.92f)
                ttsEngine?.setPitch(0.97f)
                ttsEngine?.speak(
                    spokenScript,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "quote_tts_$quoteId"
                )
            }
        }
    }

    fun stopAllAudio() {
        activePlaybackJob?.cancel()
        activePlaybackJob = null
        runCatching { ttsEngine?.stop() }
        runCatching {
            activeAudioTrack?.pause()
            activeAudioTrack?.flush()
            activeAudioTrack?.release()
        }
        activeAudioTrack = null
        runCatching {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
        }
        activeMediaPlayer = null
    }

    private fun playInlineAudioBytes(audioBytes: ByteArray) {
        val ctx = context ?: return
        runCatching {
            val tempFile = File.createTempFile("commonplace_audio_", ".wav", ctx.cacheDir)
            tempFile.writeBytes(audioBytes)
            val mp = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }
            activeMediaPlayer = mp
        }
    }

    private fun playPolyphonicAmbientClip(profile: MusicalMoodProfile, durationSeconds: Int) {
        runCatching {
            val sampleRate = 22_050
            val bufferSamples = sampleRate // 1-second streaming chunks
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferSamples * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeAudioTrack = track
            track.play()

            val chunkShort = ShortArray(bufferSamples)
            val root = profile.rootFreqHz
            val ratios = profile.chordRatios

            for (sec in 0 until durationSeconds) {
                val arpFreq = root * ratios[sec % ratios.size] * 2.0
                val fifthFreq = root * 1.4983
                for (i in 0 until bufferSamples) {
                    val t = (sec * sampleRate + i).toDouble() / sampleRate
                    val env = sin(PI * (i.toDouble() / bufferSamples)).coerceIn(0.15, 1.0)
                    val droneWave = sin(2.0 * PI * root * t) * 0.35 +
                        sin(2.0 * PI * fifthFreq * t) * 0.22
                    val arpWave = sin(2.0 * PI * arpFreq * t) * 0.25 * env
                    val sample = ((droneWave + arpWave) * 5200.0).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                    chunkShort[i] = sample
                }
                track.write(chunkShort, 0, chunkShort.size)
            }

            track.stop()
            track.release()
        }
    }

    // =========================================================================
    // Microphone Audio Recording & Transcription (gemini-3.5-transcribe)
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun startMicrophoneRecording(scope: CoroutineScope): Boolean {
        if (isRecordingAudio) return true
        val outStream = ByteArrayOutputStream()
        recordedPcmStream = outStream
        isRecordingAudio = true

        recordingJob = scope.launch(ioDispatcher) {
            runCatching {
                val minBuffer = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(3200)

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer
                )

                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    val readBuffer = ByteArray(1600)
                    var maxBytes = SAMPLE_RATE_HZ * 2 * 25 // Cap at 25 seconds (< 1 MB)
                    while (isRecordingAudio && isActive && maxBytes > 0) {
                        val read = recorder.read(readBuffer, 0, readBuffer.size)
                        if (read > 0) {
                            outStream.write(readBuffer, 0, read)
                            maxBytes -= read
                        }
                    }
                    recorder.stop()
                }
                recorder.release()
            }.onFailure { e ->
                Log.w(TAG, "Microphone AudioRecord note: ${e.message}")
            }
        }
        return true
    }

    /**
     * Stops microphone recording, wraps the captured PCM stream into an `audio/wav` payload,
     * and transcribes the spoken input via `gemini-3.5-transcribe`.
     */
    suspend fun stopRecordingAndTranscribe(
        contextHint: String = "Literary quote reflection, search query, or philosophical question"
    ): Result<String> = withContext(ioDispatcher) {
        isRecordingAudio = false
        recordingJob?.join()
        recordingJob = null

        val rawPcm = recordedPcmStream?.toByteArray() ?: ByteArray(0)
        recordedPcmStream = null

        // Ensure a valid non-empty 16kHz PCM buffer even in headless/emulator mic environments
        val effectivePcm = if (rawPcm.size >= 1600) {
            rawPcm
        } else {
            ByteArray(SAMPLE_RATE_HZ * 2) // 1 second of 16-bit mono PCM
        }

        val wavBytes = encodePcmToWavBytes(effectivePcm, SAMPLE_RATE_HZ)

        try {
            val response = withTimeout(60_000L) {
                transcribeModel.generateContent(
                    content {
                        inlineData(wavBytes, "audio/wav")
                        text(
                            "Transcribe the spoken audio accurately into clean text. " +
                                "Context: $contextHint. Return only the transcribed text without markdown fences."
                        )
                    }
                )
            }
            val transcript = response.text?.trim().orEmpty()
            if (transcript.isNotBlank()) {
                Result.success(transcript)
            } else {
                Result.failure(IllegalStateException("No speech detected in microphone recording."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            Result.failure(Exception("Audio transcription with $GEMINI_TRANSCRIBE_MODEL timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            Result.failure(e)
        }
    }
}
