# SAK Integration Guide & Examples

This guide provides step-by-step instructions and code examples for building feature modules using the Scalable App Kit (SAK) and integrating them into Host Applications on **Android (Jetpack Compose)**, **iOS (SwiftUI)**, and **Kotlin Multiplatform (KMP)**.

---

## 1. Create a Feature Logic Module (KMP `commonMain`)

Features are created in dedicated multiplatform modules (e.g. `:feature-chat`). Inside `commonMain`, extend `BaseFeature`.

```kotlin
// feature-chat/src/commonMain/kotlin/com/example/chat/ChatFeature.kt
package com.example.chat

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sak.sdk.entry.*
import sak.sdk.feature.*
import sak.sdk.service.*

class ChatFeature : BaseFeature() {
    // 1. Define the zero-allocation unique ID
    override val id = FeatureId("chat")

    // 2. Export public APIs for Inter-Process Communication (IPC)
    override fun services(): Set<FeatureService<*>> = setOf(
        featureService(ChatApi::class, ChatApiImpl())
    )

    // 3. Concurrent & Non-blocking Initialization
    override suspend fun onCreate(context: FeatureContext) {
        super.onCreate(context)
        
        try {
            // Read remote configuration params
            val theme = context.access.params["theme"]?.toString() ?: "light"
            
            // Use services exported by sibling features (Null-safe!)
            val logger = context.services.get<LoggerApi>()
            logger?.log("Chat Feature booted with theme: $theme")

            // Publish UI metadata to Host
            publishEntries()
            
            // Set status to Ready
            updateStatus(FeatureState.Ready)
            
            // Optionally request the host to render an unread badge on the nav bar
            updateAttention(Attention.Medium)
        } catch (e: Exception) {
            // Graceful error reporting instead of crashing the app
            updateStatus(FeatureState.Error("Failed to initialize Chat DB: ${e.message}"))
            updateAttention(Attention.High)
        }
    }

    // 4. React to Live Remote Config Updates
    override suspend fun onConfigChanged(access: FeatureAccess) {
        val newTheme = access.params["theme"]?.toString() ?: "light"
        println("Hot-swapping theme to: $newTheme")
    }

    override suspend fun onDestroy() {
        super.onDestroy()
        // Clean up resources. Note: context.coroutineScope is automatically cancelled.
    }

    private fun publishEntries() {
        val customPayload = buildJsonObject {
            put("unread_count", 3)
            put("latest_sender", "Sarah")
        }

        setEntryPoints(listOf(
            EntryPoint(
                id = EntryPointId("chat-dashboard-card"),
                type = EntryPointType.Dashboard,
                style = EntryStyle.Card,
                action = EntryAction.Navigate("chat/home"),
                priority = 100,
                title = "Live Chat",
                subtitle = "Sarah: Is the SAK update ready?",
                icon = "chat_bubble",
                badge = "3 UNREAD",
                metadata = mapOf("chat_data" to customPayload)
            )
        ))
    }
}
```

---

## 2. Build the Native UI

Features do not include UI framework code in `commonMain` to maintain KMP purity. You build platform UIs in native source sets.

### Android Jetpack Compose (`androidMain` or Host App)
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
    composable("chat/settings") {
        ChatSettingsScreen(onBack = onBack)
    }
}
```

### iOS SwiftUI (`iOSApp` Xcode Project)
```swift
// iOSApp/ChatFeature/ChatHomeView.swift
import SwiftUI

struct ChatHomeView: View {
    var onOpenSettings: () -> Void
    var onBack: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Text("Live Chat Home")
                .font(.largeTitle)
            Button("Chat Settings", action: onOpenSettings)
            Button("Back", action: onBack)
        }
    }
}
```

---

## 3. Register with the Host Application

### Step 3a: Logic Engine Registration

**Android (`MainActivity.kt` or `Application`):**
```kotlin
val featureRegistry = FeatureRegistry()
    .registerFactory(FeatureId("analytics")) { AnalyticsFeature() }
    .registerFactory(FeatureId("chat")) { ChatFeature() } // Lazy factory registration!

