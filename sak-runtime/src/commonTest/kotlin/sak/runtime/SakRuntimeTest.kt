package sak.runtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import sak.sdk.config.FeatureAccess
import sak.sdk.config.RouteAccess
import sak.sdk.config.SakConfig
import sak.sdk.entry.EntryPoint
import sak.sdk.entry.EntryPointId
import sak.sdk.feature.BaseFeature
import sak.sdk.feature.FeatureContext
import sak.sdk.feature.FeatureId
import sak.sdk.feature.FeatureState
import sak.sdk.navigation.Route
import sak.sdk.service.FeatureService
import sak.sdk.service.featureService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    var lastAccess: FeatureAccess? = null

    override val routes = listOf(Route("test-feature/home"))

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
        setEntryPoints(listOf(EntryPoint(id = EntryPointId("test-card"), categories = setOf("home"))))
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {
        lastAccess = access
    }

    override suspend fun onDestroy() {
        super.onDestroy()
        destroyed = true
    }
}

class TestHostLifecycle : LifecycleOwner {
    private val observers = mutableListOf<LifecycleEventObserver>()
    var currentStateValue: Lifecycle.State = Lifecycle.State.INITIALIZED

    override val lifecycle: Lifecycle = object : Lifecycle() {
        override fun addObserver(observer: LifecycleObserver) {
            if (observer is LifecycleEventObserver) {
                observers.add(observer)
            }
        }

        override fun removeObserver(observer: LifecycleObserver) {
            observers.remove(observer)
        }

        override val currentState: State
            get() = currentStateValue
    }

    fun start() {
        currentStateValue = Lifecycle.State.STARTED
        observers.toList().forEach { it.onStateChanged(this, Lifecycle.Event.ON_START) }
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
        val registry = FeatureRegistry.of(feature)
        val defaultConfig = SakConfig(
            features = mapOf("test-feature" to FeatureAccess(enabled = true, plan = "pro"))
        )

        val runtime = SakRuntime(
            defaultConfig = defaultConfig,
            featureRegistry = registry,
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        assertTrue(feature.created)
        assertEquals(feature, runtime.getFeatureOrNull(FeatureId("test-feature")))
        assertEquals("pro", feature.lastAccess?.plan)

        val activeFeature = runtime.getFeature(FeatureId("test-feature"))
        assertNotNull(activeFeature)

        val dummyService = feature.featureContext.services.find(DummyService::class)
        assertEquals("pong", dummyService?.ping())

        runtime.stop()
    }

    @Test
    fun runtime_gatesNavigationForBlockedRoutes() = runTest(testDispatcher) {
        val host = TestHostLifecycle()

        val feature = TestFeature()
        val registry = FeatureRegistry.of(feature)
        val defaultConfig = SakConfig(
            features = mapOf(
                "test-feature" to FeatureAccess(
                    enabled = true,
                    routes = RouteAccess(blocked = setOf("test-feature/home"))
                )
            )
        )

        val runtime = SakRuntime(
            defaultConfig = defaultConfig,
            featureRegistry = registry,
            scope = backgroundScope,
            hostLifecycle = host.lifecycle
        )

        runtime.start()
        host.start()

        runtime.navigate(Route("test-feature/home"))
        assertNull(runtime.navigationRoute.value)

        runtime.stop()
    }
}
