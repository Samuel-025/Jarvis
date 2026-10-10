package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.agent.StepStatus
import kotlinx.coroutines.launch
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import com.example.core.model.CommandResult
import com.example.core.model.PrivacyMode
import com.example.core.voice.VoiceState
import com.example.ui.theme.JarvisCardDark
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreenOk
import com.example.ui.theme.JarvisNavyDark
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisAccentBlue

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsDestination by remember { mutableStateOf("General") }
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = JarvisNavyDark, bottomBar = {
        NavigationBar(containerColor = JarvisSurfaceDark, contentColor = JarvisCyan) {
            val tabs = listOf(Triple("Assistant", Icons.Default.Assistant, 0), Triple("Settings", Icons.Default.Settings, 1), Triple("Permissions", Icons.Default.Lock, 2), Triple("Debug", Icons.Default.BugReport, 3))
            tabs.forEach { (label, image, index) -> NavigationBarItem(selected = selectedTab == index, onClick = { selectedTab = index }, icon = { Icon(image, contentDescription = label) }, label = { Text(label, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = JarvisCyan, selectedTextColor = JarvisTextPrimary, indicatorColor = JarvisCardDark, unselectedIconColor = JarvisTextSecondary, unselectedTextColor = JarvisTextSecondary)) }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> ConsoleTab(viewModel, uiState)
                1 -> SettingsHubTab(viewModel, uiState, settingsDestination) { settingsDestination = it }
                2 -> PermissionsOverviewTab()
                else -> AuditIntegrationsTab(viewModel)
            }
            uiState.pendingConfirmation?.let { req -> AlertDialog(onDismissRequest = { viewModel.rejectPendingAction() }, title = { Text("Confirmation Required", color = JarvisGold) }, text = { Text(req.prompt, color = JarvisTextPrimary) }, confirmButton = { Button(onClick = { viewModel.confirmPendingAction() }, colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan), modifier = Modifier.testTag("confirm_action_button")) { Text("Approve", color = JarvisNavyDark, fontWeight = FontWeight.Bold) } }, dismissButton = { OutlinedButton(onClick = { viewModel.rejectPendingAction() }, modifier = Modifier.testTag("reject_action_button")) { Text("Reject", color = JarvisRedAlert) } }, containerColor = JarvisSurfaceDark) }
        }
    }
}

