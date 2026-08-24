package sak.sdk.config

import kotlinx.serialization.json.Json

/**
 * JSON codec for [AppConfig] and related `@Serializable` entitlement types.
 *
 * Ignores unknown keys so older apps tolerate newer config fields.
 */
val ConfigJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
