# SAK on Android

This guide covers bootstrapping SAK in an Android application using Jetpack Compose.

---

## 1. Setup

### Add Dependencies
Include the SAK core libraries in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sak-sdk"))
    implementation(project(":sak-runtime"))
}
```

---

## 2. Bootstrapping

In your `MainActivity` or host Composable, initialize `SakRuntime`.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val scope = rememberCoroutineScope()
            var runtime by remember { mutableStateOf<SakRuntime?>(null) }
            val hostLifecycle = LocalLifecycleOwner.current.lifecycle

            LaunchedEffect(Unit) {
                val sakRuntime = SakRuntime(
                    defaultConfig = loadConfig(),
                    featureRegistry = FeatureRegistry.of(
                        AnalyticsFeature(),
                        LicensingFeature(licenseProvider),
                    ),
                    scope = scope,
                    hostLifecycle = hostLifecycle,
                )
                sakRuntime.start(licenseProvider)
                runtime = sakRuntime
            }

            runtime?.let { rt ->
                HarnessShell(rt)
            }
        }
    }
}
```

---

## 3. Rendering Feature Slots

Host apps consume SAK `EntryPoint` metadata descriptors reactively to render UI slots.

```kotlin
@Composable
fun ComposeFeatureSlot(
    runtime: SakRuntime,
    category: String,
) {
    val activeConfig by runtime.activeConfig.collectAsState()

    val entries by produceState<List<Pair<FeatureId, EntryPoint>>>(initialValue = emptyList(), activeConfig) {
        val resolved = mutableListOf<Pair<FeatureId, EntryPoint>>()
        activeConfig.features.keys.forEach { idVal ->
            val id = FeatureId(idVal)
            val feature = runtime.getFeature(id)
            if (feature != null) {
                feature.entryPoints.value
                    .filter { it.categories.contains(category) }
                    .forEach { resolved.add(id to it) }
            }
        }
        value = resolved.sortedByDescending { it.second.priority }
    }

    // Render cards, rows, or banners based on entries...
}
```

---

## 4. Troubleshooting

| Issue | Solution |
| :--- | :--- |
| Feature not loading | Check if the feature is `enabled` in `default-config.json`. |
| License Overlay not applying | Verify JSON schema version and feature keys match. |
| UI Crash | Check the **SAK Observability Dashboard** (`sak/debug`) to inspect feature health state. |