@Composable
fun TopSafetyHeader(
    uiState: MainUiState,
    onEmergencyStop: () -> Unit,
    onResetEmergencyStop: () -> Unit,
    onPrivacySelected: (PrivacyMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isEmergencyStopped) JarvisRedAlert else JarvisGreenOk)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isEmergencyStopped) "STATUS: HALTED" else "JARVIS MOBILE OS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isEmergencyStopped) JarvisRedAlert else JarvisCyan
                    )
                }

                // Emergency Stop Button
                if (uiState.isEmergencyStopped) {
                    Button(
                        onClick = onResetEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisGreenOk),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("emergency_reset_button")
                    ) {
                        Text("Reset Stop", fontSize = 12.sp, color = JarvisNavyDark, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisRedAlert),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("emergency_stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = JarvisTextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EMERGENCY STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Privacy Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy:",
                    fontSize = 12.sp,
                    color = JarvisTextSecondary
                )
                PrivacyMode.values().forEach { mode ->
                    FilterChip(
                        selected = uiState.privacyMode == mode,
                        onClick = { onPrivacySelected(mode) },
                        label = { Text(mode.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JarvisCyan,
                            selectedLabelColor = JarvisNavyDark,
                            labelColor = JarvisTextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ConsoleTab(viewModel: MainViewModel, uiState: MainUiState) {
    val context = LocalContext.current
    val conversationHistory by viewModel.conversationHistory.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }
    var showMicPermissionRationale by remember { mutableStateOf(false) }
    var showMicPermissionDenied by remember { mutableStateOf(false) }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showMicPermissionDenied = false
            viewModel.startVoiceListening()
        } else {
            showMicPermissionDenied = true
        }
    }
    val flashlightPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onRuntimePermissionResult(granted) }
    val listening = uiState.voiceState == VoiceState.LISTENING
    val stateColor = when {
        uiState.voiceState == VoiceState.LISTENING -> JarvisCyan
        uiState.voiceState == VoiceState.SPEAKING -> JarvisGold
        uiState.voiceState == VoiceState.ERROR -> JarvisRedAlert
        uiState.voiceState == VoiceState.PROCESSING || uiState.isProcessing -> JarvisAccentBlue
        else -> JarvisCyan
    }
    val voiceStatus = when {
        uiState.voiceState == VoiceState.ERROR -> "Voice issue"
        uiState.voiceState == VoiceState.LISTENING -> "Listening"
        uiState.voiceState == VoiceState.SPEAKING -> "Speaking"
        uiState.voiceState == VoiceState.PROCESSING || uiState.isProcessing -> "Processing"
        else -> "Ready"
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(uiState.privacyMode.name.lowercase().replaceFirstChar { it.uppercase() }, JarvisCyan)
            StatusPill(voiceStatus, stateColor)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { if (uiState.isEmergencyStopped) viewModel.resetEmergencyStop() else viewModel.triggerEmergencyStop() }, modifier = Modifier.size(48.dp).clip(CircleShape).background(JarvisSurfaceDark).testTag("emergency_stop_button")) { Icon(if (uiState.isEmergencyStopped) Icons.Default.Refresh else Icons.Default.Stop, contentDescription = "Emergency stop", tint = if (uiState.isEmergencyStopped) JarvisGreenOk else JarvisRedAlert) }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.weight(0.72f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().border(2.dp, JarvisCyan.copy(alpha = .20f), CircleShape).padding(18.dp).border(3.dp, JarvisCyan.copy(alpha = .35f), CircleShape).padding(20.dp).border(4.dp, stateColor, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(if (listening) 62.dp else 50.dp).clip(CircleShape).background(stateColor.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Box(Modifier.size(20.dp).clip(CircleShape).background(stateColor)) }
                    }
                }
                Text(uiState.voiceState.name, color = stateColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                when {
                    uiState.isProcessing -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), color = JarvisCyan, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Processing request…", color = JarvisTextSecondary, fontSize = 13.sp) }
                    uiState.lastCommandResult is CommandResult.Success -> Text((uiState.lastCommandResult as CommandResult.Success).message, color = JarvisCyanLight, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 4)
                    uiState.lastCommandResult is CommandResult.Error -> Text((uiState.lastCommandResult as CommandResult.Error).message, color = JarvisRedAlert, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 4)
                    uiState.lastRecognizedSpeech.isNotBlank() -> Text("“${uiState.lastRecognizedSpeech}”", color = JarvisTextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                    else -> Text("Tap the microphone and speak, or type a command.", color = JarvisTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp)
                }
                uiState.voiceErrorMessage?.let { Text(it, color = JarvisRedAlert, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().weight(1.12f),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CHAT", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${conversationHistory.size} saved messages", color = JarvisTextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.height(6.dp))
                if (conversationHistory.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Hi, I'm Jarvis.", color = JarvisTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Ask a question, use your voice, or try a phone command.", color = JarvisTextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(conversationHistory.take(80).reversed(), key = { it.id }) { message ->
                            val isUser = message.role.equals("USER", ignoreCase = true)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(0.88f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) JarvisCardDark else JarvisNavyDark
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp, topEnd = 14.dp,
                                        bottomEnd = if (isUser) 4.dp else 14.dp,
                                        bottomStart = if (isUser) 14.dp else 4.dp
                                    )
                                ) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(
                                            if (isUser) "You" else "Jarvis",
                                            color = if (isUser) JarvisCyanLight else JarvisGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(message.content, color = JarvisTextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = textInput, onValueChange = { textInput = it }, placeholder = { Text("Tap the microphone and speak, or type a command.", color = JarvisTextSecondary, fontSize = 13.sp) }, modifier = Modifier.weight(1f).testTag("command_input_field"), shape = RoundedCornerShape(14.dp), maxLines = 3)
            IconButton(
                onClick = {
                    if (listening) {
                        viewModel.stopVoiceListening()
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.startVoiceListening()
                    } else {
                        showMicPermissionRationale = true
                    }
                },
                modifier = Modifier.size(54.dp).clip(CircleShape).background(JarvisSurfaceDark).testTag("voice_mic_fab")
            ) { Icon(if (listening) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Microphone", tint = if (listening) JarvisRedAlert else JarvisCyan, modifier = Modifier.size(29.dp)) }
            IconButton(onClick = { if (textInput.isNotBlank()) { viewModel.executeTextCommand(textInput); textInput = "" } }, modifier = Modifier.size(44.dp).testTag("send_command_button")) { Icon(Icons.Default.Send, contentDescription = "Send command", tint = JarvisCyan, modifier = Modifier.size(29.dp)) }
        }
    }

    if (uiState.pendingRuntimePermission == Manifest.permission.CAMERA) {
        AlertDialog(
            onDismissRequest = { viewModel.onRuntimePermissionResult(false) },
            title = { Text("Allow flashlight access?", color = JarvisCyan) },
            text = { Text("Jarvis needs Android camera permission to control the phone's flashlight. It does not capture or upload images for this torch command. You can cancel and keep using other features.", color = JarvisTextPrimary) },
            confirmButton = {
                Button(onClick = { flashlightPermissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)) {
                    Text("Continue", color = JarvisNavyDark)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.onRuntimePermissionResult(false) }) { Text("Not now", color = JarvisTextSecondary) } },
            containerColor = JarvisSurfaceDark
        )
    }

    if (showMicPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showMicPermissionRationale = false },
            title = { Text("Allow microphone access?", color = JarvisCyan) },
            text = { Text("Jarvis needs microphone access only when you choose voice input. You can keep using text commands without it.", color = JarvisTextPrimary) },
            confirmButton = {
                Button(onClick = {
                    showMicPermissionRationale = false
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }, colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)) {
                    Text("Continue", color = JarvisNavyDark)
                }
            },
            dismissButton = { TextButton(onClick = { showMicPermissionRationale = false }) { Text("Not now", color = JarvisTextSecondary) } },
            containerColor = JarvisSurfaceDark
        )
    }

    if (showMicPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showMicPermissionDenied = false },
            title = { Text("Voice input unavailable", color = JarvisGold) },
            text = { Text("Microphone access was not granted. Text commands still work. You can review microphone access in Android Settings.", color = JarvisTextPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    showMicPermissionDenied = false
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                }) { Text("Open Settings", color = JarvisCyan) }
            },
            dismissButton = { TextButton(onClick = { showMicPermissionDenied = false }) { Text("Close", color = JarvisTextSecondary) } },
            containerColor = JarvisSurfaceDark
        )
    }
}

