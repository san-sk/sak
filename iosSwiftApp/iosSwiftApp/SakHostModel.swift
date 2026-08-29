import Foundation
import SakIosBridge

/// Bridges [SakIosHost] Kotlin callbacks into SwiftUI-friendly state.
final class SakHostModel: ObservableObject {
    @Published var tabs: [TabItem] = []
    @Published var selectedDestination: String = "home"
    @Published var entries: [EntryRow] = []
    @Published var activeRoute: RouteDestination? = nil

    struct RouteDestination: Identifiable, Hashable {
        let value: String
        var id: String { value }
    }

    private let host = SakIosHost()

    struct TabItem: Identifiable {
        let id: String
        let label: String
        let icon: String
        let destination: String
    }

    struct EntryRow: Identifiable {
        let id: String
        let featureId: String
        let entryId: String
        let style: String
        let resolved: IosResolvedEntry
    }

    func start() {
        host.setConfigListener { [weak self] _ in
            DispatchQueue.main.async {
                self?.refreshTabs()
                self?.refreshEntries()
            }
        }
        host.setNavigationListener { [weak self] route in
            DispatchQueue.main.async {
                self?.activeRoute = RouteDestination(value: route)
            }
        }
        host.start()
    }

    private func refreshTabs() {
        // Host defines layout
        tabs = [
            TabItem(id: "home", label: "Home", icon: "house", destination: "home"),
            TabItem(id: "alerts", label: "Alerts", icon: "bell", destination: "alerts")
        ]
    }

    func selectDestination(_ destination: String) {
        selectedDestination = destination
        refreshEntries()
    }

    func onEntryTapped(_ row: EntryRow) {
        host.onEntrySelected(entry: row.resolved)
    }

    func unlockPro() {
        host.unlockPro()
    }

    /// Navigate to a feature route (mirrors Compose NavHost — also used for in-feature pushes).
    func navigate(to route: String) {
        host.navigate(route: route)
    }

    func closeRoute() {
        activeRoute = nil
    }

    private func refreshEntries() {
        // Handle suspend function from Kotlin
        host.entriesForCategory(category: selectedDestination) { [weak self] resolved, error in
            if let resolved = resolved {
                DispatchQueue.main.async {
                    self?.entries = resolved.map { entry in
                        EntryRow(
                            id: "\(entry.featureId.value)-\(entry.entry.id.value)",
                            featureId: entry.featureId.value,
                            entryId: entry.entry.id.value,
                            style: entry.entry.style.name,
                            resolved: entry
                        )
                    }
                }
            }
        }
    }
}
