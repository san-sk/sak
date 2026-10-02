import Foundation
import Combine

/// SwiftUI Host View Model demonstrating direct consumption of SAK KMP framework on iOS.
@MainActor
final class SakHostModel: ObservableObject {
    @Published var tabs: [TabItem] = []
    @Published var selectedDestination: String = "home"
    @Published var entries: [EntryRow] = []
    @Published var activeRoute: SakRouteSignal? = nil
    @Published var isProPlan: Bool = false

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
        let title: String
        let subtitle: String
        let badge: String?
        let valueText: String?
        let style: String
        let actionRoute: String?
    }

    struct SakRouteSignal: Identifiable, Equatable {
        let id = UUID()
        let value: String
    }

    func start() {
        tabs = [
            TabItem(id: "home", label: "Home", icon: "house.fill", destination: "home"),
            TabItem(id: "alerts", label: "Alerts", icon: "bell.fill", destination: "alerts"),
            TabItem(id: "settings", label: "Settings", icon: "gearshape.fill", destination: "settings")
        ]
        refreshEntries()
    }

    func selectDestination(_ destination: String) {
        selectedDestination = destination
        refreshEntries()
    }

    func navigate(to route: String) {
        activeRoute = SakRouteSignal(value: route)
    }

    func unlockPro() {
        isProPlan = true
        refreshEntries()
    }

    func resetToTrial() {
        isProPlan = false
        refreshEntries()
    }

    func onEntryTapped(_ row: EntryRow) {
        if let route = row.actionRoute {
            navigate(to: route)
        }
    }

    func closeRoute() {
        activeRoute = nil
    }

    func refreshEntries() {
        if selectedDestination == "home" {
            var list: [EntryRow] = [
                EntryRow(
                    id: "summary-card",
                    featureId: "analytics",
                    entryId: "summary-card",
                    title: "Real-Time Traffic & Analytics",
                    subtitle: "14.2k active sessions • +18.4% growth",
                    badge: isProPlan ? "PRO UNLOCKED" : "TRIAL TIER",
                    valueText: "$128,450.00",
                    style: "Card",
                    actionRoute: "analytics/today"
                ),
                EntryRow(
                    id: "plan-row",
                    featureId: "licensing",
                    entryId: "plan-row",
                    title: "Subscription & Entitlements",
                    subtitle: isProPlan ? "Pro Enterprise Tier Active • All Features Granted" : "Free Trial Tier • Tap to Upgrade",
                    badge: isProPlan ? "PRO" : "TRIAL",
                    valueText: nil,
                    style: "Row",
                    actionRoute: "licensing/upgrade"
                )
            ]
            if !isProPlan {
                list.append(
                    EntryRow(
                        id: "unlock-banner",
                        featureId: "licensing",
                        entryId: "unlock-banner",
                        title: "Upgrade to SAK Enterprise Pro",
                        subtitle: "Unlock advanced analytics reports and automated exports",
                        badge: "50% OFF",
                        valueText: nil,
                        style: "Banner",
                        actionRoute: "licensing/upgrade"
                    )
                )
            }
            entries = list
        } else if selectedDestination == "alerts" {
            if isProPlan {
                entries = [
                    EntryRow(
                        id: "report-row",
                        featureId: "analytics",
                        entryId: "report-row",
                        title: "Weekly Performance Report Ready",
                        subtitle: "Conversion rate increased by +4.2% this week",
                        badge: "NEW REPORT",
                        valueText: nil,
                        style: "Row",
                        actionRoute: "analytics/report/weekly"
                    )
                ]
            } else {
                entries = []
            }
        } else {
            entries = [
                EntryRow(
                    id: "plan-row",
                    featureId: "licensing",
                    entryId: "plan-row",
                    title: "Subscription & Entitlements",
                    subtitle: isProPlan ? "Pro Enterprise Tier Active" : "Free Trial Tier • Tap to Upgrade",
                    badge: isProPlan ? "PRO" : "TRIAL",
                    valueText: nil,
                    style: "Row",
                    actionRoute: "licensing/upgrade"
                )
            ]
        }
    }
}
