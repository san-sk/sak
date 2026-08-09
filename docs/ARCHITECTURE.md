# SAK architecture — SDK & runtime

Visual guide to how `sak-sdk`, `sak-runtime`, and `sak-compose-ui` work together on Android and iOS.

**Related:** [config-schema.md](config-schema.md) · [ANDROID.md](ANDROID.md) · [IOS.md](IOS.md)

---

## 1. Module layers

`sak-sdk` has **no Compose** — pure contracts. `sak-runtime` implements the engine. UI lives in `sak-compose-ui` and feature modules.

```mermaid
flowchart TB
    subgraph Host["Host app (sak-harness)"]
        H[MainActivity / MainViewController]
    end

    subgraph UI["sak-compose-ui"]
        SA[SakApp — scaffold + NavHost]
    end

    subgraph Runtime["sak-runtime"]
        SL[SakLauncher]
        SR[SakRuntime]
        FL[FeatureLifecycle]
        SCR[ScaffoldRenderer]
        AR[ApiRegistry]
    end

    subgraph SDK["sak-sdk"]
        F[Feature]
        CFG[AppConfig / FeatureToggle]
        EP[EntryPoint]
        FM[FeatureModule]
    end

    subgraph Features["Feature modules"]
        A[sample-analytics]
        L[sample-licensing]
    end

    H --> SA
    SA --> SR
    SL --> SR
    SR --> FL
    SR --> SCR
    SR --> AR
    FL --> F
    SCR --> EP
    SCR --> CFG
    FM --> F
    A --> F
    L --> F
    A --> EP
    L --> EP
```

| Module | Responsibility |
|--------|----------------|
| **sak-sdk** | `Feature`, `FeatureStatus`, config types, `EntryPoint`, `FeatureModule`, license contracts |
| **sak-runtime** | Config merge, activate/deactivate features, build `RenderPlan`, API registry, navigation signals |
| **sak-compose-ui** | `SakApp` — bottom bar, scaffold destinations, single `NavHost` |
| **Feature module** | Business logic, entry widgets, internal screens, optional `FeatureApi` |

---

## 2. Launch flow (host → runtime → UI)

Same sequence on Android and iOS. Platform code only provides the Compose root.

```mermaid
flowchart TD
    A[Host loads DefaultConfig JSON] --> B[discoverFeatureModules]
    B --> C[SakLauncher.launch]
    C --> D[SakRuntime.start]
    D --> E{LicenseProvider?}
    E -->|yes| F[resolveLicense + subscribe updates]
    E -->|no| G[use DefaultConfig only]
    F --> H[mergeLicenseOverlay]
    G --> H
    H --> I[FeatureLifecycle.apply]
    I --> J[ScaffoldRenderer.build]
    J --> K[RenderPlan in StateFlow]
    K --> L[SakApp collects renderPlan]
    L --> M[Draw bottom bar + destinations + NavHost]
```

**Host code pattern** (`HarnessApp.kt`):

1. `discoverFeatureModules(...)` → list of `FeatureModule`
2. `SakLauncher(scope).launch(defaultConfig, featureModules, deps)` → `SakRuntime`
3. `SakApp(runtime, featureNavContributors = modules.navContributors(deps))`

---

## 3. Config merge

Scaffold comes **only** from DefaultConfig. License overlay updates **features** only.

```mermaid
flowchart LR
    subgraph Sources
        DC[DefaultConfig<br/>scaffold + features]
        LO[License overlay<br/>features only]
    end

    subgraph Runtime
        M[mergeLicenseOverlay]
        AC[ActiveConfig]
    end

    DC --> M
    LO --> M
    M --> AC
    DC -->|scaffold preserved| AC
```

```mermaid
flowchart TD
    AC[ActiveConfig] --> FL[FeatureLifecycle]
    FL -->|enabled=true| ON[onActivated]
    FL -->|enabled=false| OFF[onDeactivated]
    FL -->|plan/grants change| CH[onAccessChanged]
    ON --> EP[Feature publishes entryPoints]
    ON --> ST[Feature updates status]
    CH --> EP
```

---

## 4. Feature lifecycle

Lifecycle is **application-scoped** (process lifetime), not Activity / UIViewController lifecycle.

```mermaid
stateDiagram-v2
    [*] --> Inactive: feature disabled in config
    Inactive --> Active: enabled in ActiveConfig
    Active --> Inactive: disabled or runtime shutdown
    Active --> Active: onAccessChanged (license update)

    state Active {
        [*] --> Setup
        Setup --> Ready: setupComplete = true
        Ready --> Ready: publish entryPoints
        Ready --> Alert: attention = High
        Alert --> Ready: user action
    }
```

| Callback | When |
|----------|------|
| `onActivated` | Feature becomes `enabled` — start init, register screens, publish entry points |
| `onAccessChanged` | `plan`, `grants`, or `entries` changed (usually license) |
| `onDeactivated` | Feature disabled or app shutting down — clear flows, release resources |
| `onReset` | User clear-data — wipe feature local state |

**Readiness:** feature sets `FeatureStatus.setupComplete`. Config `enabled` turns the feature on; `setupComplete` means it is safe to use.

---

## 5. Scaffold & entry points

Config defines **where** widgets appear. Features define **what** widgets exist.

