# SAK on iOS

This guide covers bootstrapping SAK in an iOS application using SwiftUI and the `sak-ios-bridge`.

---

## 1. Setup

### Automated Discovery (KSP)
SAK uses KSP to automate feature registration on iOS. Simply annotate your `FeatureModule` objects in your shared Kotlin code:

```kotlin
@SakModule
object MyAnalyticsModule : FeatureModule { ... }
```

The build will automatically generate the registry needed for the iOS bridge.

### Xcode Integration
Ensure the `sak-ios-bridge` is correctly linked as a framework in your Xcode project.

---

## 2. Bootstrapping (Swift Side)

SwiftUI is the "Host" on iOS and defines the navigation structure.

```swift
import SakIosBridge

@MainActor
final class AppModel: ObservableObject {
    private let host = SakIosHost()
    @Published var entries: [IosResolvedEntry] = []

    func start() {
        host.start()
        
        // Refresh entries when config changes
        host.setConfigListener { _ in
            self.entries = host.entriesForCategory(category: "home")
        }
    }
}
```

---

## 3. Creating the Shell

Use the data provided by the bridge to build your native SwiftUI layout.

```swift
struct HomeView: View {
    @ObservedObject var model: AppModel

    var body: some View {
        ScrollView {
            VStack {
                ForEach(model.entries, id: \.entry.id) { resolved in
                    // Render your native card based on 'resolved.entry.metadata'
                    NativeFeatureCard(entry: resolved.entry)
                }
            }
        }
        .onAppear { model.start() }
    }
}
```

---

## 4. Key Differences from Android

- **Headless Architecture**: `SakIosHost` does not provide UI components. It provides the **data and state**, allowing you to build 100% native SwiftUI views.
- **Reactive Navigation**: The bridge provides signals when a feature wants to navigate. You are responsible for pushing the view onto your `NavigationStack`.
- **Async Loading**: `entriesForCategory` is an asynchronous operation that triggers lazy loading of features if they are not yet instantiated.
