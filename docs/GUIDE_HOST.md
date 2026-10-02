# Host Integrator Guide

This guide explains how to integrate SAK into a host application (Android Jetpack Compose or iOS SwiftUI).

---

## 1. Initializing the Runtime

The host bootstraps `SakRuntime` during app startup.

```kotlin
val appScope = rememberCoroutineScope()
val hostLifecycle = LocalLifecycleOwner.current.lifecycle

val runtime = SakRuntime(
    defaultConfig = mySakConfig,
    featureRegistry = FeatureRegistry.of(
        AnalyticsFeature(),
        LicensingFeature(licenseProvider)
    ),
    serviceRegistry = ServiceRegistry(mapOf(
        HarnessLogService::class to myLogService
    )),
    scope = appScope,
    hostLifecycle = hostLifecycle,
)

LaunchedEffect(Unit) {
    runtime.start(licenseProvider)
}
```

---

## 2. Rendering Slots in Jetpack Compose (Android)

Because SAK is protocol-first and UI-agnostic, host apps observe published `entryPoints` from active features and render custom Compose UI cards.

```kotlin
@Composable
fun ComposeFeatureSlot(
    runtime: SakRuntime,
    category: String,
    modifier: Modifier = Modifier
) {
    val activeConfig by runtime.activeConfig.collectAsState()

    val entries by produceState<List<Pair<FeatureId, EntryPoint>>>(initialValue = emptyList(), activeConfig) {
        val resolved = mutableListOf<Pair<FeatureId, EntryPoint>>()
        activeConfig.features.keys.forEach { idVal ->
            val id = FeatureId(idVal)
            val feature = runtime.getFeature(id)
            if (feature != null) {
                feature.entryPoints.value
                    .filter { it.categories.contains(category) }
                    .forEach { resolved.add(id to it) }
            }
        }
        value = resolved.sortedByDescending { it.second.priority }
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(entries) { (featureId, entry) ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    when (val action = entry.action) {
                        is EntryAction.Navigate -> runtime.navigate(Route(action.route))
                        else -> Unit
                    }
                }
            ) {
                Text(text = "${featureId.value} · ${entry.id.value}")
            }
        }
    }
}
```

---

## 3. Integrating with iOS (SwiftUI)

On iOS, host apps import the KMP framework export and observe `SakRuntime` / `EntryPoint` state flows in SwiftUI views:

```swift
final class SakHostModel: ObservableObject {
    @Published var entries: [EntryPoint] = []

    func start() {
        // Query runtime active entryPoints
    }
}
```