```mermaid
flowchart TB
    subgraph Config["default-config.json scaffold"]
        BB[bottomBar: home, alerts, profile]
        DEST[destinations per tab]
        SEC[sections: entries | query]
    end

    subgraph Feature["Active feature"]
        EPL[entryPoints StateFlow]
        ST[status StateFlow]
    end

    subgraph Runtime["ScaffoldRenderer"]
        RP[RenderPlan]
    end

    subgraph UI["SakApp"]
        NAV[NavigationBar from bottomBar]
        TAB[Scaffold destination screens]
        W[Entry widgets sorted by priority]
    end

    BB --> NAV
    DEST --> SEC
    SEC -->|entries refs| EPL
    SEC -->|query categories| EPL
    EPL --> RP
    RP --> TAB
    RP --> W
    ST -.->|future: badges| NAV
```

**Section types:**

| Type | Config | Runtime behavior |
|------|--------|------------------|
| `entries` | Explicit `{ feature, entry }` refs | Resolve that one widget |
| `query` | `categories: ["alerts"]` | Collect all entry points matching category |

**Alerts example:** feature publishes `EntryPoint(categories = setOf("alerts"))` → Alerts tab query section picks it up.

---

## 6. Navigation (one NavHost)

Scaffold routes and feature routes live in the **same** navigation graph.

```mermaid
flowchart TB
    NH[NavHost]

    subgraph ScaffoldRoutes["Scaffold routes"]
        SH[scaffold/home]
        SA[scaffold/alerts]
        SP[scaffold/profile]
    end

    subgraph FeatureRoutes["Feature routes"]
        AT[analytics/today]
        AR[analytics/report/id]
        LU[licensing/upgrade]
    end

    NH --> SH
    NH --> SA
    NH --> SP
    NH --> AT
    NH --> AR
    NH --> LU

    EP[EntryPoint action Navigate] -->|tap widget| NH
    RT[SakRuntime.navigate] -->|notification / deep link| NH
```

Features register routes via `FeatureNavigationModule.navContributor` when the host builds `SakApp`.

---

## 7. Feature APIs & cross-feature calls

```mermaid
flowchart LR
    LF[LicensingFeature] -->|apis| LP[LicenseProvider]
    LF --> AR[ApiRegistry]
    AR --> AL[ApiLookup in FeatureContext]
    AL --> AF[AnalyticsFeature]
    AF -->|apis.find LicenseProvider| LP
    SR[SakRuntime] -->|license updates| AC[ActiveConfig merge]
```

Licensing feature exposes `LicenseProvider` as a `FeatureApi`. Runtime subscribes to license updates and re-merges config.

---

## 8. Android vs iOS hosting

Runtime and `SakApp` are **identical**. Only the native shell and feature discovery differ.

```mermaid
flowchart TB
    subgraph Android
        MA[MainActivity]
        SL_A[ServiceLoader<br/>META-INF/services]
        MA --> HA[HarnessApp commonMain]
        SL_A --> HA
    end

    subgraph iOS
        SW[SwiftUI ContentView]
        MVC[MainViewController.kt]
        MAN[manualModules list]
        FW[SakHarness.framework]
        SW --> MVC
        MVC --> HA
        MAN --> HA
        FW --> MVC
    end

    subgraph Shared["commonMain — same on all platforms"]
        HA --> SL2[SakLauncher]
        SL2 --> SR[SakRuntime]
        SR --> SA[SakApp]
    end
```

| | Android | iOS |
|---|---------|-----|
| Entry | `MainActivity` | `MainViewController()` in framework |
| Feature discovery | `ServiceLoader` + optional manual list | **Manual** `FeatureModule` list only |
| Build | Gradle → APK | Gradle → framework + Xcode links |
| iOS targets | — | `iosSimulatorArm64`, `iosArm64` |

---

## 9. Data flows (StateFlows)

What Compose observes from the runtime:

```mermaid
flowchart LR
    SR[SakRuntime]

    SR --> RP[renderPlan]
    SR --> AC[activeConfig]
    SR --> NR[navigationRoute]
    SR --> RT[routeTable]

    RP --> SA[SakApp UI]
    NR --> SA
    AC -.->|debug / host| HOST[Host app]

    F[Feature] --> ST[status]
    F --> EP[entryPoints]
    ST -.-> HOST
    EP --> SCR[ScaffoldRenderer]
    SCR --> RP
```

---

## 10. Quick reference — who owns what

```mermaid
flowchart TD
    subgraph HostOwns["Host owns"]
        H1[DefaultConfig JSON]
        H2[Which FeatureModules are bundled]
        H3[LicenseVerifier production]
        H4[Theme / branding slot]
    end

    subgraph RuntimeOwns["Runtime owns"]
        R1[ActiveConfig merge]
        R2[Feature activate/deactivate]
        R3[Entry widget filtering]
        R4[ApiRegistry]
        R5[RenderPlan]
    end

    subgraph FeatureOwns["Feature owns"]
        F1[Internal screens & routes]
        F2[entryPoints + status flows]
        F3[plan/grants business logic]
        F4[setupComplete / attention]
    end

    subgraph ConfigOwns["DefaultConfig scaffold owns"]
        C1[bottomBar tabs]
        C2[destination layouts]
        C3[which entries appear per tab]
    end
```

---

## Source code map

| Concept | Primary files |
|---------|----------------|
| Contracts | `sak-sdk/.../Feature.kt`, `FeatureStatus.kt`, `EntryPoint.kt`, `AppConfig.kt` |
| Engine | `sak-runtime/.../SakRuntime.kt`, `FeatureLifecycle.kt`, `ScaffoldRenderer.kt` |
| Launch | `sak-runtime/.../SakLauncher.kt` |
| UI shell | `sak-compose-ui/.../SakApp.kt` |
| Harness | `sak-harness/.../HarnessApp.kt` |
| iOS entry | `sak-harness/.../MainViewController.kt` |
| iOS modules | `sak-harness/.../HarnessBundledFeatures.kt` |
