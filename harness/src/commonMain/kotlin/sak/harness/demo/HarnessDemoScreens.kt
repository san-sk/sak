package sak.harness.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import sak.runtime.SakRuntime
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.feature.FeatureStatus

/** Registers the Messaging demo routes on the shared SAK NavHost. */
fun NavGraphBuilder.harnessDemoNavigation(runtimeRef: HarnessDemoRuntimeRef, onBack: () -> Unit) {
    composable("messages/home") { ChatConversationScreen(onBack) }
}

private fun getMessagesFeature(): HarnessDemoFeature? =
    HarnessDemoRuntimeRef.runtime?.getFeatureOrNull(FeatureId("messages")) as? HarnessDemoFeature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(onBack: () -> Unit) {
    val feature = getMessagesFeature()
    if (feature == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Messages") }) }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Messages module is disabled in settings.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    val messages by feature.messages.collectAsState()
    val unreadCount by feature.unreadCount.collectAsState()
    var inputText by remember { mutableStateOf("") }

    // Automatically mark messages as read when viewing this screen
    LaunchedEffect(Unit) {
        feature.markAllAsRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sarah Connor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Active Now • SAK Messaging Plugin", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { feature.markAllAsRead() }) {
                            Text("Mark Read")
                        }
                    }
                    TextButton(onClick = { 
                        feature.receiveIncomingMessage("Sarah", "Hey! New automated alert for testing!") 
                    }) {
                        Text("+ Simulate Input")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                feature.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "💡 Tap '+ Simulate Input' above to simulate a background message. SAK will automatically set 'Attention.High' and trigger a red badge on the bottom bar!",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(messages) { msg ->
                MessageBubble(msg)
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (msg.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (msg.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (msg.isFromMe) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (!msg.isFromMe) {
                    Text(msg.sender, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    msg.text, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    msg.timestamp, 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (msg.isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboardScreen(runtime: SakRuntime, onBack: () -> Unit) {
    val statuses by runtime.allStatuses.collectAsState()
    val activeConfig by runtime.activeConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAK System Observability") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Architecture Health Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active Plugins: ${statuses.size}", style = MaterialTheme.typography.bodySmall)
                            Text("Config v${activeConfig.version}", style = MaterialTheme.typography.bodySmall)
                            Text("Engine: ONLINE", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Mounted Feature Modules (${statuses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(statuses.toList()) { (id, status) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(id.value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = when (status.state) {
                                is FeatureState.Ready -> "READY"
                                is FeatureState.Initializing -> "INIT"
                                is FeatureState.Error -> "ERROR"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
