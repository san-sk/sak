# SAK Integration Guide & Examples

This guide explains how to build a new feature module using the Scalable App Kit (SAK) and how to register it with host applications across Kotlin Multiplatform (KMP), Android (Compose), and iOS (SwiftUI). It covers basic setup, as well as advanced concepts like IPC (Inter-Process Communication), custom JSON metadata, and Error handling.

---

## 1. Create the Feature Logic (KMP `commonMain`)

Features should be created in a dedicated KMP module (e.g., `:feature-chat`). 
Inside `commonMain`, implement your feature logic by extending `BaseFeature`.

### Basic Feature Implementation
```kotlin
// feature-chat/src/commonMain/kotlin/com/example/chat/ChatFeature.kt
package com.example.chat

import sak.sdk.feature.*
import sak.sdk.entry.*

class ChatFeature : BaseFeature() {
    // 1. Define the zero-allocation unique ID
    override val id = FeatureId("chat")

    // 2. Initialize heavy logic (Concurrent & Non-blocking)
    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        
        publishEntries()
        
        // Mark as ready
        updateStatus(FeatureState.Ready)
    }

    private fun publishEntries() {
        setEntryPoints(listOf(
            EntryPoint(
                id = EntryPointId("chat-dashboard-card"),
                type = EntryPointType.Dashboard,
                style = EntryStyle.Card,
                action = EntryAction.Navigate("chat/home"), // Navigates to the native UI route
                priority = 100,
                title = "Live Chat",
                icon = "chat_bubble"
            )
        ))
    }
}
```

---

## 2. Advanced Feature Capabilities

### Inter-Process Communication (IPC) via Service Locator
Features should not have hard Gradle dependencies on each other. Instead, use the `ServiceRegistry`.

**Exporting a Service (Feature A):**
```kotlin
// core-feature/src/commonMain/kotlin/com/example/core/CoreFeature.kt
interface LoggerApi {
    fun log(message: String)
}

class CoreFeature : BaseFeature() {
    override val id = FeatureId("core")

    // Export the API to the Host Runtime
    override fun services(): Set<FeatureService<*>> = setOf(
        featureService(LoggerApi::class, object : LoggerApi {
            override fun log(message: String) { println("LOG: $message") }
        })
    )
}
```

**Consuming a Service (Feature B):**
```kotlin
// chat-feature/src/commonMain/kotlin/com/example/chat/ChatFeature.kt
override suspend fun onCreate(context: FeatureContext) {
    super.onCreate(context)
    
    // Look up the service. Returns null if CoreFeature is disabled!
    val logger = context.services.get<LoggerApi>()
    logger?.log("Chat feature is booting up safely.")
}
```

### Advanced UI via EntryPoint `metadata`
If an `EntryPoint` needs to render a complex custom graph, it can pass arbitrary JSON data to the host.

```kotlin
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val graphData = buildJsonObject {
    put("type", "line_chart")
    put("data_points", "[10, 20, 50, 30]")
}

EntryPoint(
    id = EntryPointId("analytics-graph"),
    type = EntryPointType.Custom,
    title = "Weekly Traffic",
    metadata = mapOf("custom_graph_payload" to graphData)
)
```
*The Compose/SwiftUI host app can read this `metadata` dictionary and draw the chart nativeley.*

### Handling Errors & Attention Levels
Features are observable. If a database fails to load, inform the host app gracefully.

```kotlin
override suspend fun onCreate(context: FeatureContext) {
    super.onCreate(context)
    
    try {
        loadDatabase()
        updateStatus(FeatureState.Ready)
        
        // Request the host app to show a badge on the navigation bar
        updateAttention(Attention.Medium) 
        
    } catch (e: Exception) {
        // Host app will observe this and can show a localized Error Card instead of crashing
        updateStatus(FeatureState.Error("Failed to load local chat database."))
        updateAttention(Attention.High)
    }
}
```

### Reacting to Live Remote Config Updates
If the server pushes a new JSON config payload while the app is running, your feature can react instantly without restarting.

