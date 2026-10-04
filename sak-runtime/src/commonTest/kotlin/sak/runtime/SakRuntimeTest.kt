package sak.runtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import sak.sdk.config.FeatureAccess
import sak.sdk.config.SakConfig
import sak.sdk.entry.EntryAction
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.license.AllFeaturesLicensed
import sak.sdk.license.LicensedFeatures
import sak.sdk.license.NoFeaturesLicensed
import sak.sdk.navigation.Route
import sak.sdk.service.FeatureService
import sak.sdk.service.featureService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import sak.sdk.service.get
import kotlin.test.assertNull
import kotlin.test.assertTrue

interface DummyService {
    fun ping(): String
}

class TestFeature(
    override val id: FeatureId = FeatureId("test-feature")
) : BaseFeature() {
    var created = false
    var destroyed = false
    var configChanged = false
    var lastAccess: FeatureAccess? = null

    override fun services(): Set<FeatureService<*>> = setOf(
        featureService(DummyService::class, object : DummyService {
            override fun ping() = "pong"
        })
    )

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        created = true
        lastAccess = context.access
        updateStatus(FeatureState.Ready)
        setEntryPoints(
            listOf(
                EntryPoint(
                    id = EntryPointId("test-card"),
                    title = "Test Card",
                    action = EntryAction.Navigate("test-feature/home"),
                )
            )
        )
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {
        lastAccess = access
        configChanged = true
    }

    override suspend fun onDestroy() {
        super.onDestroy()
        destroyed = true
    }
}