@Composable
private fun StatusPill(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.clip(RoundedCornerShape(50)).border(1.dp, color.copy(alpha = .6f), RoundedCornerShape(50)).background(color.copy(alpha = .10f)).padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color)); Text(label, color = JarvisTextPrimary, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun SettingsHubTab(viewModel: MainViewModel, uiState: MainUiState, destination: String, onDestinationChange: (String) -> Unit) {
    val destinations = listOf("General", "AI & Models", "Personal OS", "Agent & Vision")
    Column(Modifier.fillMaxSize()) {
        Text("Settings", color = JarvisTextPrimary, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp))
        androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(destinations) { item -> FilterChip(selected = destination == item, onClick = { onDestinationChange(item) }, label = { Text(item, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = JarvisCyan, selectedLabelColor = JarvisNavyDark, labelColor = JarvisTextSecondary)) } }
        when (destination) { "AI & Models" -> AISettingsTab(viewModel); "Personal OS" -> PersonalOsTab(viewModel); "Agent & Vision" -> AgentVisionTab(viewModel, uiState); else -> GeneralSettingsContent(viewModel, uiState) }
    }
}

@Composable
private fun GeneralSettingsContent(viewModel: MainViewModel, uiState: MainUiState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy Mode", color = JarvisCyan, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                listOf(PrivacyMode.STRICT to "Strict — local-only, no cloud data", PrivacyMode.BALANCED to "Balanced — confirm before cloud queries", PrivacyMode.CLOUD to "Cloud — send queries/photos to selected AI").forEach { (mode, label) ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.setPrivacyMode(mode) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.RadioButton(selected = uiState.privacyMode == mode, onClick = { viewModel.setPrivacyMode(mode) }, colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = JarvisCyan)); Spacer(Modifier.width(10.dp)); Text(label, color = JarvisTextPrimary, fontSize = 13.sp) }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Divider(color = JarvisCardDark)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Include saved memory in Balanced cloud queries", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Off by default. When enabled, relevant saved memories, notes and chat excerpts may be sent to the selected cloud AI provider with your question. Strict mode remains local-only; Cloud mode uses memory automatically.", color = JarvisTextSecondary, fontSize = 11.sp)
                    }
                    Switch(checked = uiState.includeMemoryInCloud, onCheckedChange = { viewModel.setIncludeMemoryInCloud(it) })
                }
                Text("Voice", color = JarvisCyan, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Recognition language", color = JarvisTextPrimary)
                Text("Jarvis uses the language selected by your Android speech-recognition service. Change it in your device's speech input settings.", color = JarvisTextSecondary, fontSize = 13.sp)
                Divider(color = JarvisCardDark)
                Text("Voice status: ${uiState.voiceState.name}", color = JarvisTextPrimary, fontSize = 13.sp)
                Text("Spoken responses use the Android text-to-speech engine configured on your device.", color = JarvisTextSecondary, fontSize = 13.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Diagnostics & Safety", color = JarvisCyan, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (uiState.isEmergencyStopped) "Emergency stop is active. Commands are halted." else "Emergency stop is inactive.", color = JarvisTextSecondary, fontSize = 13.sp)
                Button(onClick = { if (uiState.isEmergencyStopped) viewModel.resetEmergencyStop() else viewModel.triggerEmergencyStop() }, colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isEmergencyStopped) JarvisGreenOk else JarvisRedAlert)) { Text(if (uiState.isEmergencyStopped) "Reset emergency stop" else "Trigger emergency stop", color = JarvisNavyDark) }
            }
        }
    }
}

