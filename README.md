# SAK: Scalable App Kit

SAK is a production-ready, highly decoupled Kotlin Multiplatform (KMP) architecture designed for scaling massive mobile applications. 

It allows teams to build independent **Feature Modules** that act as plugins. Features manage their own lifecycles, expose public APIs to sibling modules without hard dependencies, and emit UI metadata (`EntryPoints`) to the host applications (Android Compose / iOS SwiftUI) to render dynamically.

## Core Capabilities
*   **Plugin Architecture:** Features are lazy-loaded and decoupled. They can be dynamically added or removed at runtime without restarting the app.
*   **The 3-Gate Activation System:** Features only boot up if they are (1) compiled into the app, (2) cryptographically licensed, and (3) enabled via Remote Config.
*   **Micro-Frontend UI (`EntryPoint`):** Features do not dictate UI frameworks. They publish JSON-serializable `EntryPoint` widgets. The host app reads this stream and natively draws Android Compose or iOS SwiftUI cards.
*   **Exactly-Once Navigation:** Navigation events are streamed through Coroutine `Channel`s to prevent Compose "double-navigation" bugs.
*   **Zero-Allocation Typed IDs:** Domain IDs use `@JvmInline value class` for strict compile-time safety with zero runtime heap allocation.

## Documentation

The documentation has been consolidated into two comprehensive, production-ready guides:

1.  [**Architecture & Knowledge Base**](docs/architecture.md): Deep dive into *why* the SDK is built this way. Contains architecture diagrams, explanations of SOLID principles, KMP boundaries, and Concurrency & Memory optimizations.
2.  [**Integration Guide & Examples**](docs/integration.md): Step-by-step code examples on how to build a `Feature`, write a Compose/SwiftUI UI, and register it in the Host App.

---

## Quick Start Overview

### 1. Build a Feature (KMP `commonMain`)
```kotlin
class ChatFeature : BaseFeature() {
    override val id = FeatureId("chat")

    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        
        // Expose a UI widget to the host app
        setEntryPoints(listOf(
            EntryPoint(
                id = EntryPointId("chat-card"),
                type = EntryPointType.Dashboard,
                action = EntryAction.Navigate("chat/home"),
                title = "Live Chat",
                icon = "chat_bubble"
            )
        ))
        
        updateStatus(FeatureState.Ready)
    }
}
```

### 2. Boot the Runtime (Host App)
```kotlin
val registry = FeatureRegistry()
    .registerFactory(FeatureId("chat")) { ChatFeature() } // Lazy loaded!

val runtime = SakRuntime(
    featureRegistry = registry,
    hostLifecycle = activityLifecycle,
    scope = appScope
)
runtime.start()
```
