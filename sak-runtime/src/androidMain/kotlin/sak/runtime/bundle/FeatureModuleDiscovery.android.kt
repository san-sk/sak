package sak.runtime.bundle

import java.util.ServiceLoader
import sak.sdk.bundle.FeatureModule

/** Android: discovers modules via Java [ServiceLoader] + optional manual list. */
actual fun discoverFeatureModules(manualModules: List<FeatureModule>): List<FeatureModule> {
    val fromServiceLoader = runCatching {
        ServiceLoader.load(FeatureModule::class.java, FeatureModule::class.java.classLoader)
            .toList()
    }.getOrDefault(emptyList())

    return mergeFeatureModules(fromServiceLoader, manualModules)
}
