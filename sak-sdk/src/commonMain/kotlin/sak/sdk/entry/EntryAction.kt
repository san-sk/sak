package sak.sdk.entry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Activation behavior when the user taps or selects an [EntryPoint].
 */
@Serializable
sealed class EntryAction {
    /** Navigate to an in-app route in the shared NavHost. */
    @Serializable
    @SerialName("navigate")
    data class Navigate(val route: String) : EntryAction()

    /** Open a deep link URI (host handles platform-specific routing). */
    @Serializable
    @SerialName("deepLink")
    data class DeepLink(val uri: String) : EntryAction()

    /** Dispatch a host- or feature-defined command name. */
    @Serializable
    @SerialName("command")
    data class Command(val name: String) : EntryAction()

    /** Display-only — no activation handler. */
    @Serializable
    @SerialName("none")
    data object None : EntryAction()
}
