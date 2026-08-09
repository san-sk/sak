package sak.runtime.bundle

import sak.sdk.bundle.FeatureModule

/** iOS/Native: no ServiceLoader — pass bundled [FeatureModule] objects from the host. */
actual fun discoverFeatureModules(manualModules: List<FeatureModule>): List<FeatureModule> =
    mergeFeatureModules(emptyList(), manualModules)
