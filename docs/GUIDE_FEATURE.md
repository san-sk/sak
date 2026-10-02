# Feature Developer Guide

This guide explains how to build a business feature module for the SAK platform.

---

## 1. The Core Interface
Your feature implements the `Feature` interface in `commonMain` (or optionally extends `BaseFeature`).

```kotlin
class MyAnalyticsFeature : Feature {
    override val id = FeatureId("analytics")
    
    private val _status = MutableStateFlow<FeatureStatus>(FeatureStatus())
    override val status = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints = _entryPoints.asStateFlow()

    override val routes = listOf(Route("analytics/today"))

    override suspend fun onCreate(context: FeatureContext) {
        publishEntries(context.access)
    }

    override suspend fun onConfigChanged(access: FeatureAccess) {
        publishEntries(access)
    }

    override suspend fun onDestroy() {
        _entryPoints.value = emptyList()
    }

    private fun publishEntries(access: FeatureAccess) {
        _entryPoints.value = listOf(
            EntryPoint(
                id = EntryPointId("summary-card"),
                type = "summary",
                style = EntryStyle.Card,
                action = EntryAction.Navigate("analytics/today"),
                priority = 10,
                categories = setOf("home")
            )
        )
    }
}
```

### 3 Clean Lifecycle Methods
- **`onCreate(context: FeatureContext)`**: Called when feature is instantiated and bound to host platform lifecycle.
- **`onConfigChanged(access: FeatureAccess)`**: Called when license overlays or entitlement grants update.
- **`onDestroy()`**: Called when feature is deactivated or runtime shuts down. Release long-lived resources.

---

## 2. Using the `FeatureContext`

The `FeatureContext` provided to your feature gives access to runtime capabilities:

- **`coroutineScope`**: Managed coroutine scope automatically cancelled when your feature stops.
- **`lifecycle`**: Platform lifecycle reference (`androidx.lifecycle.Lifecycle`).
- **`access`**: Current `FeatureAccess` snapshot (`enabled`, `plan`, `grants`, `attrs`).
- **`services`**: Unified `ServiceLookup` container for host services and cross-feature APIs.

```kotlin
// Example: Looking up a service provided by host or another feature
val logService = context.services.find(HarnessLogService::class)
logService?.log("Feature initialized")
```

---

## 3. Exporting Services / APIs

Features can export services for use by other features or the host app:

```kotlin
interface MyFeatureApi {
    fun doSomething()
}

class MyFeature : BaseFeature() {
    override fun services(): Set<FeatureService<*>> =
        setOf(featureService(MyFeatureApi::class, myApiProvider))
}
```

---

## 4. Feature Registration

Features are registered into `FeatureRegistry`:

```kotlin
val registry = FeatureRegistry.of(
    AnalyticsFeature(),
    LicensingFeature(licenseProvider),
    MyCustomFeature()
)
```
