# SAK on iOS — step by step

Use this guide to run the demo harness on the iOS Simulator or embed SAK in your own iOS host app.

**Prerequisites:** macOS, Xcode (Kotlin 2.4.10 supports up to Xcode 26.4), JDK 17+.

---

## 1. Run the demo harness

There are **two** iOS demo apps:

| Xcode project | Framework | UI | Use when |
|---------------|-----------|-----|----------|
| `iosApp/iosApp.xcodeproj` | `SakHarness` | Compose via `MainViewController()` | Reference shared Compose shell |
| `iosSwiftApp/iosSwiftApp.xcodeproj` | `SakIosBridge` | Pure SwiftUI from `renderPlan` | Native iOS shell (no Compose UI) |

### Option A — Compose harness (Xcode)

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Set **Development Team** under Signing (or set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`).
3. Select an iOS Simulator.
4. Run the **iosApp** scheme.

Xcode runs a **Compile Kotlin** script that calls:

```bash
./gradlew :sak-harness:embedAndSignAppleFrameworkForXcode
```

Swift hosts Kotlin Compose via `ContentView.swift` → `MainViewControllerKt.MainViewController()`.

### Option B — SwiftUI harness (Xcode)

1. Open `iosSwiftApp/iosSwiftApp.xcodeproj` in Xcode.
2. Set **Development Team** (or `TEAM_ID` in `iosSwiftApp/Configuration/Config.xcconfig`).
3. Select an iOS Simulator (arm64; x86_64 is excluded — no `iosX64` Kotlin target).
4. Run the **iosSwiftApp** scheme.

Gradle embed task:

```bash
./gradlew :sak-ios-bridge:embedAndSignAppleFrameworkForXcode
```

Swift observes `SakIosHost` (`sak-ios-bridge` module): bottom bar from `bottomBarItems()`, entries from `entriesForDestination()`, navigation via `onEntrySelected` / `setNavigationListener`.

### Option C — Gradle only (framework build)

```bash
cd SAK
./gradlew :sak-harness:linkDebugFrameworkIosSimulatorArm64
./gradlew :sak-ios-bridge:linkDebugFrameworkIosSimulatorArm64
```

Outputs:

- `sak-harness/build/bin/iosSimulatorArm64/debugFramework/SakHarness.framework`
- `sak-ios-bridge/build/bin/iosSimulatorArm64/debugFramework/SakIosBridge.framework`

---

## 2. How iOS hosting works

### Compose path (`iosApp` + `sak-harness`)

```text
iosApp (SwiftUI shell)
  └── SakHarness.framework (KMP)
        └── MainViewController() → HarnessApp → SakApp
```

- **Kotlin/Compose** lives in `sak-harness` (static framework `SakHarness`).
- **Swift** is a thin wrapper; business UI is shared `commonMain` Compose.

Entry point: `sak-harness/src/iosMain/kotlin/sak/harness/MainViewController.kt`.

### Native SwiftUI path (`iosSwiftApp` + `sak-ios-bridge`)

```text
iosSwiftApp (SwiftUI shell)
  └── SakIosBridge.framework (KMP, no Compose UI)
        └── SakIosHost → SakLauncher / SakRuntime
              └── renderPlan, navigationRoute (observed in Swift)
```

- **`sak-ios-bridge`** exposes [SakIosHost](sak-ios-bridge/src/commonMain/kotlin/sak/ios/bridge/SakIosHost.kt) — headless runtime bridge for SwiftUI/UIKit.
- Swift builds tabs and entry cards from `RenderPlan`; feature screens are native in `iosSwiftApp/iosSwiftApp/FeatureRouteView.swift` (`analytics/today`, `analytics/report/{id}`, `licensing/upgrade`).
- Sample features still ship Compose navigation modules transitively; the Swift shell does not call `SakApp`.

---

## 3. Embed SAK in your own iOS app

### Step 1 — Add a KMP module (or reuse harness pattern)

Your host module needs iOS targets and a framework binary:

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "YourHostFramework"
            isStatic = true
        }
    }
}
```

Depend on `sak-compose-ui`, `sak-runtime`, `sak-sdk`, and your feature modules.

### Step 2 — Expose a UIViewController

In `iosMain`:

```kotlin
fun MainViewController() = ComposeUIViewController {
    MaterialTheme {
        YourHostApp(defaultConfig = loadConfig())
    }
}
```

Mirror `HarnessApp.kt`: discover modules, `SakLauncher.launch`, `SakApp`.

### Step 3 — Register feature modules manually

iOS has no ServiceLoader. List modules explicitly:

```kotlin
val featureModules = discoverFeatureModules(
    manualModules = listOf(
        AnalyticsFeatureModule,
        LicensingFeatureModule,
    ),
)
```

Reference: `sak-harness/.../HarnessBundledFeatures.kt`.

### Step 4 — Ship default config

Harness embeds JSON in `HarnessEmbeddedConfig.kt` (works on all platforms without asset loading).

For production you can:

- Embed JSON in Kotlin (like harness), or
- Load from iOS bundle in Swift and pass to Kotlin, or
- Fetch remotely before `SakLauncher.launch`.

### Step 5 — Xcode integration

1. Create or copy `iosApp/` structure from this repo.
2. Add **Compile Kotlin** run script (before Compile Sources):

```sh
if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then exit 0; fi
cd "$SRCROOT/.."
./gradlew :your-module:embedAndSignAppleFrameworkForXcode
```

3. Set **Framework Search Paths**:

```text
$(SRCROOT)/../your-module/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

4. Set **Other Linker Flags**: `-framework YourHostFramework`
5. Disable **User Script Sandboxing** for the run script phase.

### Step 6 — SwiftUI host (Compose)

```swift
import SwiftUI
import YourHostFramework

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.keyboard)
    }
}
```

Replace `YourHostFramework` / `MainViewControllerKt` with your framework and Kotlin entry names.

### Step 7 — SwiftUI host (native shell, no Compose)

Use `sak-ios-bridge` (or mirror its pattern):

```swift
import SakIosBridge

