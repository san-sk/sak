package sak.sdk.config

import kotlinx.serialization.Serializable

/**
 * Root JSON configuration structure for SAK framework runtime.
 *
 * Maps feature IDs to their corresponding [FeatureAccess] entitlement snapshot.
 */
@Serializable
data class SakConfig(
    /** Schema/payload version. */
    val version: Int = 1,
    /** Map of feature IDs to entitlement access definitions. */
    val features: Map<String, FeatureAccess> = emptyMap(),
)
