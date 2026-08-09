# SAK — Standard Application Kit

Kotlin Multiplatform platform for **Android and iOS** — build business features once and host them in mobile apps. Access is driven by **JSON config** with an optional **signed license overlay**.

This repo contains the **platform**, a **demo harness**, and two sample features.

---

## Quick start

| Platform | Guide |
|----------|--------|
| **Architecture** | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — SDK, runtime, Mermaid diagrams |
| **Android** | [docs/ANDROID.md](docs/ANDROID.md) — build, run harness, embed in your app |
| **iOS** | [docs/IOS.md](docs/IOS.md) — Xcode, Simulator, embed framework |
| **Config JSON** | [docs/config-schema.md](docs/config-schema.md) |

```bash
# Android
./gradlew :sak-harness:installDebug

# iOS framework (Compose harness)
./gradlew :sak-harness:linkDebugFrameworkIosSimulatorArm64
# Then open iosApp/iosApp.xcodeproj and run

# iOS framework (SwiftUI harness)
./gradlew :sak-ios-bridge:linkDebugFrameworkIosSimulatorArm64
# Then open iosSwiftApp/iosSwiftApp.xcodeproj and run
```

---

## Repository layout

```text
sak-sdk/           Contracts (Feature, config, entry points) — no Compose
sak-runtime/       Engine (lifecycle, config merge, render plan)
sak-compose-ui/    SakApp scaffold + NavHost
sak-ios-bridge/    Headless iOS bridge (SakIosHost) for native SwiftUI/UIKit
sample-analytics/  Example feature
sample-licensing/  Example licensing + LicenseProvider
sak-harness/       Demo app (Android + iOS Compose framework)
iosApp/            Xcode wrapper — Compose harness (SakHarness)
iosSwiftApp/       Xcode wrapper — SwiftUI harness (SakIosBridge)
docs/              Platform guides + config schema
```

---

## Core loop

1. Host loads **DefaultConfig** (JSON).
2. Host bundles **feature modules** and calls **`SakLauncher.launch`**.
3. Runtime merges license overlay → **ActiveConfig**, activates features.
4. **`SakApp`** renders scaffold (bottom bar + destinations) and feature routes in one NavHost.

---

## Prerequisites

- JDK 17+
- Android SDK (compile 35, min 26) for Android
- macOS + Xcode for iOS (Kotlin 2.4.10 certifies Xcode up to 26.4)

---

## Key concepts

| Term | Meaning |
|------|---------|
| **Feature** | Pluggable module: entry points, routes, `FeatureStatus` |
| **EntryPoint** | Widget on a scaffold tab; can navigate into the feature |
| **setupComplete** | Feature reports when internal init is done (`FeatureStatus`) |
| **ActiveConfig** | DefaultConfig + signed license `features` overlay |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for flowcharts (launch, config merge, lifecycle, navigation, iOS vs Android). Code comments in `sak-sdk` and `sak-runtime` describe each type in detail.

---

## License

Personal project by san. All rights reserved.
