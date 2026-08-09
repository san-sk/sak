# SAK on Android — step by step

Use this guide to run the demo harness or embed SAK in your own Android host app.

**Prerequisites:** JDK 17+, Android Studio, Android SDK (compile SDK 35, min SDK 26).

---

## 1. Run the demo harness

```bash
cd SAK
./gradlew :sak-harness:assembleDebug
./gradlew :sak-harness:installDebug
```

Or open the project in Android Studio and run the `sak-harness` configuration.

**What you should see:** Home tab with analytics + licensing cards → tap analytics for internal screens → unlock Pro on licensing → Alerts tab shows extra widget.

---

## 2. Understand the modules

| Module | Role |
|--------|------|
| `sak-sdk` | Contracts: Feature, config, entry points (no UI) |
| `sak-runtime` | Engine: lifecycle, config merge, render plan |
| `sak-compose-ui` | `SakApp` — scaffold + NavHost |
| `sample-*` | Example features |
| `sak-harness` | Demo Android app |

---

## 3. Embed SAK in your Android app

### Step 1 — Add Gradle dependencies

In your host `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sak-compose-ui"))  // or published artifacts
    implementation(project(":sak-runtime"))
    implementation(project(":sak-sdk"))
    implementation(project(":sample-analytics"))  // your feature modules
}
```

Include the same modules in `settings.gradle.kts`.

### Step 2 — Ship default config

Place JSON in `src/main/assets/default-config.json` (see `sak-harness/src/androidMain/assets/default-config.json`).

Load it in your Activity:

```kotlin
val json = assets.open("default-config.json").bufferedReader().readText()
val defaultConfig = SakJson.decodeFromString(AppConfig.serializer(), json)
```

Schema reference: [config-schema.md](config-schema.md).

### Step 3 — Discover feature modules

Android auto-discovers `FeatureModule` via ServiceLoader. Each feature module needs:

```text
src/androidMain/resources/META-INF/services/sak.sdk.bundle.FeatureModule
```

Contents (one line per module):

```text
sak.sample.analytics.AnalyticsFeatureModule
sak.sample.licensing.LicensingFeatureModule
```

Then in your host:

```kotlin
val featureModules = discoverFeatureModules()
val deps = SakDependencies(licenseProvider = yourLicenseProvider)
```

### Step 4 — Launch runtime and show UI

In a `ComponentActivity` with Compose:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultConfig = loadDefaultConfig()
        setContent {
            MaterialTheme {
                SakHost(defaultConfig)
            }
        }
    }
}

@Composable
fun SakHost(defaultConfig: AppConfig) {
    var runtime by remember { mutableStateOf<SakRuntime?>(null) }
    val scope = rememberCoroutineScope()
    val modules = remember { discoverFeatureModules() }
    val deps = SakDependencies(licenseProvider = ...)

    LaunchedEffect(defaultConfig) {
        if (runtime == null) {
            runtime = SakLauncher(scope).launch(
                defaultConfig = defaultConfig,
                featureModules = modules,
                deps = deps,
            )
        }
    }

    runtime?.let { rt ->
        SakApp(
            runtime = rt,
            featureNavContributors = modules.navContributors(deps),
        )
    }
}
```

Reference implementation: `sak-harness/.../HarnessApp.kt` and `MainActivity.kt`.

### Step 5 — Add your own feature

1. Create a KMP module implementing `Feature` + `FeatureModule`.
2. Register `META-INF/services/sak.sdk.bundle.FeatureModule`.
3. Add navigation in `commonMain` (`NavGraphBuilder` extension).
4. Add Gradle dependency from host.
5. Reference entry points in `default-config.json` under `scaffold` and `features`.

---

## 4. Feature readiness and alerts

| Signal | Where | Purpose |
|--------|-------|---------|
| `Feature.status.setupComplete` | Feature → host | Internal init finished |
| `Feature.status.attention` | Feature → host | High-priority user attention |
| `EntryPoint(categories = setOf("alerts"))` | Feature | Show on Alerts tab via config query |

Update `status` and `entryPoints` in `onActivated` / `onAccessChanged`.

---

## 5. Licensing (optional)

- Licensing feature exposes `LicenseProvider` as a `FeatureApi`.
- Runtime merges signed overlay `features` into ActiveConfig (scaffold unchanged).
- Dev: `AcceptAllLicenseVerifier` in `sak-runtime`. Production: supply real `LicenseVerifier`.

---

## 6. Troubleshooting

| Issue | Fix |
|-------|-----|
| Feature not appearing | Check `features.{id}.enabled` and `entries.allowed` in config |
| ServiceLoader empty | Verify `META-INF/services` file and module on classpath |
| Navigation crash | Route must be registered in feature `navContributor` |

---

## See also

- [ARCHITECTURE.md](ARCHITECTURE.md) — SDK & runtime diagrams
- [IOS.md](IOS.md) — iOS host setup
- [config-schema.md](config-schema.md) — JSON config reference
- Root [README.md](../README.md) — repo overview
