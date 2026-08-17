package com.example.edupathai.ui.chatbox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Mic button that captures speech on-device via [SpeechRecognizer] (no system dialog),
 * requests RECORD_AUDIO at runtime if needed, and reports the transcript via [onResult].
 */
@Composable
fun VoiceInputButton(
    enabled: Boolean = true,
    onResult: (String) -> Unit,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            onError(
                "Speech recognition isn't available on this device. " +
                        "Make sure Google app / Speech Services by Google is installed and up to date " +
                        "(on an emulator, use one with the Google Play Store, not a bare AOSP image)."
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                // ERROR_NO_MATCH / ERROR_SPEECH_TIMEOUT just mean "heard silence" - not worth surfacing.
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    return
                }
                val message = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice input needs an internet connection."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice input is still finishing up - try again in a moment."
                    SpeechRecognizer.ERROR_CLIENT -> "Voice input was cancelled. Try again."
                    else -> "Voice input error (code $error). Please try again."
                }
                onError(message)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val transcript = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!transcript.isNullOrBlank()) {
                    onResult(transcript)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            onError("Microphone permission is required for voice input.")
        }
    }

    IconButton(
        enabled = enabled,
        onClick = {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            when {
                isListening -> speechRecognizer?.stopListening()
                hasPermission -> startListening()
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isListening) "Stop voice input" else "Start voice input",
            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}