```kotlin
override suspend fun onConfigChanged(access: FeatureAccess) {
    // The server sent down {"params": {"sampling_rate": 0.1}}
    val rate = access.params["sampling_rate"]?.toString()?.toDoubleOrNull() ?: 1.0
    applyNewSamplingRate(rate)
}
```

---

## 3. Build the Platform UI

Because SAK strictly protects the KMP boundary, UI logic stays out of `commonMain`. You build your UI in platform-specific source sets or modules.

### Android Compose (in `androidMain` or `app` module)
```kotlin
// feature-chat/src/androidMain/kotlin/com/example/chat/ChatNavigation.kt
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.chatNavigation(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    composable("chat/home") {
        ChatHomeScreen(
            onOpenSettings = { onNavigate("chat/settings") },
            onBack = onBack
        )
    }
}
```

### iOS SwiftUI (in Xcode)
```swift
// iOSApp/ChatFeature/ChatHomeView.swift
import SwiftUI

struct ChatHomeView: View {
    var onOpenSettings: () -> Void
    var onBack: () -> Void
    
    var body: some View {
        VStack {
            Text("Live Chat Home")
            Button("Settings", action: onOpenSettings)
        }
    }
}
```

---

## 4. Register with the Host Application

To make the feature available, the Host App must register it in two places: The **Logic Engine** and the **UI Router**.

### Step 4a: Register the Logic in SAK Runtime
In your main DI container, add the feature to the `FeatureRegistry` using a **lazy factory**.

**Android:**
```kotlin
val featureRegistry = FeatureRegistry()
    .registerFactory(FeatureId("analytics")) { AnalyticsFeature() }
    .registerFactory(FeatureId("chat")) { ChatFeature() } // Lazy initialization!

val sakRuntime = SakRuntime(
    featureRegistry = featureRegistry,
    hostLifecycle = activityLifecycle,
    scope = appScope
)
```

**iOS (Swift):**
```swift
let sakHost = SakIosHost(
    defaultConfigJson: "{}",
    features: [AnalyticsFeature(), ChatFeature()]
)
sakHost.start()
```

### Step 4b: Register the UI in the Host Router
Wire up the UI function to listen to the exact-once `navigationEvents` channel.

**Android (Compose NavHost):**
```kotlin
// HarnessApp.kt
val navController = rememberNavController()

LaunchedEffect(runtime) {
    // Exact-once delivery prevents "double navigation"
    runtime.navigationEvents.collectLatest { route ->
        navController.navigate(route.value) { launchSingleTop = true }
    }
}

NavHost(navController = navController, startDestination = "home") {
    // Attach the Chat Feature UI
    chatNavigation(
        onNavigate = { route -> runtime.navigate(Route(route)) },
        onBack = { navController.popBackStack() }
    )
}
```

**iOS (SwiftUI NavigationStack):**
```swift
struct ContentView: View {
    @State private var navPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navPath) {
            HomeDashboard()
                .navigationDestination(for: String.self) { route in
                    if route == "chat/home" {
                        ChatHomeView(
                            onOpenSettings: { sakHost.navigate(route: "chat/settings") },
                            onBack: { navPath.removeLast() }
                        )
                    }
                }
        }
        .onAppear {
            sakHost.observeNavigation { route in
                if let r = route { navPath.append(r) }
            }
        }
    }
}
```

---

## 5. Enable via Configuration

Even though the feature is registered, it will not consume memory or run until the configuration enables it.

```kotlin
// JSON payload from remote server (e.g. Firebase Remote Config)
val remoteConfigJson = """
{
  "version": 2,
  "features": {
    "chat": { 
      "enabled": true,
      "params": { "theme": "dark" }
    }
  }
}
"""

// Apply to runtime
val overlay = SakJson.decodeFromString<SakConfig>(remoteConfigJson)
sakRuntime.updateConfig(overlay)
```

When this runs, `FeatureManager` will instantly:
1. Detect the state change inside a fast Mutex lock.
2. Concurrently call `ChatFeature.onCreate()`.
3. Publish the entry point widget to the Home screen (updating the UI StateFlow).
4. Route user taps seamlessly over the exact-once Channel to the native `NavHost` / `NavigationStack`!
