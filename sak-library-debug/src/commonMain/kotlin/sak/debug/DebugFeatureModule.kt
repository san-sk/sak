package sak.debug

import sak.sdk.bundle.FeatureModule
import sak.sdk.bundle.SakDependencies
import sak.sdk.bundle.SakModule
import sak.sdk.feature.Feature
import sak.sdk.feature.FeatureId

@SakModule
object DebugFeatureModule : FeatureModule {
    override val id = FeatureId("sak-debug")

    override fun create(deps: SakDependencies): Feature {
        return DebugFeature()
    }
}
