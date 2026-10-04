package sak.sdk.feature

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sak.sdk.config.FeatureAccess
import sak.sdk.entry.EntryPoint

/**
 * An abstract base class for implementing [Feature]s that eliminates structural boilerplate.
 *
 * Developers should inherit from `BaseFeature` rather than implementing `Feature` directly.
 * It automatically manages the internal `LifecycleRegistry`, provides backing fields for the 
 * observable flows (`status`, `entryPoints`), and provides convenient setter methods.
 */
abstract class BaseFeature : Feature {

    private var internalContext: FeatureContext? = null

    /**
     * The mutable lifecycle registry specific to this feature module.
     * The SAK `FeatureManager` pushes Host OS events (like ON_PAUSE and ON_STOP) directly 
     * into this registry, ensuring the feature naturally syncs with the user's device state.
     */
    val registry = LifecycleRegistry.createUnsafe(this)

    /**
     * The active runtime context provided during [onCreate].
     * @throws IllegalStateException If accessed before [onCreate] completes or after [onDestroy] executes.
     */
    val featureContext: FeatureContext
        get() = internalContext ?: error("Feature ${id.value}: context requested before onCreate or after onDestroy")

    /** Convenient internal shorthand accessor for [featureContext]. */
    protected val context: FeatureContext
        get() = featureContext

    private val mutableStatus = MutableStateFlow(FeatureStatus())
    override val status: StateFlow<FeatureStatus> = mutableStatus.asStateFlow()

    private val mutableEntryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints: StateFlow<List<EntryPoint>> = mutableEntryPoints.asStateFlow()

    override val lifecycle: Lifecycle
        get() = registry

    /**
     * The CoroutineScope bound to this feature's lifecycle, automatically provided by the [context].
     */
    val featureScope: CoroutineScope
        get() = context.coroutineScope

    override suspend fun onCreate(context: FeatureContext) {
        this.internalContext = context
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {}

    override suspend fun onDestroy() {
        mutableEntryPoints.value = emptyList()
        this.internalContext = null
    }

    /**
     * Transitions the feature's health state.
     * @param state E.g., [FeatureState.Ready] or [FeatureState.Error].
     */
    protected fun updateStatus(state: FeatureState) {
        mutableStatus.value = mutableStatus.value.copy(state = state)
    }

    /**
     * Elevates or lowers the feature's urgency request.
     * @param attention E.g., [Attention.Medium] to request a notification badge from the host UI.
     */
    protected fun updateAttention(attention: Attention) {
        mutableStatus.value = mutableStatus.value.copy(attention = attention)
    }

    /**
     * Pushes a new list of `EntryPoint` UI descriptors to the Host App.
     * @param list The widgets the host app should render. Replaces the previously emitted list entirely.
     */
    protected fun setEntryPoints(list: List<EntryPoint>) {
        mutableEntryPoints.value = list
    }
}
