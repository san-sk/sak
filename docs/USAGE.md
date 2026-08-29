# SAK Quick Start Guide

Welcome to SAK! This 5-minute guide will get you up and running with the framework.

---

## 🚦 For Feature Developers

### 1. Create your module
Implement the `Feature` interface in `commonMain`.

```kotlin
class NewsFeature : Feature {
    override val id = FeatureId("news")
    
    // Publish a simple card
    private val _entryPoints = MutableStateFlow(listOf(
        EntryPoint(id = EntryPointId("headline"), type = "summary", ...)
    ))
    override val entryPoints = _entryPoints.asStateFlow()

    override suspend fun onActivated(context: FeatureContext) {
        // Init logic
    }
}
```

### 2. Register for Discovery
Add the `@SakModule` annotation to your factory object.

```kotlin
@SakModule
object NewsModule : FeatureModule {
    override val id = FeatureId("news")
    override fun create(deps: SakDependencies) = NewsFeature()
}
```

---

## 🏢 For Host Developers

### 1. Launch the Runtime
Start the engine in your application entry point.

```kotlin
val runtime = SakLauncher(scope).launch(
    defaultConfig = myConfig,
    featureModules = discoverFeatureModules()
)
```

### 2. Add Placeholders
Place widgets in your Compose (Android) or SwiftUI (iOS) layout.

```kotlin
// Android
SakFeaturePlaceHolder(runtime, category = "home", ...)

// iOS
let entries = host.entriesForCategory(category: "home")
```

---

## 🛠️ Debugging
Include the `:sak-library-debug` module in your debug build to access the **SAK Observability Dashboard**. Navigate to `sak/debug` in your app to see live feature states and configurations.

---

## 📖 Deep Dives
- [Feature Developer Guide](GUIDE_FEATURE.md)
- [Host Integrator Guide](GUIDE_HOST.md)
- [Configuration Reference](CONFIG.md)
