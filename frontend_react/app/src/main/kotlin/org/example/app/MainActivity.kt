package org.example.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var micButton: Button
    private lateinit var statusText: TextView

    private lateinit var queryEditText: EditText
    private lateinit var searchProgress: ProgressBar
    private lateinit var searchResultText: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private var isListening: Boolean = false

    // Used for the placeholder async "search" operation.
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private var pendingSearchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        micButton = findViewById(R.id.micButton)
        statusText = findViewById(R.id.statusText)

        queryEditText = findViewById(R.id.queryEditText)
        searchProgress = findViewById(R.id.searchProgress)
        searchResultText = findViewById(R.id.searchResultText)

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
                        // Put the final recognition result into the EditText...
                        queryEditText.setText(finalText)
                        // ...and automatically trigger a "search" based on that text.
                        triggerAutoSearch(finalText)
                    }

                    statusText.text = getString(R.string.status_tap_mic)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Per instructions: write FINAL speech result into the EditText.
                    // For partial results we keep status only (no query updates) to avoid excessive churn.
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

    /**
     * Runs a placeholder "search" based on the final recognized query.
     * Shows a loading indicator while the search is "running" and then displays the result.
     */
    private fun triggerAutoSearch(query: String) {
        // Cancel any previously scheduled placeholder search so the latest query wins.
        pendingSearchRunnable?.let { mainHandler.removeCallbacks(it) }

        setSearchingUiState(true)

        // Placeholder behavior: simulate a network/search delay.
        pendingSearchRunnable = Runnable {
            val cleaned = query.trim()
            val result = if (cleaned.isBlank()) {
                "No query provided."
            } else {
                // Placeholder result text — replace with real search logic when available.
                "Result for: \"$cleaned\""
            }

            searchResultText.text = result
            setSearchingUiState(false)
        }.also { runnable ->
            // Simulate ~1 second "search" time.
            mainHandler.postDelayed(runnable, 1000L)
        }
    }

    private fun setSearchingUiState(isSearching: Boolean) {
        searchProgress.visibility = if (isSearching) View.VISIBLE else View.GONE
        // Optional UX: prevent accidental edits while "searching"
        queryEditText.isEnabled = !isSearching
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

        pendingSearchRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingSearchRunnable = null

        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 2001
    }
}
