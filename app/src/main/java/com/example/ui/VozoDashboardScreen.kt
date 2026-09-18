package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VoicePreference
import com.example.theme.VozoAccent
import com.example.theme.VozoBorder
import com.example.theme.VozoDarkBg
import com.example.theme.VozoDarkSurface
import com.example.theme.VozoDarkSurfaceVariant
import com.example.theme.VozoError
import com.example.theme.VozoPrimary
import com.example.theme.VozoSecondary
import com.example.theme.VozoSuccess
import com.example.theme.VozoTextMuted
import com.example.theme.VozoTextPrimary
import com.example.theme.VozoTextSecondary
import com.example.ui.components.VoiceLabWebViewDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VozoDashboardScreen(
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier,
    hasMicPermission: Boolean = true,
    onRequestAudioPermission: (() -> Unit)? = null,
    viewModel: VozoStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val voicePref by viewModel.voicePreference.collectAsStateWithLifecycle()

    var showEditVoiceName by remember { mutableStateOf(false) }
    var editedVoiceName by remember(voicePref.customVoiceName) {
        mutableStateOf(voicePref.customVoiceName)
    }

    val availableVoices = listOf(
        Triple("minha_voz", voicePref.customVoiceName.ifBlank { "Minha Voz (Clone)" }, "Clonagem Personalizada"),
        Triple("river", "River", "Natural & Equilibrada"),
        Triple("spark", "Spark", "Energética & Jovem"),
        Triple("honey", "Honey", "Suave & Narrativa"),
        Triple("sunny", "Sunny", "Brilhante & Animada")
    )

    val providers = listOf(
        "gemini" to "Gemini Multimodal Live",
        "elevenlabs" to "ElevenLabs Engine",
        "neural_local" to "Local Neural Cache"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VozoDarkBg)
            .testTag("vozo_dashboard_screen")
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Dashboard & Configurações",
                        color = VozoTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gerenciamento de Vozes e Preferências",
                        color = VozoTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            actions = {
                Button(
                    onClick = onNavigateToStudio,
                    colors = ButtonDefaults.buttonColors(containerColor = VozoPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_go_to_studio_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Abrir Studio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VozoDarkSurface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Overview Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VozoPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = VozoDarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(VozoPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = VozoPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voz Ativa no Laboratório",
                                color = VozoTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (voicePref.selectedVoiceId == "minha_voz") {
                                    voicePref.customVoiceName.ifBlank { "Minha Voz (Clone)" }
                                } else {
                                    voicePref.selectedVoiceId.replaceFirstChar { it.uppercase() }
                                },
                                color = VozoTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(VozoSuccess)
                                )
                                Text(
                                    text = "Motor: ${providers.find { it.first == voicePref.voiceProvider }?.second ?: "Gemini Live"}",
                                    color = VozoAccent,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Voice Laboratory Selection Section
            item {
                Text(
                    text = "Seleção de Voz do Estúdio",
                    color = VozoTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableVoices.forEach { (voiceId, name, desc) ->
                        val isSelected = voicePref.selectedVoiceId == voiceId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectVoice(voiceId, voicePref.voiceProvider)
                                    Toast.makeText(context, "Voz selecionada: $name", Toast.LENGTH_SHORT).show()
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) VozoPrimary else VozoBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("voice_item_$voiceId"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) VozoPrimary.copy(alpha = 0.08f) else VozoDarkSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) VozoPrimary else VozoDarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (voiceId == "minha_voz") Icons.Default.RecordVoiceOver else Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else VozoTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = VozoTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = desc,
                                        color = VozoTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (voiceId == "minha_voz") {
                                    IconButton(
                                        onClick = { showEditVoiceName = !showEditVoiceName },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar nome",
                                            tint = VozoAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(VozoPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selecionada",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Edit Cloned Voice Name Collapsible
                AnimatedVisibility(visible = showEditVoiceName) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(1.dp, VozoAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = VozoDarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Personalizar Nome da Voz Clonada",
                                color = VozoTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editedVoiceName,
                                onValueChange = { editedVoiceName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_voice_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VozoPrimary,
                                    unfocusedBorderColor = VozoBorder,
                                    focusedTextColor = VozoTextPrimary,
                                    unfocusedTextColor = VozoTextPrimary
                                ),
                                placeholder = { Text("Ex: Minha Voz (Dublagem)", color = VozoTextMuted) },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        val newName = editedVoiceName.trim().ifBlank { "Minha Voz (Clone)" }
                                        viewModel.updateVoicePreference(
                                            voicePref.copy(customVoiceName = newName)
                                        )
                                        showEditVoiceName = false
                                        Toast.makeText(context, "Nome salvo: $newName", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VozoPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_save_voice_name")
                                ) {
                                    Text("Salvar Nome", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Voice Engine Provider Section
            item {
                Text(
                    text = "Provedor de Síntese Vocal",
                    color = VozoTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.forEach { (providerKey, providerLabel) ->
                        val isSelected = voicePref.voiceProvider == providerKey
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateVoicePreference(voicePref.copy(voiceProvider = providerKey))
                            },
                            label = {
                                Text(
                                    text = providerLabel,
                                    color = if (isSelected) VozoTextPrimary else VozoTextSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VozoSecondary,
                                containerColor = VozoDarkSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) VozoAccent else VozoBorder
                            ),
                            modifier = Modifier.testTag("provider_chip_$providerKey")
                        )
                    }
                }
            }

            // Permissions & Hardware Section
            item {
                Text(
                    text = "Hardware e Permissões",
                    color = VozoTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VozoBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VozoDarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (hasMicPermission) VozoSuccess.copy(alpha = 0.15f) else VozoError.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (hasMicPermission) VozoSuccess else VozoError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Captura de Microfone",
                                    color = VozoTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasMicPermission) "Permissão Concedida (Audio Capture Pronto)" else "Permissão Ausente ou Bloqueada",
                                    color = if (hasMicPermission) VozoSuccess else VozoError,
                                    fontSize = 12.sp
                                )
                            }

                            if (!hasMicPermission) {
                                Button(
                                    onClick = { onRequestAudioPermission?.invoke() },
                                    colors = ButtonDefaults.buttonColors(containerColor = VozoPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_request_mic_permission")
                                ) {
                                    Text("Conceder", fontSize = 12.sp)
                                }
                            }
                        }

                        // Low-latency Audio Sampling Info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VozoDarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = VozoAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Taxa de Amostragem: 16kHz / 24kHz PCM (Full Duplex)",
                                color = VozoTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Session & Storage Management Section
            item {
                Text(
                    text = "Sessão e Armazenamento Local",
                    color = VozoTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VozoBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VozoDarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = VozoSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Banco de Dados Room (SQLite)",
                                    color = VozoTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Preferências de voz e tokens persistidos com segurança",
                                    color = VozoTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (authState.isLoggedIn) VozoSuccess else VozoTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (authState.isLoggedIn) "Sessão Conectada" else "Modo Visitante / Convidado",
                                    color = VozoTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (authState.token != null) "Token ativo (ID: ${authState.token?.take(8)}...)" else "Sem token de acesso persistido",
                                    color = VozoTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (authState.isLoggedIn) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.logout()
                                        Toast.makeText(context, "Sessão encerrada com sucesso", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VozoError),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_logout")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Encerrar Sessão", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        WebStorage.getInstance().deleteAllData()
                                        CookieManager.getInstance().removeAllCookies(null)
                                        Toast.makeText(context, "Cache e cookies limpos", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erro ao limpar cache", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VozoTextSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_clear_cache")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Limpar Cache", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Quick External Links
            item {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(VoiceLabWebViewDefaults.VOICE_LAB_URL))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_in_browser"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VozoTextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(VozoBorder))
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = VozoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Voice Lab no Navegador Externo", fontSize = 13.sp)
                }
            }

            // Big Action Button at bottom to navigate to Studio
            item {
                Button(
                    onClick = onNavigateToStudio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_go_to_studio_bottom"),
                    colors = ButtonDefaults.buttonColors(containerColor = VozoPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ir para o Estúdio de Voz",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
