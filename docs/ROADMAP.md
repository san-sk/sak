# SAK Roadmap

Future improvements and features planned for the framework.

---

## 🛠️ Phase 1: Robustness & DX (Near Term)

### 1. Remote Config Helpers (Point 6)
Add generic helper methods to `FeatureContext` to easily parse JSON attributes from the `ActiveConfig` into typed Kotlin data classes.
- **Goal**: Reduce boilerplate for features fetching quotas or feature flags.

### 2. Observability Dashboard
Create a built-in `SakDebugFeature` that can be included in development builds.
- **Features**:
    - List all discovered modules and their current `FeatureState`.
    - Real-time log of lifecycle events (Create, Activate, etc.).
    - Manual `Entitlement` override to test UI reactions.
    - Memory leak detection for feature scopes.

---

## 🏗️ Phase 2: Core Evolution (Mid Term)

### 1. Type-Safe Navigation Contracts
Move away from raw string routes (`"analytics/today"`) to a compile-time checked contract.
- **Approach**: Features expose a `NavigationContract` object. The host uses this contract to trigger navigation.

### 2. Dependency Injection Integration
Provide official bridge patterns for **Koin** and **Hilt**.
- **Goal**: Allow features to use standard DI for their internal ViewModels while still being managed by the SAK lifecycle.

---

## 🚀 Phase 3: Platform Expansion (Long Term)

### 1. Advanced iOS Bridge (SwiftUI Native)
Enhance the iOS bridge to allow features to provide native SwiftUI views directly through a Kotlin interface, reducing the overhead for non-Compose iOS hosts.

### 2. Desktop & Web Support
Explore hosting SAK features in **Compose HTML** and **Compose for Desktop** (Wasm/JVM) to make the framework truly universal.
