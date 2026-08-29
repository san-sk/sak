# SAK Usage Guide

This guide provides step-by-step instructions for implementing features and hosting the SAK runtime.

---

## For Feature Developers

### 1. Define your Feature
Implement the `Feature` interface in `commonMain`. Focus on publishing `EntryPoints` based on the current `access` (entitlements).

```kotlin
class MyFeature : Feature {
    override val id = FeatureId("my-feature")
    
    private val _status = MutableStateFlow(FeatureStatus())
    override val status = _status.asStateFlow()

    private val _entryPoints = MutableStateFlow<List<EntryPoint>>(emptyList())
    override val entryPoints = _entryPoints.asStateFlow()

    override suspend fun onCreate(context: FeatureContext) {
        // Heavy init (DB, SDKs) starts here
    }

    override suspend fun onActivated(context: FeatureContext) {
        // Feature enabled in config - publish your cards/widgets
        publishEntries(context.access)
        _status.value = _status.value.copy(state = FeatureState.Ready)
    }

    private fun publishEntries(access: FeatureToggle) {
        _entryPoints.value = listOf(
            EntryPoint(
                id = EntryPointId("main-card"),
                type = "summary",
                style = EntryStyle.Card,
                action = EntryAction.Navigate("my-feature/main"),
                categories = setOf("home")
            )
        )
    }
}
```

### 2. Provide Platform UI
Implement the `ComposeFeature` interface in `androidMain` (or `commonMain` for Compose Multiplatform).

```kotlin
// In sak-compose-ui compatible module
class MyFeatureModule : FeatureModule, ComposeFeature {
    // ...
    @Composable
    override fun EntryPointView(id: EntryPointId, modifier: Modifier) {
        when(id.value) {
            "main-card" -> MyCustomCard(modifier)
        }
    }
}
```

### 3. Register your Module
Annotate your `FeatureModule` object with `@SakModule` for iOS discovery and ensure `META-INF/services` is set up for Android.

```kotlin
@SakModule
object MyFeatureModule : FeatureModule {
    override val id = FeatureId("my-feature")
    override fun create(deps: SakDependencies) = MyFeature()
}
```

---

## For Host Developers

### 1. Bootstrap the Runtime
Initialize the `SakLauncher` with your bundled modules and configuration.

```kotlin
val launcher = SakLauncher(appScope)
val runtime = launcher.launch(
    defaultConfig = loadConfig(),
    featureModules = discoverFeatureModules(),
    deps = SakDependencies(licenseProvider = myLicense)
)
```

### 2. Build the Shell (Android/Compose)
Use `SakApp` and `SakFeaturePlaceHolder` to build your UI.

```kotlin
SakApp(runtime = runtime) { padding ->
    Scaffold(
        bottomBar = { MyNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(...) {
            composable("home") {
                SakFeaturePlaceHolder(
                    runtime = runtime,
                    category = "home",
                    onNavigate = { runtime.navigate(it) }
                )
            }
        }
    }
}
```

### 3. Build the Shell (iOS/SwiftUI)
Use `SakIosHost` to bridge data to your native views.

```swift
let host = SakIosHost()
host.setConfigListener { config in
    // Re-render native tabs
}
host.start()

// In a View
ForEach(host.entriesForCategory(category: "home"), id: \.id) { entry in
    // Render native card or call feature's native view
}
```
