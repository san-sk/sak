package sak.sdk.navigation

import kotlin.jvm.JvmInline

/**
 * A type-safe wrapper for a route string in the shared NavHost (e.g. `analytics/today`, `scaffold/home`).
 */
@JvmInline
value class Route(val value: String)
