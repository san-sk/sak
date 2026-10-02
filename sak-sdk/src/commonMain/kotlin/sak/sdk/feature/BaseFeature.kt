package sak.sdk.feature

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.EntryPoint

/**
 * Optional base implementation of [Feature] that handles common boilerplate.
 */
abstract class BaseFeature : Feature {

    private var _context: FeatureContext? = null

    /**
     * The current feature context. Available after [onCreate].
     */
    val featureContext: FeatureContext
        get() = _context ?: error("Feature $id: context requested before onCreate or after onDestroy")

    protected val context: FeatureContext
        get() = featureContext

    protected val _status = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = _status.asStateFlow()

    protected val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = _entryPoints.asStateFlow()

    override val lifecycle: Lifecycle
        get() = context.lifecycle

    /**
     * Managed scope for this feature.
     */
    val featureScope: CoroutineScope
        get() = context.coroutineScope

    override suspend fun onCreate(context: FeatureContext) {
        this._context = context
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {}

    override suspend fun onDestroy() {
        _entryPoints.value = emptyList()
        this._context = null
    }

    /** Helper to check if a grant is present in the current context. */
    protected fun hasGrant(grant: String): Boolean = context.hasGrant(grant)

    /** Helper to observe transitions of a grant status. */
    protected fun observeGrant(grant: String): Flow<Boolean> = context.observeGrant(grant)

    /**
     * Update the feature state (Ready, Initializing, Error).
     */
    protected fun updateStatus(state: FeatureState) {
        _status.value = _status.value.copy(state = state)
    }

    /**
     * Update the attention level (None, Info, Warning, Critical).
     */
    protected fun updateAttention(attention: Attention) {
        _status.value = _status.value.copy(attention = attention)
    }

    /**
     * Update the list of published entry points.
     */
    protected fun setEntryPoints(list: List<EntryPoint>) {
        _entryPoints.value = list
    }
}
