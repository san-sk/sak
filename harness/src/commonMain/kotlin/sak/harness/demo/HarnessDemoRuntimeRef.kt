package sak.harness.demo

import sak.runtime.SakRuntime

/** Set once by [sak.harness.HarnessApp] after launch so demo screens can read runtime StateFlows. */
class HarnessDemoRuntimeRef(
    val onToggleFeature: (String, Boolean) -> Unit,
    val isFeatureEnabled: (String) -> Boolean,
) {
    companion object {
        var runtime: SakRuntime? = null
    }
}
