# SAK on Android

This guide covers bootstrapping SAK in an Android application using Jetpack Compose.

---

## 1. Setup

### Add Dependencies
Include the SAK core and UI libraries in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sak-sdk"))
    implementation(project(":sak-runtime"))
    implementation(project(":sak-compose-ui"))
    
    // Optional: Observability Dashboard (Debug only)
    debugImplementation(project(":sak-library-debug"))
}
```

---

## 2. Bootstrapping

In your `MainActivity`, initialize the `SakLauncher`.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val scope = rememberCoroutineScope()
            var runtime by remember { mutableStateOf<SakRuntime?>(null) }

            LaunchedEffect(Unit) {
                runtime = SakLauncher(scope).launch(
                    defaultConfig = loadConfig(),
                    featureModules = discoverFeatureModules()
                )
            }

            runtime?.let { rt ->
                SakApp(runtime = rt) {
                    MainShell(rt)
                }
            }
        }
    }
}
```

---

## 3. Creating the Shell

Use `SakFeaturePlaceHolder` to inject feature entry points into specific layout areas.

```kotlin
@Composable
fun MainShell(runtime: SakRuntime) {
    Scaffold(
        bottomBar = { MyBottomNav() }
    ) { innerPadding ->
        NavHost(...) {
            composable("home") {
                // Renders all feature widgets marked with "home" category
                SakFeaturePlaceHolder(
                    runtime = runtime,
                    category = "home",
                    onNavigate = { route -> navController.navigate(route.value) }
                )
            }
        }
    }
}
```

---

## 4. Troubleshooting

| Issue | Solution |
| :--- | :--- |
| Feature not loading | Check if the feature is `enabled` in `default-config.json`. |
| Discovery failed | Verify `META-INF/services/sak.sdk.bundle.FeatureModule` exists in the feature module. |
| UI Crash | Check the **SAK Observability Dashboard** (`sak/debug`) to see if the feature is in an `ERROR` state. |