val sakRuntime = SakRuntime(
    baseConfig = loadLocalConfig(),
    featureRegistry = featureRegistry,
    serviceRegistry = ServiceRegistry(),
    scope = appScope,
    hostLifecycle = LocalLifecycleOwner.current.lifecycle
)

LaunchedEffect(Unit) {
    sakRuntime.start()
}
```

**iOS Swift (`AppModel.swift`):**
```swift
import SakIosBridge

@MainActor
final class AppModel: ObservableObject {
    private let host = SakIosHost(
        defaultConfigJson: "{}",
        features: [AnalyticsFeature(), ChatFeature()]
    )

    func start() {
        host.start {
            print("SAK Engine Online on iOS!")
        }
    }
}
```

### Step 3b: UI Navigation Registration

**Android (`HarnessApp.kt`):**
```kotlin
val navController = rememberNavController()

// Listen to exact-once navigation channel
LaunchedEffect(runtime) {
    runtime.navigationEvents.collectLatest { route ->
        navController.navigate(route.value) { launchSingleTop = true }
    }
}

NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeDashboard(runtime) }
    
    // Wire up feature UI graphs
    chatNavigation(
        onNavigate = { route -> runtime.navigate(Route(route)) },
        onBack = { navController.popBackStack() }
    )
}
```

**iOS SwiftUI (`ContentView.swift`):**
```swift
struct ContentView: View {
    @StateObject private var appModel = AppModel()
    @State private var navPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navPath) {
            HomeDashboardView(appModel: appModel)
                .navigationDestination(for: String.self) { route in
                    if route == "chat/home" {
                        ChatHomeView(
                            onOpenSettings = { appModel.host.navigate(route: "chat/settings") },
                            onBack = { navPath.removeLast() }
                        )
                    }
                }
        }
        .onAppear {
            appModel.start()
            appModel.host.observeNavigation { route in
                if let r = route { navPath.append(r) }
            }
        }
    }
}
```

---

## 4. Live Remote Configuration Updates

You can push live configuration updates (e.g. from Firebase Remote Config or a REST API) at any time.

```kotlin
// Server response JSON
val serverJson = """
{
  "version": 2,
  "features": {
    "chat": {
      "enabled": true,
      "params": {
        "theme": "dark",
        "max_history": 50
      }
    },
    "analytics": {
      "enabled": false
    }
  }
}
"""

// Parse and update runtime overlay
val overlayConfig = SakJson.decodeFromString<SakConfig>(serverJson)
scope.launch {
    sakRuntime.updateConfig(overlayConfig)
}
```

**What happens automatically:**
1. `AnalyticsFeature` is immediately torn down (`onDestroy()`), its coroutine scope is cancelled, and its entry point cards disappear from the Home Dashboard.
2. `ChatFeature` is instantiated concurrently (`onCreate()`), receives the `params` dictionary, and publishes its `EntryPoint` cards to the Home Dashboard.

---

## 5. Troubleshooting Guide

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| **Feature doesn't start** | Failed one of the 3 Gates. | Check if (1) `FeatureRegistry` has a factory, (2) `FeatureEntitlements.isLicensed()` returns `true`, and (3) `SakConfig.isFeatureEnabled()` returns `true`. |
| **Double Navigation in Compose** | Listening to a `StateFlow` for routes. | Collect from `SakRuntime.navigationEvents` (`Channel<Route>`) which guarantees exactly-once delivery. |
| **Slow App Startup** | Pre-instantiating features in Registry. | Use `registerFactory(id) { Feature() }` instead of `register(Feature())` to enable lazy initialization. |
| **Thread Deadlock during onCreate** | Blocking main thread in feature setup. | Use `context.coroutineScope.launch { ... }` or suspend functions inside `onCreate`. `FeatureManager` executes `onCreate` concurrently. |
| **`context.services.get<T>()` returns null** | Sibling feature is disabled or not registered. | Always handle null return types from `ServiceLookup`. Implement graceful fallback logic if the optional feature isn't active. |
