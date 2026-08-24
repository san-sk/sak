package sak.sdk.entry

import kotlinx.serialization.json.JsonElement

/**
 * Arbitrary entry-point content as `Map<String, JsonElement>`.
 *
 * Keys and structure are defined by the feature; the host/renderer interprets them.
 */
typealias EntryPayload = Map<String, JsonElement>