let host = SakIosHost()
host.setRenderPlanListener { plan in
    // refresh tab bar + entry lists from host.bottomBarItems() / entriesForDestination(...)
}
host.setNavigationListener { route in
    // push native screen for route
}
host.start()
```

Reference: `iosSwiftApp/iosSwiftApp/SakHostModel.swift`.

---

## 4. Feature modules on iOS

Same `Feature` implementation as Android — code in `commonMain`.

| Task | iOS note |
|------|----------|
| Feature discovery | `manualModules` list required |
| Navigation | Shared `NavGraphBuilder` in `commonMain` |
| Entry points / alerts | Same `categories` + config query sections |
| Readiness | `Feature.status.setupComplete` — feature reports, host observes |

---

## 5. Toolchain warnings

Gradle may print:

- `Xcode 26.5+ is not yet supported`
- `macOS 26.5+ is not yet supported`

These are informational on preview OS/Xcode. Builds often still work. Use Xcode ≤ 26.4 if you hit native/cinterop errors.

---

## 6. Troubleshooting

| Issue | Fix |
|-------|-----|
| `PhaseScriptExecution` / Compile Kotlin failed | Open **Report navigator** → expand **Compile Kotlin** for the real error. Common: **no JDK** (install JDK 17), **ios_x64** (arm64 Simulator), or run `cd SAK && ./scripts/xcode-embed-kotlin.sh :sak-ios-bridge:embedAndSignAppleFrameworkForXcode` from Terminal |
| `No such module SakHarness` / `SakIosBridge` | Run Gradle embed task; check framework search paths |
| Blank screen | Verify `MainViewController` calls `SakLauncher.launch` before `SakApp` |
| Features missing | Add modules to `manualModules` on iOS |
| `ios_x64` / framework architecture error | Exclude `x86_64` for Simulator (see `iosSwiftApp`); targets are `iosArm64` + `iosSimulatorArm64` only |
| Kotlin changes not applied | Clean build folder; re-run embed script |

---

## See also

- [ARCHITECTURE.md](ARCHITECTURE.md) — SDK & runtime diagrams (including iOS hosting)
- [ANDROID.md](ANDROID.md) — Android host setup (ServiceLoader, assets)
- [config-schema.md](config-schema.md) — JSON config reference
- Root [README.md](../README.md) — repo overview
