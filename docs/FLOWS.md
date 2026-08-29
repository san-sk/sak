# SAK Dynamic Flows

This document details the reactive behaviors of the SAK framework using sequence diagrams.

---

## 1. System Boot Sequence
This flow shows how the host initializes the SAK runtime and how features are created and activated.

```mermaid
sequenceDiagram
    participant Host as Host App
    participant Launcher as SakLauncher
    participant Runtime as SakRuntime
    participant Lifecycle as FeatureLifecycle
    participant Feature as Feature Module

    Host->>Launcher: launch(DefaultConfig, Modules)
    Launcher->>Runtime: create(DefaultConfig)
    Runtime->>Lifecycle: apply(DefaultConfig)
    
    rect rgb(240, 240, 240)
    Note over Lifecycle, Feature: Startup Features
    Lifecycle->>Feature: onCreate(context)
    Lifecycle->>Feature: onActivated(access)
    Feature-->>Runtime: publish entryPoints
    end

    Runtime-->>Host: return runtime instance
    Host->>Host: render placeholders
```

---

## 2. Reactive License Update
Shows what happens when a user upgrades their license while the app is running.

```mermaid
sequenceDiagram
    participant Provider as LicenseProvider
    participant Runtime as SakRuntime
    participant Lifecycle as FeatureLifecycle
    participant Feature as Feature Module
    participant Host as Host UI

    Provider->>Runtime: emit(NewLicensePayload)
    Runtime->>Runtime: merge(Default, NewLicense)
    Runtime->>Lifecycle: apply(ActiveConfig)

    alt Feature Newly Enabled
        Lifecycle->>Feature: onCreate(context)
        Lifecycle->>Feature: onActivated(access)
    else Entitlements Changed
        Lifecycle->>Feature: onAccessChanged(access)
    end

    Feature-->>Runtime: update entryPoints flow
    Runtime-->>Host: update activeConfig flow
    Host->>Host: Re-render placeholders
```

---

## 3. Just-In-Time Lazy Loading
Shows the flow when a feature marked as `LoadingPriority.Lazy` is accessed for the first time.

```mermaid
sequenceDiagram
    participant Host as Host UI
    participant Placeholder as SakFeaturePlaceHolder
    participant Runtime as SakRuntime
    participant Lifecycle as FeatureLifecycle
    participant Feature as Feature Module

    Host->>Placeholder: render(category="home")
    Placeholder->>Runtime: getFeature(lazyId)
    
    Runtime->>Lifecycle: getOrInstantiate(lazyId)
    Lifecycle->>Feature: onCreate(context)
    Lifecycle->>Feature: onActivated(access)
    
    Lifecycle-->>Runtime: Feature instance
    Runtime-->>Placeholder: Feature instance
    
    Placeholder->>Feature: render EntryPointView()
    Feature-->>Host: Composable / View
```

---

## 4. Error Isolation (Sandbox)
Shows how a failure in one feature is isolated to prevent a global crash.

```mermaid
sequenceDiagram
    participant Lifecycle as FeatureLifecycle
    participant BuggyFeature as Buggy Feature
    participant Runtime as SakRuntime
    participant Host as Host UI

    Lifecycle->>BuggyFeature: onCreate()
    Note right of BuggyFeature: 💥 Throws Exception
    Lifecycle->>Lifecycle: Catch exception (SupervisorJob)
    Lifecycle->>BuggyFeature: Update status to Error
    
    Runtime-->>Host: status flow emits ERROR
    Host->>Host: SakEntryWidget renders Error UI
    Note over Host: Rest of the app remains functional
```
