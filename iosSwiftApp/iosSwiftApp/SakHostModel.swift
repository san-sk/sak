import Foundation
import Combine
import SakIosBridge

/// SwiftUI host shell.
///
/// The shell owns all presentation decisions: it decides which tabs exist, how
/// entry points are grouped, and what a tap does. Features only publish
/// descriptive entry points plus an optional action.
@MainActor
final class SakHostModel: ObservableObject {
    /// Entry points received from the runtime, grouped by the host's chosen filter.
    @Published private(set) var entries: [EntryRow] = []
    @Published private(set) var availableDestinations: [String] = []
    @Published var selectedDestination: String = Home
    @Published var activeRoute: SakRouteSignal?

    /// Feature enablement observed from the runtime config.
    @Published private(set) var featureStates: [String: Bool] = [:]

    static let Home = "home"
    static let All = "all"

    struct EntryRow: Identifiable, Equatable {
        let id: String
        let featureId: String
        let type: String
        let style: String
        let title: String
        let subtitle: String
        let icon: String?
        let badge: String?
        let priority: Int
        let actionRoute: String?
    }

    struct SakRouteSignal: Identifiable, Equatable {
        let id = UUID()
        let value: String
    }

    private let host = SakIosHost(
        defaultConfigJson: Self.defaultConfigJson,
        features: []
    )

    /// Config the app ships with. Missing features default to enabled.
    private static let defaultConfigJson = """
    {
      "version": 1,
      "features": {
        "analytics": { "enabled": true },
        "licensing": { "enabled": true }
      }
    }
    """

    func start() {
        host.start {
            self.refreshEntries()
        }
        observeNavigation()
        observeFeatureStates()
    }

    /// Re-reads entry points from the runtime and applies the host's own grouping.
    func refreshEntries() {
        host.queryEntryPoints { entries in
            let rows = entries.map { entry in
                EntryRow(
                    id: "\(entry.featureId).\(entry.id)",
                    featureId: entry.featureId,
                    type: entry.type,
                    style: entry.style,
                    title: entry.title,
                    subtitle: entry.subtitle ?? "",
                    icon: entry.icon,
                    badge: entry.badge,
                    priority: entry.priority,
                    actionRoute: entry.actionRoute
                )
            }

            self.entries = rows
            let grouped = Set(rows.map { $0.featureId })
            self.availableDestinations = [SakHostModel.Home] + grouped.sorted()

            // Drop the current selection if it no longer has any content.
            if !self.availableDestinations.contains(self.selectedDestination) {
                self.selectedDestination = SakHostModel.Home
            }
        }
    }

    /// Toggles a feature at runtime; the runtime destroys or recreates it.
    func setFeatureEnabled(_ featureId: String, _ enabled: Bool) {
        host.setFeatureEnabled(featureId: featureId, enabled: enabled)
    }

    func isFeatureEnabled(_ featureId: String) -> Bool {
        featureStates[featureId] ?? host.isFeatureEnabled(featureId: featureId)
    }

    func onEntryTapped(_ row: EntryRow) {
        if let route = row.actionRoute {
            navigate(to: route)
        }
    }

    func navigate(to route: String) {
        host.navigate(route: route)
    }

    func closeRoute() {
        host.clearNavigation()
        activeRoute = nil
    }

    private func observeNavigation() {
        host.observeNavigation { route in
            if let route, !route.isEmpty {
                self.activeRoute = SakRouteSignal(value: route)
            } else {
                self.activeRoute = nil
            }
        }
    }

    private func observeFeatureStates() {
        for featureId in ["analytics", "licensing"] {
            host.observeFeatureEnabled(featureId: featureId) { enabled in
                self.featureStates[featureId] = enabled
            }
        }
    }
}