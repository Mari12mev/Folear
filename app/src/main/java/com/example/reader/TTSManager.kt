package com.example.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex = _currentParagraphIndex.asStateFlow()

    private var paragraphsList: List<String> = emptyList()
    private var speechRate: Float = 1.0f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale("pt", "BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.getDefault()
                }
                engine.setSpeechRate(speechRate)
                isInitialized = true

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        val nextIdx = _currentParagraphIndex.value + 1
                        if (nextIdx < paragraphsList.size) {
                            _currentParagraphIndex.value = nextIdx
                            speakParagraph(nextIdx)
                        } else {
                            _isPlaying.value = false
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                    }
                })
            }
        }
    }

    fun startReading(paragraphs: List<String>, startFromIndex: Int = 0) {
        paragraphsList = paragraphs
        _currentParagraphIndex.value = startFromIndex.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))
        speakParagraph(_currentParagraphIndex.value)
    }

    private fun speakParagraph(index: Int) {
        if (!isInitialized || index >= paragraphsList.size) return
        val text = paragraphsList[index]
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "folhear_tts_$index")
        _isPlaying.value = true
    }

    fun pauseOrResume() {
        if (_isPlaying.value) {
            tts?.stop()
            _isPlaying.value = false
        } else {
            speakParagraph(_currentParagraphIndex.value)
        }
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun setSpeed(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun nextParagraph() {
        val next = _currentParagraphIndex.value + 1
        if (next < paragraphsList.size) {
            _currentParagraphIndex.value = next
            speakParagraph(next)
        }
    }

    fun prevParagraph() {
        val prev = (_currentParagraphIndex.value - 1).coerceAtLeast(0)
        _currentParagraphIndex.value = prev
        speakParagraph(prev)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
