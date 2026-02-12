package org.example.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var micButton: Button
    private lateinit var statusText: TextView
    private lateinit var transcriptionText: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private var isListening: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        micButton = findViewById(R.id.micButton)
        statusText = findViewById(R.id.statusText)
        transcriptionText = findViewById(R.id.transcriptionText)

        // Initialize speech recognizer if available on the device.
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = getString(R.string.status_not_available)
            micButton.isEnabled = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = getString(R.string.status_listening)
                }

                override fun onBeginningOfSpeech() {
                    // No-op
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // No-op (could be used to animate the mic button)
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // No-op
                }

                override fun onEndOfSpeech() {
                    statusText.text = getString(R.string.status_processing)
                }

                override fun onError(error: Int) {
                    // Reset UI state on errors so user can try again.
                    isListening = false
                    statusText.text = getString(R.string.status_error) + " ($error)"
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val finalText = matches?.firstOrNull()
                    if (!finalText.isNullOrBlank()) {
                        transcriptionText.text = finalText
                    }
                    statusText.text = getString(R.string.status_tap_mic)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Stream partial transcription into the UI.
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = matches?.firstOrNull()
                    if (!partialText.isNullOrBlank()) {
                        transcriptionText.text = partialText
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // No-op
                }
            })
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            // Prefer on-device recognition if supported.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // Free-form dictation
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        micButton.setOnClickListener {
            // Toggle listening on mic button click.
            if (isListening) {
                stopListening()
            } else {
                startListeningWithPermissionCheck()
            }
        }
    }

    private fun startListeningWithPermissionCheck() {
        if (hasRecordAudioPermission()) {
            startListening()
        } else {
            statusText.text = getString(R.string.status_permission_needed)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startListening() {
        val sr = speechRecognizer ?: return
        val intent = recognizerIntent ?: return

        statusText.text = getString(R.string.status_listening)
        isListening = true
        sr.startListening(intent)
    }

    private fun stopListening() {
        val sr = speechRecognizer ?: return
        isListening = false
        sr.stopListening()
        statusText.text = getString(R.string.status_processing)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startListening()
            } else {
                statusText.text = getString(R.string.status_permission_needed)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 2001
    }
}
