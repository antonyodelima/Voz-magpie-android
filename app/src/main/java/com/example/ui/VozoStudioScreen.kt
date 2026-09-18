package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.AppPersistenceBridge
import com.example.theme.VozoAccent
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurfaceVariant
import com.example.theme.VozoError
import com.example.theme.VozoPrimary
import com.example.theme.VozoTextSecondary
import com.example.ui.components.AudioWaveformLoader
import com.example.ui.components.ConnectionErrorView
import com.example.ui.components.VoiceLabWebView
import com.example.ui.components.VoiceLabWebViewDefaults
import com.example.ui.components.VoiceQuickToolbar
import com.example.ui.components.VozoSplashScreen

private const val VOZO_STUDIO_URL = VoiceLabWebViewDefaults.VOICE_LAB_URL

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VozoStudioScreen(
    modifier: Modifier = Modifier,
    hasMicPermission: Boolean = true,
    onRequestAudioPermission: (((Boolean) -> Unit) -> Unit)? = null,
    onNavigateToDashboard: (() -> Unit)? = null,
    viewModel: VozoStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val voicePref by viewModel.voicePreference.collectAsStateWithLifecycle()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewKey by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File upload callback for WebChromeClient
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // Hardware back navigation
    BackHandler(enabled = canGoBack) {
        webViewInstance?.let { webView ->
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }

    val persistenceBridge = remember(viewModel.repository, coroutineScope) {
        AppPersistenceBridge(viewModel.repository, coroutineScope)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VozoDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("vozo_studio_screen")
    ) {
        // Quick control header
        VoiceQuickToolbar(
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onBack = { webViewInstance?.goBack() },
            onForward = { webViewInstance?.goForward() },
            onRefresh = {
                hasError = false
                webViewInstance?.reload()
            },
            onHome = {
                hasError = false
                webViewInstance?.loadUrl(VOZO_STUDIO_URL)
            },
            onOpenBrowser = {
                val currentUrl = webViewInstance?.url ?: VOZO_STUDIO_URL
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                context.startActivity(intent)
            },
            onShare = {
                val currentUrl = webViewInstance?.url ?: VOZO_STUDIO_URL
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out Vozo Magpie Voice Studio: $currentUrl")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Vozo Studio")
                context.startActivity(shareIntent)
            },
            hasMicPermission = hasMicPermission,
            onRequestAudioPermission = {
                onRequestAudioPermission?.invoke { }
            },
            activeVoiceName = voicePref.customVoiceName.ifBlank { voicePref.selectedVoiceId },
            isLoggedIn = authState.isLoggedIn,
            onClearAuth = {
                viewModel.logout()
                webViewInstance?.evaluateJavascript(
                    "(function() { try { localStorage.removeItem('base44_access_token'); localStorage.removeItem('token'); } catch(e){} })();",
                    null
                )
            },
            onNavigateToDashboard = onNavigateToDashboard
        )

        // Microphone permission notification banner if not granted
        AnimatedVisibility(visible = !hasMicPermission) {
            Surface(
                color = VozoDarkSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mic_permission_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = VozoError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mic_permission_banner_text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = VozoTextSecondary,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onRequestAudioPermission?.invoke { } },
                        modifier = Modifier.testTag("btn_request_mic_banner")
                    ) {
                        Text(
                            text = stringResource(R.string.mic_permission_request),
                            color = VozoAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Progress bar
        AnimatedVisibility(
            visible = isLoading && loadProgress < 100,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = VozoPrimary,
                trackColor = VozoDarkBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Reusable VoiceLabWebView
            key(webViewKey) {
                VoiceLabWebView(
                    modifier = Modifier.fillMaxSize(),
                    url = VOZO_STUDIO_URL,
                    onWebViewCreated = { webView ->
                        webViewInstance = webView
                    },
                    onLoadingChanged = { loading, progress ->
                        isLoading = loading
                        loadProgress = progress
                    },
                    onNavigationStateChanged = { back, fwd ->
                        canGoBack = back
                        canGoForward = fwd
                    },
                    onError = { _, desc, _ ->
                        isLoading = false
                        hasError = true
                        errorMessage = desc
                    },
                    onRequestAudioPermission = onRequestAudioPermission,
                    onShowFileChooser = { callback, _ ->
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        filePickerLauncher.launch("*/*")
                        true
                    },
                    persistenceBridge = persistenceBridge
                )
            }

            // Vozo Magpie Splash Screen while WebView initialises
            if (isLoading) {
                VozoSplashScreen(
                    loadProgress = loadProgress,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Connection Error Overlay
            if (hasError) {
                ConnectionErrorView(
                    errorMessage = errorMessage,
                    onRetry = {
                        hasError = false
                        isLoading = true
                        webViewKey++
                    },
                    onOpenExternal = {
                        val currentUrl = webViewInstance?.url ?: VOZO_STUDIO_URL
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}
