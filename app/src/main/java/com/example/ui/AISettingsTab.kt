package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ai.provider.ConnectionState
import com.example.core.ai.provider.ProviderCapability
import com.example.core.ai.provider.ProviderType
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

@Composable
fun AISettingsTab(viewModel: MainViewModel) {
    val selectedProviderType by viewModel.selectedProviderType.collectAsStateWithLifecycle()
    val configurations by viewModel.providerConfigurations.collectAsStateWithLifecycle()
    val statuses by viewModel.providerConnectionStatuses.collectAsStateWithLifecycle()

    val currentProvider = viewModel.aiProviderManager.getProvider(selectedProviderType)
    val currentConfig = configurations[selectedProviderType]
    val currentStatus = statuses[selectedProviderType]

    var enteredKey by remember(selectedProviderType) { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var customEndpoint by remember(selectedProviderType) {
        mutableStateOf(currentConfig?.customEndpoint ?: currentProvider?.descriptor?.defaultEndpoint ?: "")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            Text(
                text = "BRING YOUR OWN API KEY (BYOK) SETTINGS",
                fontWeight = FontWeight.Bold,
                color = JarvisCyan,
                fontSize = 13.sp
            )
            Text(
                text = "Keys are securely encrypted using Android Keystore (AES-256 GCM). They are never sent to third-party tracking or saved in plaintext.",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Provider Selector Chips
        item {
            Text("SELECT AI PROVIDER:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisGold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProviderType.values().forEach { type ->
                    FilterChip(
                        selected = selectedProviderType == type,
                        onClick = { viewModel.setSelectedProvider(type) },
                        label = { Text(type.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JarvisCyan,
                            selectedLabelColor = JarvisNavyDark,
                            labelColor = JarvisTextSecondary
                        ),
                        modifier = Modifier.testTag("provider_chip_${type.name}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Active Provider Configuration Card
        currentProvider?.let { provider ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = provider.descriptor.type.displayName,
                                fontWeight = FontWeight.Bold,
                                color = JarvisTextPrimary,
                                fontSize = 15.sp
                            )
                            if (viewModel.hasProviderApiKey(provider.descriptor.type)) {
                                Text(
                                    text = "KEY CONFIGURED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisGreenOk
                                )
                            } else if (provider.descriptor.requiresApiKey) {
                                Text(
                                    text = "NO KEY ENTERED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisRedAlert
                                )
                            } else {
                                Text(
                                    text = "NO KEY REQUIRED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyanLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Capability Indicators
                        Text("CAPABILITIES:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = JarvisTextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeModelId = currentConfig?.selectedModelId ?: provider.descriptor.defaultModelId
                            ProviderCapability.values().forEach { cap ->
                                val supported = provider.supportsCapability(cap, activeModelId)
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (supported) JarvisCardDark else JarvisNavyDark
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = cap.name.replace("_", " "),
                                        fontSize = 9.sp,
                                        color = if (supported) JarvisCyanLight else JarvisTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Model Selection
                        Text("SELECT MODEL:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        provider.descriptor.supportedModels.forEach { model ->
                            val isSelected = (currentConfig?.selectedModelId ?: provider.descriptor.defaultModelId) == model.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        viewModel.updateProviderConfig(
                                            (currentConfig ?: com.example.core.ai.provider.ProviderConfiguration(provider.descriptor.type, model.id))
                                                .copy(selectedModelId = model.id)
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) JarvisCardDark else JarvisNavyDark
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(model.displayName, color = if (isSelected) JarvisCyanLight else JarvisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("ID: ${model.id}", color = JarvisTextSecondary, fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = JarvisCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        // Custom Endpoint (if allowed)
                        if (provider.descriptor.allowsCustomEndpoint) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("CUSTOM HTTPS ENDPOINT:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customEndpoint,
                                onValueChange = {
                                    customEndpoint = it
                                    viewModel.updateProviderConfig(
                                        (currentConfig ?: com.example.core.ai.provider.ProviderConfiguration(provider.descriptor.type, provider.descriptor.defaultModelId))
                                            .copy(customEndpoint = it)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("custom_endpoint_field"),
                                shape = RoundedCornerShape(8.dp),
                                placeholder = { Text("https://your-custom-endpoint/v1/", fontSize = 11.sp) }
                            )
                        }

                        // API Key Entry (if required)
                        if (provider.descriptor.requiresApiKey) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("API KEY ENTRY:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = JarvisTextSecondary)
                            if (viewModel.hasProviderApiKey(provider.descriptor.type)) {
                                Text(
                                    text = "Current saved key: ${viewModel.getMaskedProviderApiKey(provider.descriptor.type)}",
                                    color = JarvisGold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = enteredKey,
                                onValueChange = { enteredKey = it },
                                label = { Text("Enter / Replace API Key") },
                                modifier = Modifier.fillMaxWidth().testTag("api_key_input_field"),
                                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle key visibility"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (enteredKey.isNotBlank()) {
                                            viewModel.saveProviderApiKey(provider.descriptor.type, enteredKey)
                                            enteredKey = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                                    modifier = Modifier.weight(1f).testTag("save_api_key_button")
                                ) {
                                    Text("Save Key", color = JarvisNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (viewModel.hasProviderApiKey(provider.descriptor.type)) {
                                    OutlinedButton(
                                        onClick = { viewModel.removeProviderApiKey(provider.descriptor.type) },
                                        modifier = Modifier.weight(1f).testTag("remove_api_key_button")
                                    ) {
                                        Text("Remove Key", color = JarvisRedAlert, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Connection Button
                        Button(
                            onClick = { viewModel.testProviderConnection(provider.descriptor.type) },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCardDark),
                            modifier = Modifier.fillMaxWidth().testTag("test_connection_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Connection", color = JarvisCyanLight, fontSize = 12.sp)
                        }

                        // Connection Status Banner
                        currentStatus?.let { status ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (status.state) {
                                        ConnectionState.CONNECTED -> JarvisCardDark
                                        ConnectionState.CHECKING -> JarvisCardDark
                                        ConnectionState.NOT_CONFIGURED -> JarvisCardDark
                                        else -> JarvisCardDark
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (status.state) {
                                        ConnectionState.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = JarvisCyan)
                                        ConnectionState.CONNECTED -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = JarvisGreenOk, modifier = Modifier.size(16.dp))
                                        ConnectionState.NOT_CONFIGURED -> Icon(Icons.Default.Key, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(16.dp))
                                        else -> Icon(Icons.Default.Error, contentDescription = null, tint = JarvisRedAlert, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = status.message,
                                        fontSize = 11.sp,
                                        color = when (status.state) {
                                            ConnectionState.CONNECTED -> JarvisGreenOk
                                            ConnectionState.NOT_CONFIGURED -> JarvisGold
                                            else -> JarvisRedAlert
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
