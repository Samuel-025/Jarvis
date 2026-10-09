package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.agent.StepStatus
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Console, 1: OS (Tasks/Notes), 2: Agent & Vision, 3: Audit & Integrations

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisNavyDark,
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurfaceDark,
                contentColor = JarvisCyan
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Console") },
                    label = { Text("Console") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCardDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Note, contentDescription = "Personal OS") },
                    label = { Text("Personal OS") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCardDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Assistant, contentDescription = "Agent") },
                    label = { Text("Agent") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCardDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Audit & Status") },
                    label = { Text("Audit") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCardDark
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Extension, contentDescription = "AI Settings") },
                    label = { Text("AI Config") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisCyan,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCardDark
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Status & Safety Bar
            TopSafetyHeader(
                uiState = uiState,
                onEmergencyStop = { viewModel.triggerEmergencyStop() },
                onResetEmergencyStop = { viewModel.resetEmergencyStop() },
                onPrivacySelected = { viewModel.setPrivacyMode(it) }
            )

            // Confirmation Dialog if an action requires explicit user confirmation
            uiState.pendingConfirmation?.let { req ->
                AlertDialog(
                    onDismissRequest = { viewModel.rejectPendingAction() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = JarvisGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmation Required", color = JarvisGold)
                        }
                    },
                    text = {
                        Text(
                            text = req.prompt,
                            color = JarvisTextPrimary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmPendingAction() },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            modifier = Modifier.testTag("confirm_action_button")
                        ) {
                            Text("Approve", color = JarvisNavyDark, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.rejectPendingAction() },
                            modifier = Modifier.testTag("reject_action_button")
                        ) {
                            Text("Reject", color = JarvisRedAlert)
                        }
                    },
                    containerColor = JarvisSurfaceDark
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ConsoleTab(viewModel, uiState)
                    1 -> PersonalOsTab(viewModel)
                    2 -> AgentVisionTab(viewModel, uiState)
                    3 -> AuditIntegrationsTab(viewModel)
                    4 -> AISettingsTab(viewModel)
                }
            }
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
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Voice State / Status indicator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice State: ${uiState.voiceState.name}",
                        fontWeight = FontWeight.SemiBold,
                        color = when (uiState.voiceState) {
                            VoiceState.LISTENING -> JarvisCyan
                            VoiceState.SPEAKING -> JarvisGold
                            VoiceState.ERROR -> JarvisRedAlert
                            VoiceState.PROCESSING -> JarvisAccentBlue
                            VoiceState.IDLE -> JarvisTextSecondary
                        },
                        fontSize = 13.sp
                    )
                    if (uiState.lastRecognizedSpeech.isNotBlank()) {
                        Text(
                            text = "\"${uiState.lastRecognizedSpeech}\"",
                            color = JarvisTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    if (uiState.voiceErrorMessage != null) {
                        Text(
                            text = uiState.voiceErrorMessage ?: "",
                            color = JarvisRedAlert,
                            fontSize = 11.sp
                        )
                    }
                }

                // Mic FAB
                FloatingActionButton(
                    onClick = {
                        if (uiState.voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceListening()
                        } else {
                            viewModel.startVoiceListening()
                        }
                    },
                    containerColor = if (uiState.voiceState == VoiceState.LISTENING) JarvisRedAlert else JarvisCyan,
                    contentColor = JarvisNavyDark,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_mic_fab")
                ) {
                    Icon(
                        imageVector = if (uiState.voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone Toggle"
                    )
                }
            }
        }

        // Quick Command Chips
        Text(
            text = "QUICK COMMANDS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = JarvisTextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickCommands = listOf("Battery", "Flashlight on", "Flashlight off", "Volume up", "Time")
            quickCommands.forEach { cmd ->
                Card(
                    modifier = Modifier
                        .clickable { viewModel.executeTextCommand(cmd) }
                        .testTag("quick_command_$cmd"),
                    colors = CardDefaults.cardColors(containerColor = JarvisCardDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = cmd,
                        fontSize = 11.sp,
                        color = JarvisCyanLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Response / Execution Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Text(
                    text = "COMMAND EXECUTION OUTPUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Processing command with safety verification...", color = JarvisTextSecondary, fontSize = 13.sp)
                    }
                } else if (uiState.lastCommandResult != null) {
                    when (val res = uiState.lastCommandResult) {
                        is CommandResult.Success -> {
                            Text(
                                text = res.message,
                                color = JarvisCyanLight,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            if (res.details.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                res.details.forEach { (k, v) ->
                                    Text("• $k: $v", color = JarvisTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                        is CommandResult.Error -> {
                            Text(
                                text = "ERROR: ${res.message}",
                                color = JarvisRedAlert,
                                fontSize = 14.sp
                            )
                            if (res.recoverySuggestion != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Suggestion: ${res.recoverySuggestion}", color = JarvisGold, fontSize = 12.sp)
                            }
                        }
                        is CommandResult.RequiresConfirmation -> {
                            Text(
                                text = "Awaiting Confirmation: ${res.confirmationPrompt}",
                                color = JarvisGold,
                                fontSize = 14.sp
                            )
                        }
                        null -> {}
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Awaiting command input.\nSpeak or enter a command below.",
                            color = JarvisTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Text input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask JARVIS or type command...", color = JarvisTextSecondary, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.executeTextCommand(textInput)
                    textInput = ""
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = JarvisNavyDark
                )
            }
        }
    }
}

@Composable
fun PersonalOsTab(viewModel: MainViewModel) {
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
                    Text("Captures and analyzes sensor frames through VisionProvider.", color = JarvisTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        label = { Text("Vision Inspection Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            // Synthesize test sensor bitmap frame (256x256 test pattern)
                            val bitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
                            viewModel.analyzeVisionBitmap(bitmap, visionPrompt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        modifier = Modifier.fillMaxWidth().testTag("analyze_vision_button")
                    ) {
                        Text("Capture Frame & Analyze", color = JarvisNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
