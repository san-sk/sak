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
        let resolved: Sak_runtimeResolvedEntry
    }

    func start() {
        host.setRenderPlanListener { [weak self] plan in
            DispatchQueue.main.async {
                self?.applyRenderPlan(plan)
            }
        }
        host.setNavigationListener { [weak self] route in
            DispatchQueue.main.async {
                self?.activeRoute = RouteDestination(value: route)
            }
        }
        host.start()
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

    private func applyRenderPlan(_ plan: Sak_runtimeRenderPlan) {
        let items = host.bottomBarItems()
        tabs = items.map { item in
            TabItem(
                id: item.id,
                label: item.label,
                icon: item.icon,
                destination: item.destination
            )
        }
        if !tabs.contains(where: { $0.destination == selectedDestination }) {
            selectedDestination = tabs.first?.destination ?? "home"
        }
        refreshEntries()
    }

    private func refreshEntries() {
        let resolved = host.entriesForDestination(destinationId: selectedDestination)
        entries = resolved.map { entry in
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
