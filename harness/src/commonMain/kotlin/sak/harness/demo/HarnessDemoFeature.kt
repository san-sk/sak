package sak.harness.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.entry.EntryPointType
import sak.sdk.entry.EntryStyle
import sak.sdk.feature.Attention
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
)

/**
 * Real-world Chat & Messaging Feature Module.
 *
 * Demonstrates:
 * 1. Maintaining internal state flows for live messaging.
 * 2. Signaling the Host App via `updateAttention(Attention.Medium)` when an unread message arrives.
 * 3. Exposing a Micro-Frontend `EntryPoint` to render a widget on the Home Dashboard.
 */
class HarnessDemoFeature : BaseFeature() {
    override val id = FeatureId("messages")

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("1", "Sarah", "Hey! Is the SAK architecture update ready?", "10:42 AM", false),
            ChatMessage("2", "You", "Yes! It now supports concurrent execution and value classes.", "10:43 AM", true),
            ChatMessage("3", "Sarah", "Awesome! I will review the PR.", "10:45 AM", false),
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(1)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        publishEntries()
        updateStatus(FeatureState.Ready)
        
        // Signal medium attention for the initial unread message
        updateAttention(Attention.Medium)
    }

    /** Simulates receiving an incoming message from a remote contact. */
    fun receiveIncomingMessage(sender: String, text: String) {
        val nextId = (_messages.value.size + 1).toString()
        val newMessage = ChatMessage(nextId, sender, text, "Just now", false)
        _messages.value = _messages.value + newMessage
        _unreadCount.value += 1
        
        // Signal high attention to trigger the red badge on the Host App's bottom navigation
        updateAttention(Attention.High)
        publishEntries()
    }

    /** User sends a message. */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val nextId = (_messages.value.size + 1).toString()
        val newMessage = ChatMessage(nextId, "You", text, "Just now", true)
        _messages.value = _messages.value + newMessage
    }

    /** Clears unread badge. */
    fun markAllAsRead() {
        _unreadCount.value = 0
        updateAttention(Attention.None)
        publishEntries()
    }

    private fun publishEntries() {
        val count = _unreadCount.value
        setEntryPoints(
            listOf(
                EntryPoint(
                    id = EntryPointId("messages-dashboard-card"),
                    type = EntryPointType.Dashboard,
                    style = EntryStyle.Card,
                    action = EntryAction.Navigate("messages/home"),
                    priority = 85,
                    title = "Recent Messages",
                    subtitle = _messages.value.lastOrNull()?.let { "${it.sender}: ${it.text}" } ?: "No messages",
                    icon = "chat_bubble",
                    badge = if (count > 0) "$count UNREAD" else null,
                )
            )
        )
    }
}