class TestHostLifecycle : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: Lifecycle get() = registry

    fun start() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun pause() {
        registry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.CREATED
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SakRuntimeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun runtime_activatesEnabledFeatures() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = mapOf("test-feature" to FeatureAccess(enabled = true))),
            featureRegistry = FeatureRegistry.of(feature),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        assertTrue(feature.created)
        assertEquals(feature, runtime.getFeatureOrNull(FeatureId("test-feature")))
        assertEquals(true, feature.lastAccess?.enabled)

        val activeFeature = runtime.getFeature(FeatureId("test-feature"))
        assertNotNull(activeFeature)

        val dummyService = feature.featureContext.services.get<DummyService>()
        assertEquals("pong", dummyService?.ping())

        runtime.stop()
    }

    @Test
    fun runtime_skipsDisabledFeatures() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = mapOf("test-feature" to FeatureAccess(enabled = false))),
            featureRegistry = FeatureRegistry.of(feature),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        assertTrue(!feature.created)
        assertNull(runtime.getFeatureOrNull(FeatureId("test-feature")))
        assertNull(runtime.getFeature(FeatureId("test-feature")))

        runtime.stop()
    }

    @Test
    fun runtime_defaultsToEnabledWhenFeatureAbsentFromConfig() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = emptyMap()),
            featureRegistry = FeatureRegistry.of(feature),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        assertTrue(feature.created)

        runtime.stop()
    }

    @Test
    fun runtime_setFeatureEnabledDestroysAndRecreatesFeature() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = mapOf("test-feature" to FeatureAccess(enabled = true))),
            featureRegistry = FeatureRegistry.of(feature),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()
        assertTrue(feature.created)

        runtime.setFeatureEnabled("test-feature", false)
        assertTrue(feature.destroyed)
        assertNull(runtime.getFeatureOrNull(FeatureId("test-feature")))

        runtime.setFeatureEnabled("test-feature", true)
        assertNotNull(runtime.getFeatureOrNull(FeatureId("test-feature")))

        runtime.stop()
    }

    @Test
    fun runtime_aggregatesEntryPointsFromEnabledFeatures() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = mapOf("test-feature" to FeatureAccess(enabled = true))),
            featureRegistry = FeatureRegistry.of(feature),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        val entries = runtime.allEntryPoints.value
        assertEquals(1, entries.size)
        assertEquals("Test Card", entries.first().second.title)
        assertEquals(FeatureId("test-feature"), entries.first().first)

        runtime.stop()
    }

    @Test
    fun runtime_updateConfigAccumulatesOverlays() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val alpha = TestFeature(FeatureId("alpha"))
        val beta = TestFeature(FeatureId("beta"))

        val runtime = SakRuntime(
            baseConfig = SakConfig(
                features = mapOf(
                    "alpha" to FeatureAccess(enabled = false),
                    "beta" to FeatureAccess(enabled = false),
                )
            ),
            featureRegistry = FeatureRegistry.of(alpha, beta),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()
        assertTrue(!alpha.created)
        assertTrue(!beta.created)

        // A partial overlay must not discard previously applied overlay changes.
        runtime.updateConfig(SakConfig(features = mapOf("alpha" to FeatureAccess(enabled = true))))
        assertTrue(alpha.created)
        assertTrue(!beta.created)

        runtime.updateConfig(SakConfig(features = mapOf("beta" to FeatureAccess(enabled = true))))
        assertTrue(beta.created)
        assertTrue(runtime.activeConfig.value.isFeatureEnabled("alpha"))
        assertTrue(runtime.activeConfig.value.isFeatureEnabled("beta"))

        runtime.stop()
    }

    @Test
    fun runtime_forwardsNavigationRequestsViaChannel() = runTest(testDispatcher) {
        val host = TestHostLifecycle()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = emptyMap()),
            featureRegistry = FeatureRegistry.of(TestFeature()),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        val eventCollection = async { runtime.navigationEvents.first() }

        runtime.navigate(Route("test-feature/home"))

        val result = eventCollection.await()
        assertEquals("test-feature/home", result.value)

        runtime.stop()
    }

    @Test
    fun featureLifecycle_followsHostLifecycle() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = mapOf("test-feature" to FeatureAccess(enabled = true))),
            featureRegistry = FeatureRegistry.of(feature),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()
        assertEquals(Lifecycle.State.RESUMED, feature.lifecycle.currentState)

        host.pause()
        assertEquals(Lifecycle.State.STARTED, feature.lifecycle.currentState)

        host.stop()
        assertEquals(Lifecycle.State.CREATED, feature.lifecycle.currentState)

        host.start()
        assertEquals(Lifecycle.State.RESUMED, feature.lifecycle.currentState)

        runtime.stop()
        assertTrue(feature.destroyed)
    }

    @Test
    fun runtime_skipsUnlicensedFeatures() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = emptyMap()),
            featureRegistry = FeatureRegistry.of(feature),
            entitlements = LicensedFeatures(setOf(FeatureId("other-feature"))),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        assertTrue(!feature.created)
        assertTrue(!runtime.isActive(FeatureId("test-feature")))
        assertNull(runtime.getFeatureOrNull(FeatureId("test-feature")))
        assertNull(runtime.getFeature(FeatureId("test-feature")))

        runtime.stop()
    }

    @Test
    fun runtime_activatesLicensedFeatures() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = emptyMap()),
            featureRegistry = FeatureRegistry.of(feature),
            entitlements = LicensedFeatures(setOf(FeatureId("test-feature"))),
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        assertTrue(feature.created)
        assertTrue(runtime.isActive(FeatureId("test-feature")))

        runtime.stop()
    }

    @Test
    fun runtime_updateEntitlementsActivatesAndDeactivatesFeatures() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = emptyMap()),
            featureRegistry = FeatureRegistry.of(feature),
            entitlements = NoFeaturesLicensed,
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()
        assertTrue(!feature.created)

        // Granting a license must bring the feature up...
        runtime.updateEntitlements(LicensedFeatures(setOf(FeatureId("test-feature"))))
        assertTrue(feature.created)
        assertTrue(runtime.isActive(FeatureId("test-feature")))

        // ...and revoking it must tear the feature back down.
        runtime.updateEntitlements(NoFeaturesLicensed)
        assertTrue(feature.destroyed)
        assertTrue(!runtime.isActive(FeatureId("test-feature")))

        runtime.stop()
    }

    @Test
    fun runtime_isActiveRequiresAllThreeGates() = runTest(testDispatcher) {
        val host = TestHostLifecycle()
        val feature = TestFeature()

        val runtime = SakRuntime(
            baseConfig = SakConfig(features = emptyMap()),
            featureRegistry = FeatureRegistry.of(feature),
            entitlements = AllFeaturesLicensed,
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        // Available and licensed, absent from config so enabled by default.
        assertTrue(runtime.isActive(FeatureId("test-feature")))

        // Disabled by config: the config gate closes.
        runtime.setFeatureEnabled("test-feature", false)
        assertTrue(!runtime.isActive(FeatureId("test-feature")))

        // Never registered: the availability gate stays closed regardless of license.
        assertTrue(!runtime.isActive(FeatureId("not-registered")))

        runtime.stop()
    }
}