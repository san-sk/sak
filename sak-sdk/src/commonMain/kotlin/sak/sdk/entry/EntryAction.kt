package sak.sdk.entry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What happens when the user taps an entry point widget. */
@Serializable
sealed class EntryAction {
    /** Open a route in the host NavHost (e.g. `analytics/today`). */
    @Serializable
    @SerialName("navigate")
    data class Navigate(val route: String) : EntryAction()

    /** Open an external or app deep link URI. */
    @Serializable
    @SerialName("deepLink")
    data class DeepLink(val uri: String) : EntryAction()

    /** Display-only widget; tap does nothing. */
    @Serializable
    @SerialName("none")
    data object None : EntryAction()
}
