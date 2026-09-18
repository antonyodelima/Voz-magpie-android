package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.theme.VozoAccent
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurface
import com.example.theme.VozoDarkSurfaceVariant
import com.example.theme.VozoError
import com.example.theme.VozoPrimary
import com.example.theme.VozoSuccess
import com.example.theme.VozoTextMuted
import com.example.theme.VozoTextPrimary
import com.example.theme.VozoTextSecondary

@Composable
fun VoiceQuickToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onOpenBrowser: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    hasMicPermission: Boolean = true,
    onRequestAudioPermission: (() -> Unit)? = null,
    activeVoiceName: String = "Minha Voz (Clone)",
    isLoggedIn: Boolean = false,
    onClearAuth: () -> Unit = {},
    onNavigateToDashboard: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = VozoDarkSurface,
        contentColor = VozoTextPrimary,
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_quick_toolbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (hasMicPermission) VozoPrimary else VozoError),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (hasMicPermission) "Microphone Active" else "Microphone Disabled",
                        tint = VozoDarkBg,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vozo Magpie",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VozoTextPrimary
                    )
                )
            }

            // Navigation Actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasMicPermission && onRequestAudioPermission != null) {
                    IconButton(
                        onClick = onRequestAudioPermission,
                        modifier = Modifier.testTag("btn_mic_permission")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = stringResource(R.string.mic_permission_request),
                            tint = VozoError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = if (canGoBack) VozoTextPrimary else VozoTextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier.testTag("btn_forward")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.nav_forward),
                        tint = if (canGoForward) VozoTextPrimary else VozoTextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("btn_refresh")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.nav_reload),
                        tint = VozoTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onHome,
                    modifier = Modifier.testTag("btn_home")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.nav_home),
                        tint = VozoAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("btn_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = VozoTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(VozoDarkSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (hasMicPermission) stringResource(R.string.mic_permission_granted)
                                    else stringResource(R.string.mic_permission_request),
                                    color = VozoTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (hasMicPermission) VozoSuccess else VozoError
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                if (!hasMicPermission) {
                                    onRequestAudioPermission?.invoke()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Voz Salva: $activeVoiceName", color = VozoTextPrimary, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = VozoAccent)
                            },
                            onClick = {
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isLoggedIn) "Sessão: Conectada" else "Sessão: Convidado / Anônimo",
                                    color = if (isLoggedIn) VozoSuccess else VozoTextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (isLoggedIn) VozoSuccess else VozoTextSecondary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                if (isLoggedIn) {
                                    onClearAuth()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dashboard & Ajustes", color = VozoTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = VozoPrimary)
                            },
                            onClick = {
                                menuExpanded = false
                                onNavigateToDashboard?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open in Browser", color = VozoTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = VozoPrimary)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenBrowser()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Link", color = VozoTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null, tint = VozoAccent)
                            },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                    }
                }
            }
        }
    }
}
