package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.theme.VozoDarkBg
import com.example.theme.VozoMagpieTheme
import com.example.ui.VozoStudioScreen

class MainActivity : ComponentActivity() {

    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    var hasMicPermission by mutableStateOf(false)
        private set

    private val requestAudioPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasMicPermission = isGranted
            pendingPermissionCallback?.invoke(isGranted)
            pendingPermissionCallback = null
            if (!isGranted) {
                Toast.makeText(
                    this,
                    getString(R.string.audio_permission_rationale),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    fun requestMicrophonePermission(onResult: ((Boolean) -> Unit)? = null) {
        val isAlreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        hasMicPermission = isAlreadyGranted

        if (isAlreadyGranted) {
            onResult?.invoke(true)
        } else {
            pendingPermissionCallback = onResult
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check current microphone permission status
        hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // If not granted, request permission on launch for voice processing in WebView
        if (!hasMicPermission) {
            requestMicrophonePermission()
        }

        setContent {
            VozoMagpieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VozoDarkBg
                ) {
                    VozoStudioScreen(
                        hasMicPermission = hasMicPermission,
                        onRequestAudioPermission = { callback ->
                            requestMicrophonePermission(callback)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission state in case user modified it in system settings
        hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
