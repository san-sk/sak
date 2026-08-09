package sak.runtime.bundle

import sak.sdk.bundle.FeatureModule

/**
 * Discovers [FeatureModule] implementations bundled in the host app.
 *
 * Android: ServiceLoader reads `META-INF/services/sak.sdk.bundle.FeatureModule`.
 * iOS: pass module objects via [manualModules].
 */
expect fun discoverFeatureModules(manualModules: List<FeatureModule> = emptyList()): List<FeatureModule>

internal fun mergeFeatureModules(
    discovered: List<FeatureModule>,
    manual: List<FeatureModule>,
): List<FeatureModule> {
    val byId = mutableMapOf<String, FeatureModule>()
    discovered.forEach { module -> byId[module.id.value] = module }
    manual.forEach { module -> byId[module.id.value] = module }
    return byId.values.toList()
}
