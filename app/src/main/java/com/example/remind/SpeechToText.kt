package co.rikin.speechtotext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

interface SpeechToText {
    val text: StateFlow<String>
    val isSpeechRecognitionAvailable: Boolean
    fun start()
    fun stop()
    fun destroy()
}

class RealSpeechToText(private val context: Context) : SpeechToText {
    override val text = MutableStateFlow("")
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val speechRecognizer: SpeechRecognizer?
    private val intent: Intent

    override val isSpeechRecognitionAvailable: Boolean

    init {
        isSpeechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        if (!isSpeechRecognitionAvailable) {
            Log.e("RealSpeechToText", "Speech Recognition not available on this device")
            speechRecognizer = null
            intent = Intent()
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("RealSpeechToText", "onReadyForSpeech")
                        _isListening.value = true
                    }

                    override fun onEndOfSpeech() {
                        Log.d("RealSpeechToText", "onEndOfSpeech")
                        _isListening.value = false
                    }

                    override fun onPartialResults(results: Bundle?) {
                        val partial = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.getOrNull(0) ?: ""

                        Log.d("RealSpeechToText", "onPartialResults: $partial")
                        text.value = partial
                    }

                    override fun onResults(results: Bundle?) {
                        val finalResult = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.getOrNull(0) ?: ""
                        Log.d("RealSpeechToText", "onResults: $finalResult")
                        text.value = finalResult
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio"
                            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "Cannot Check Support"
                            SpeechRecognizer.ERROR_CLIENT -> "Client"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient Permissions"
                            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language Not Supported"
                            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language Unavailable"
                            SpeechRecognizer.ERROR_NETWORK -> "Network"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network Timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No Match"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server Error"
                            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server Disconnected"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech Timeout"
                            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too Many Requests"
                            else -> "Unknown"
                        }
                        Log.e("RealSpeechToText", "STT Error: $message")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("RealSpeechToText", "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }

            intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS, true
                )
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS, 1
                )
            }
        }
    }

    override fun start() {
        if (speechRecognizer != null) {
            Log.d("RealSpeechToText", "Starting speech recognition")
            speechRecognizer.startListening(intent)
        } else {
            Log.e("RealSpeechToText", "Speech Recognizer is null, cannot start")
        }
    }

    override fun stop() {
        speechRecognizer?.stopListening()
    }

    override fun destroy() {
        speechRecognizer?.destroy()
    }
}
