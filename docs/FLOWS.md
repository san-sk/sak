# SAK Dynamic Flows & Sequence Diagrams

This document details the reactive behaviors and lifecycle flows of the SAK framework.

---

## 1. System Boot Sequence

Shows how the host application initializes `SakRuntime`, binds platform lifecycle, and initializes features.

```mermaid
sequenceDiagram
    participant Host as Host App Shell
    participant Launcher as SakLauncher
    participant Runtime as SakRuntime
    participant Lifecycle as FeatureLifecycle
    participant Feature as Feature Module

    Host->>Launcher: launch(defaultConfig, featureRegistry, hostLifecycle)
    Launcher->>Runtime: SakRuntime(config, registry, hostLifecycle)
    Runtime->>Lifecycle: apply(SakConfig)
    
    rect rgb(240, 240, 240)
    Note over Lifecycle, Feature: Active Features
    Lifecycle->>Feature: onCreate(context)
    Feature-->>Runtime: publish entryPoints flow
    end

    Runtime-->>Host: return runtime instance
    Host->>Host: render slots with native Compose / SwiftUI
```

---

## 2. Reactive License Overlay Update

Shows what happens when a user upgrades their license tier or unlocks new entitlements at runtime.

```mermaid
sequenceDiagram
    participant License as LicenseProvider
    participant Runtime as SakRuntime
    participant Lifecycle as FeatureLifecycle
    participant Feature as Feature Module
    participant Host as Host UI

    License->>Runtime: emit(SignedLicensePayload)
    Runtime->>Runtime: mergeLicenseOverlay(baseConfig, overlay)
    Runtime->>Lifecycle: apply(mergedConfig)

    alt Feature Newly Enabled
        Lifecycle->>Feature: onCreate(context)
    else Entitlements / Grants Changed
        Lifecycle->>Feature: onConfigChanged(access)
    end

    Feature-->>Runtime: update entryPoints flow
    Runtime-->>Host: activeConfig flow updates
    Host->>Host: Re-render UI slots reactively
```

---

## 3. Just-In-Time Lazy Loading

Shows the flow when a lazy feature is accessed for the first time by slot category query or service lookup.

```mermaid
sequenceDiagram
    participant Host as Host UI
    participant Runtime as SakRuntime
    participant Lifecycle as FeatureLifecycle
    participant Feature as Feature Module

    Host->>Runtime: getFeature(featureId)
    Runtime->>Lifecycle: getOrInstantiate(featureId)
    Lifecycle->>Feature: onCreate(context)
    Lifecycle-->>Runtime: Feature instance
    Runtime-->>Host: Feature instance
```

---

## 4. Error Isolation (Sandbox)

Shows how failures in a single feature are isolated using `SupervisorJob`.

```mermaid
sequenceDiagram
    participant Lifecycle as FeatureLifecycle
    participant BuggyFeature as Buggy Feature
    participant Host as Host UI

    Lifecycle->>BuggyFeature: onCreate(context)
    Note right of BuggyFeature: 💥 Exception Thrown
    Lifecycle->>Lifecycle: Catch exception (SupervisorJob)
    Lifecycle->>BuggyFeature: Status updated to FeatureState.Error
    Host->>Host: Host UI handles error state gracefully
```
