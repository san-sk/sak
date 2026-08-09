import SwiftUI

/// Maps SAK route strings to native SwiftUI screens (sample features).
enum SakParsedRoute: Equatable {
    case analyticsToday
    case analyticsReport(reportId: String)
    case licensingUpgrade
    case unknown(String)

    init(_ route: String) {
        switch route {
        case "analytics/today":
            self = .analyticsToday
        case "licensing/upgrade":
            self = .licensingUpgrade
        default:
            let reportPrefix = "analytics/report/"
            if route.hasPrefix(reportPrefix) {
                let reportId = String(route.dropFirst(reportPrefix.count))
                self = .analyticsReport(reportId: reportId.isEmpty ? "unknown" : reportId)
            } else {
                self = .unknown(route)
            }
        }
    }

    var navigationTitle: String {
        switch self {
        case .analyticsToday:
            return "Analytics · Today"
        case .analyticsReport(let reportId):
            return "Report: \(reportId)"
        case .licensingUpgrade:
            return "Upgrade license"
        case .unknown(let route):
            return route
        }
    }
}

struct FeatureRouteView: View {
    let route: String
    @ObservedObject var model: SakHostModel

    private var parsed: SakParsedRoute { SakParsedRoute(route) }

    var body: some View {
        Group {
            switch parsed {
            case .analyticsToday:
                AnalyticsTodayView(
                    onOpenReport: { reportId in
                        model.navigate(to: "analytics/report/\(reportId)")
                    },
                    onBack: { model.closeRoute() }
                )
            case .analyticsReport(let reportId):
                AnalyticsReportView(
                    reportId: reportId,
                    onBack: { model.navigate(to: "analytics/today") }
                )
            case .licensingUpgrade:
                LicensingUpgradeView(
                    onUnlock: { model.unlockPro() },
                    onBack: { model.closeRoute() }
                )
            case .unknown(let route):
                UnknownRouteView(route: route)
            }
        }
        .navigationTitle(parsed.navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Analytics (mirrors sample-analytics Compose screens)

private struct AnalyticsTodayView: View {
    let onOpenReport: (String) -> Void
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Analytics · Today")
                .font(.largeTitle.bold())
            Text("Trial summary: 42 events today")
                .foregroundStyle(.secondary)
            Button("Open weekly report") {
                onOpenReport("weekly")
            }
            .buttonStyle(.borderedProminent)
            Button("Back") {
                onBack()
            }
            .buttonStyle(.bordered)
            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct AnalyticsReportView: View {
    let reportId: String
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Report: \(reportId)")
                .font(.largeTitle.bold())
            Text("Pro analytics report content")
                .foregroundStyle(.secondary)
            Button("Back") {
                onBack()
            }
            .buttonStyle(.bordered)
            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

// MARK: - Licensing (mirrors sample-licensing Compose screen)

private struct LicensingUpgradeView: View {
    let onUnlock: () -> Void
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Upgrade license")
                .font(.largeTitle.bold())
            Text("Unlock pro analytics via signed license overlay")
                .foregroundStyle(.secondary)
            Button("Unlock Pro") {
                onUnlock()
            }
            .buttonStyle(.borderedProminent)
            Button("Back") {
                onBack()
            }
            .buttonStyle(.bordered)
            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct UnknownRouteView: View {
    let route: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Unknown route")
                .font(.headline)
            Text(route)
                .font(.body.monospaced())
            Text("No native screen registered for this path.")
                .foregroundStyle(.secondary)
            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