@Composable
fun PermissionsOverviewTab() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshToken by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshToken++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshToken++ }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshToken++ }
    val permissions = listOf(
        Triple("Internet", "Normal install-time permission used by cloud AI providers when enabled.", "internet"),
        Triple("Network state", "Normal install-time permission used to detect connectivity.", "network"),
        Triple("Vibration", "Normal permission for haptic feedback.", Manifest.permission.VIBRATE),
        Triple("Microphone", "Optional. Requested only when you choose voice input. Text commands work without it.", Manifest.permission.RECORD_AUDIO),
        Triple("Flashlight control", "Optional CAMERA permission is needed only when Jarvis controls the phone torch. Vision uses Android's camera preview flow separately and asks before cloud image analysis.", Manifest.permission.CAMERA)
    )

    Column(Modifier.fillMaxSize()) {
        Text("Permissions", color = JarvisTextPrimary, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp))
        Text("Only permissions used by this build are listed. Runtime access is requested when you choose the related feature.", color = JarvisTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        key(refreshToken) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(permissions) { (title, description, permission) ->
                    val status = when (permission) {
                        "network", "internet" -> "Declared · install-time"
                        Manifest.permission.VIBRATE -> if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) "Available" else "Unavailable"
                        else -> if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) "Granted" else "Not granted"
                    }
                    val canRequest = permission == Manifest.permission.RECORD_AUDIO || permission == Manifest.permission.CAMERA
                    val actionModifier = if (canRequest && status == "Not granted") Modifier.clickable {
                        when (permission) {
                            Manifest.permission.RECORD_AUDIO -> microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            Manifest.permission.CAMERA -> cameraLauncher.launch(Manifest.permission.CAMERA)
                            else -> Unit
                        }
                    } else Modifier
                    Card(Modifier.fillMaxWidth().then(actionModifier), colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark), shape = RoundedCornerShape(15.dp)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(title, color = JarvisTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(description, color = JarvisTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                                }
                                if (canRequest && status == "Not granted") Icon(Icons.Default.ChevronRight, contentDescription = "Request $title permission", tint = JarvisTextSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusPill(if (canRequest) "Optional" else "Normal", JarvisCyan)
                                StatusPill(status, if (status == "Granted" || status.startsWith("Declared") || status == "Available") JarvisGreenOk else JarvisGold)
                                if (canRequest && status == "Not granted") Text("Tap to request", color = JarvisCyan, fontSize = 10.sp)
                            }
                        }
                    }
                }
                item {
                    Text("This build does not currently declare an Accessibility service, notification listener, or floating overlay. Those special-access settings are therefore not requested or shown as enabled.", color = JarvisTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
fun PersonalOsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val okfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val bundle = viewModel.exportOkfBundle()
                val output = context.contentResolver.openOutputStream(uri) ?: error("Could not open the selected destination")
                ZipOutputStream(output).use { zip ->
                    bundle.forEach { (path, content) ->
                        zip.putNextEntry(ZipEntry(path))
                        zip.write(content.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                }
                bundle.size
            }.onSuccess { exportStatus = "Exported $it OKF files. Keep the ZIP private; it contains your saved memory and chat history." }
             .onFailure { exportStatus = "OKF export failed: ${it.message ?: "unknown error"}" }
        }
    }
    val okfImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val files = linkedMapOf<String, String>()
                var totalBytes = 0
                var entryCount = 0
                val input = context.contentResolver.openInputStream(uri) ?: error("Could not open the selected ZIP")
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entryCount > 500) error("ZIP contains too many entries (limit 500)")
                        if (!entry.isDirectory) {
                            val name = entry.name.replace('\\\\', '/')
                            if (name.startsWith("/") || name.split('/').any { it == ".." } || name.length > 240) {
                                error("ZIP contains an unsafe file path")
                            }
                            val bytes = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            var entryBytes = 0
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                entryBytes += count
                                totalBytes += count
                                if (entryBytes > 100_000) error("File exceeds 100 KB: $name")
                                if (totalBytes > 5_000_000) error("ZIP exceeds 5 MB uncompressed limit")
                                bytes.write(buffer, 0, count)
                            }
                            if (name.endsWith(".md", ignoreCase = true)) {
                                val content = bytes.toByteArray().toString(Charsets.UTF_8)
                                if (files.put(name, content) != null) error("ZIP contains duplicate path: $name")
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                viewModel.importOkfBundle(files)
            }.onSuccess { summary ->
                exportStatus = "Import finished: ${summary.importedMemories} memories and ${summary.importedNotes} notes added; ${summary.skippedFiles} other files skipped; ${summary.errors.size} validation errors. Existing entries were preserved."
            }.onFailure { exportStatus = "OKF import failed: ${it.message ?: "unknown error"}" }
        }
    }
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LONG-TERM MEMORY & OKF", color = JarvisCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Chat turns are stored locally in Room and retrieved for relevant AI questions. Export a portable Open Knowledge Format v0.2 bundle of memories, notes and conversation history.", color = JarvisTextSecondary, fontSize = 11.sp)
                    Button(onClick = { okfExportLauncher.launch("Jarvis-Memory-OKF.zip") }, colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)) {
                        Text("Export Memory as OKF ZIP", color = JarvisNavyDark, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { okfImportLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                        Text("Import OKF ZIP", color = JarvisCyanLight, fontWeight = FontWeight.Bold)
                    }
                    Text("Imports only validated memory and note files from OKF v0.2. Conversation logs are never imported as trusted memory. Existing data is not overwritten.", color = JarvisTextSecondary, fontSize = 10.sp)
                    exportStatus?.let { Text(it, color = JarvisTextSecondary, fontSize = 10.sp) }
                }
            }
        }
        // Section: Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TASKS & ROUTINES", fontWeight = FontWeight.Bold, color = JarvisCyan, fontSize = 13.sp)
                Button(
                    onClick = { showAddTaskDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCardDark),
                    modifier = Modifier.testTag("add_task_button")
                ) {
                    Text("+ Add Task", color = JarvisCyanLight, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (tasks.isEmpty()) {
            item {
                Text("No pending tasks. Create one using '+ Add Task' or voice command.", color = JarvisTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            items(tasks) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.toggleTask(task) },
                            colors = CheckboxDefaults.colors(checkedColor = JarvisCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                color = if (task.isCompleted) JarvisTextSecondary else JarvisTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Category: ${task.category}",
                                color = JarvisTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = JarvisRedAlert, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(14.dp)) }
        }

        // Section: Notes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NOTES & OBSERVATIONS", fontWeight = FontWeight.Bold, color = JarvisGold, fontSize = 13.sp)
                Button(
                    onClick = { showAddNoteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCardDark),
                    modifier = Modifier.testTag("add_note_button")
                ) {
                    Text("+ Add Note", color = JarvisGold, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (notes.isEmpty()) {
            item {
                Text("No notes recorded yet.", color = JarvisTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = note.title, fontWeight = FontWeight.Bold, color = JarvisTextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = note.content, color = JarvisTextSecondary, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = JarvisRedAlert, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(14.dp)) }
        }

        // Section: Memory Vault
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PERSISTENT MEMORY VAULT", fontWeight = FontWeight.Bold, color = JarvisAccentBlue, fontSize = 13.sp)
                TextButton(
                    onClick = { viewModel.clearAllMemories() },
                    modifier = Modifier.testTag("clear_memories_button")
                ) {
                    Text("Clear All", color = JarvisRedAlert, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (memories.isEmpty()) {
            item {
                Text("Memory vault is empty. Tell JARVIS 'remember <fact>' to persist.", color = JarvisTextSecondary, fontSize = 12.sp)
            }
        } else {
            items(memories) { mem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = mem.key, fontWeight = FontWeight.SemiBold, color = JarvisCyanLight, fontSize = 12.sp)
                            Text(text = mem.value, color = JarvisTextPrimary, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.deleteMemory(mem.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = JarvisRedAlert, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("General") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("New Task", color = JarvisCyan) },
            text = {
                Column {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addTask(taskTitle, category)
                        }
                        showAddTaskDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                ) {
                    Text("Add", color = JarvisNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            },
            containerColor = JarvisSurfaceDark
        )
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("New Note", color = JarvisGold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            viewModel.addNote(noteTitle, noteContent)
                        }
                        showAddNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisGold)
                ) {
                    Text("Save", color = JarvisNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            },
            containerColor = JarvisSurfaceDark
        )
    }
}

@Composable
fun AgentVisionTab(viewModel: MainViewModel, uiState: MainUiState) {
    var goalInput by remember { mutableStateOf("Prepare morning routine and check battery") }
    var visionPrompt by remember { mutableStateOf("Inspect scene and identify objects") }
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> if (bitmap != null) viewModel.analyzeVisionBitmap(bitmap, visionPrompt) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Bounded Agent Planner Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("BOUNDED AUTONOMOUS AGENT", fontWeight = FontWeight.Bold, color = JarvisCyan, fontSize = 13.sp)
                    Text("Executes multi-step plans with strict budget <= 5 steps and pre-step emergency stop verification.", color = JarvisTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        label = { Text("Agent Goal") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.executeTextCommand("agent $goalInput") },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        modifier = Modifier.fillMaxWidth().testTag("execute_agent_goal_button")
                    ) {
                        Text("Plan & Execute Autonomous Goal", color = JarvisNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Display current active plan
        uiState.currentAgentPlan?.let { plan ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisCardDark)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Plan: ${plan.goal}", fontWeight = FontWeight.Bold, color = JarvisGold, fontSize = 13.sp)
                        Text("Status: ${if (plan.isCompleted) "Completed" else "In Progress"} (Budget: ${plan.steps.size}/${plan.maxStepBudget})", color = JarvisTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        plan.steps.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("[Step ${step.id}]", fontWeight = FontWeight.Bold, color = JarvisCyanLight, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(step.description, color = JarvisTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(
                                    text = step.status.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (step.status) {
                                        StepStatus.COMPLETED -> JarvisGreenOk
                                        StepStatus.RUNNING -> JarvisCyan
                                        StepStatus.FAILED -> JarvisRedAlert
                                        StepStatus.SKIPPED -> JarvisTextSecondary
                                        StepStatus.PENDING -> JarvisGold
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Vision Module Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("OPTICAL VISION SENSOR", fontWeight = FontWeight.Bold, color = JarvisCyan, fontSize = 13.sp)
                    Text("Opens Android's camera for a real preview photo. Photo analysis is available only in Cloud privacy mode; Strict and Balanced modes never send photos to cloud AI.", color = JarvisTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        label = { Text("Vision Inspection Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { cameraPreviewLauncher.launch(null) },
                        enabled = uiState.privacyMode == PrivacyMode.CLOUD && !uiState.isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, disabledContainerColor = JarvisSurfaceDark),
                        modifier = Modifier.fillMaxWidth().testTag("analyze_vision_button")
                    ) {
                        Text(
                            if (uiState.privacyMode == PrivacyMode.CLOUD) "Take Photo & Analyze" else "Switch to Cloud mode to analyze photos",
                            color = if (uiState.privacyMode == PrivacyMode.CLOUD) JarvisNavyDark else JarvisTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    uiState.visionAnalysisResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        when (res) {
                            is com.example.core.vision.VisionResult.Success -> {
                                Text("Vision Result:", fontWeight = FontWeight.Bold, color = JarvisGreenOk, fontSize = 12.sp)
                                Text(res.description, color = JarvisTextPrimary, fontSize = 12.sp)
                            }
                            is com.example.core.vision.VisionResult.Error -> {
                                Text("Vision Error: ${res.message}", color = JarvisRedAlert, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditIntegrationsTab(viewModel: MainViewModel) {
    val auditEvents by viewModel.auditEvents.collectAsStateWithLifecycle()
    val integrations by viewModel.integrations.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Integrations Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INTEGRATION ADAPTERS", fontWeight = FontWeight.Bold, color = JarvisCyan, fontSize = 13.sp)
                IconButton(onClick = { viewModel.refreshIntegrations() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(integrations) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = if (item.isAvailable) JarvisCyan else JarvisTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary, fontSize = 13.sp)
                        Text(item.description, color = JarvisTextSecondary, fontSize = 11.sp)
                        Text(item.statusSummary, color = if (item.isAvailable) JarvisGreenOk else JarvisGold, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text("SECURITY AUDIT & EVENT LOG", fontWeight = FontWeight.Bold, color = JarvisGold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (auditEvents.isEmpty()) {
            item {
                Text("No security audit events logged yet.", color = JarvisTextSecondary, fontSize = 12.sp)
            }
        } else {
            items(auditEvents) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${event.formattedTime} [${event.source}]",
                                fontSize = 10.sp,
                                color = JarvisTextSecondary
                            )
                            Text(
                                text = "Risk: ${event.riskLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (event.riskLevel) {
                                    "HIGH" -> JarvisRedAlert
                                    "MEDIUM" -> JarvisGold
                                    else -> JarvisGreenOk
                                }
                            )
                        }
                        Text(
                            text = event.action,
                            fontWeight = FontWeight.Bold,
                            color = if (event.success) JarvisCyanLight else JarvisRedAlert,
                            fontSize = 12.sp
                        )
                        Text(
                            text = event.details,
                            color = JarvisTextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
