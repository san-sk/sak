package sak.sdk.entry

/**
 * Feature-published UI descriptor — data only, not a Composable or View.
 *
 * The feature declares what exists; the host decides placement and rendering from [kind] and [payload].
 */
data class EntryPointId(val value: String)

/**
 * @property id Stable id within the feature (e.g. `summary-card`).
 * @property kind Widget shape hint for the host renderer.
 * @property payload Arbitrary JSON-compatible map — titles, icons, metrics, etc.
 * @property tags Semantic filters (`setup`, `alert`, …) — not tab or route names.
 * @property priority Higher values sort earlier when the host groups entries.
 * @property action What happens when the user activates this entry.
 */
data class EntryPoint(
    val id: EntryPointId,
    val kind: EntryKind,
    val payload: EntryPayload = emptyMap(),
    val tags: Set<String> = emptySet(),
    val priority: Int = 0,
    val action: EntryAction = EntryAction.None,
)
