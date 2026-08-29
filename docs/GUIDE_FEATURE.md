# Feature Developer Guide

This guide explains how to build a business feature for the SAK platform.

---

## 1. The Core Interface
Your feature must implement the `Feature` interface in `commonMain`.

### Lifecycle Hooks
- **`onCreate(context)`**: Heavy setup (Database, long-lived sockets). This is called only once.
- **`onActivated(context)`**: The feature is enabled in config. This is where you should publish your entry points and routes.
- **`onAccessChanged(context)`**: The user's plan or entitlements updated. Re-calculate and publish entries here.
- **`onDeactivated()`**: Stop background flows and clear internal state.
- **`onDestroy()`**: Release all resources.

### Publishing Entry Points
Use the `entryPoints` flow to tell the host what widgets you have available.

```kotlin
private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
override val entryPoints = _entryPoints.asStateFlow()

private fun publishEntries(access: FeatureToggle) {
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
```

---

## 2. Using the FeatureContext
The `context` provided in lifecycle hooks gives you access to vital platform capabilities:

- **`coroutineScope`**: A managed scope that is automatically cancelled when your feature stops. Use this for all background work.
- **`access`**: The current configuration (Plan, Grants, Attributes).
- **`typedAttr<T>(key)`**: Easily parse JSON config attributes into Kotlin data classes.
- **`apis`**: Look up capabilities provided by other features.

---

## 3. Platform-Native UI
To provide a custom widget, implement the bridge interface for your target platforms.

### Android (Compose)
Implement `ComposeFeature` in your module.

```kotlin
class MyFeature : Feature, ComposeFeature {
    @Composable
    override fun EntryPointView(id: EntryPointId, modifier: Modifier) {
        when(id.value) {
            "summary-card" -> MySummaryCard(modifier)
        }
    }
}
```

---

## 4. Cross-Feature Communication
Features can expose their own APIs for others to use.

### Exporting an API
```kotlin
interface MyAnalyticsApi {
    fun logEvent(name: String)
}

// In your Feature implementation:
override fun apis() = setOf(featureApi(MyAnalyticsApi::class, myProvider))
```

### Consuming an API
```kotlin
val analytics = context.apis.find(MyAnalyticsApi::class)
analytics?.logEvent("tab_opened")
```

---

## 5. Automated Registration
To ensure your feature is found on all platforms:
1.  **Android**: Add your `FeatureModule` class name to `META-INF/services/sak.sdk.bundle.FeatureModule`.
2.  **iOS**: Annotate your `FeatureModule` object with `@SakModule`.

```kotlin
@SakModule
object MyFeatureModule : FeatureModule {
    override val id = FeatureId("my-feature")
    override val priority = LoadingPriority.Lazy // Instantiate only when needed
    override fun create(deps: SakDependencies) = MyFeature()
}
```
