import SwiftUI

/// Maps SAK route strings to native SwiftUI screens.
///
/// Route strings come from feature entry point actions; the host decides how to
/// present them.
enum SakParsedRoute: Equatable {
    case analyticsToday
    case analyticsReport(reportId: String)
    case featureControls
    case unknown(String)

    init(_ route: String) {
        switch route {
        case "analytics/today":
            self = .analyticsToday
        case "licensing/upgrade":
            self = .featureControls
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
            return "Real-Time Analytics"
        case .analyticsReport(let reportId):
            return "Report: \(reportId)"
        case .featureControls:
            return "Feature Controls"
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
            case .featureControls:
                FeatureControlsView(model: model, onBack: { model.closeRoute() })
            case .unknown(let route):
                UnknownRouteView(route: route)
            }
        }
        .navigationTitle(parsed.navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Analytics

private struct AnalyticsTodayView: View {
    let onOpenReport: (String) -> Void
    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Total Gross Revenue")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    HStack(alignment: .firstTextBaseline) {
                        Text("$128,450.00")
                            .font(.largeTitle.bold())
                        Text("+18.4%")
                            .font(.headline.bold())
                            .foregroundStyle(.green)
                    }

                    HStack(alignment: .bottom, spacing: 6) {
                        ForEach([0.4, 0.6, 0.3, 0.85, 0.55, 0.95, 0.7], id: \.self) { fraction in
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.accentColor)
                                .frame(height: 50 * fraction)
                        }
                    }
                    .frame(height: 60)
                    .padding(.top, 8)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16))

                HStack(spacing: 12) {
                    MetricWidget(title: "Active Users", value: "14,210", change: "+12%")
                    MetricWidget(title: "Avg Session", value: "4m 12s", change: "+5%")
                }

                Button("Open Weekly Performance Report") {
                    onOpenReport("weekly")
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)

                Button("Back to Shell") {
                    onBack()
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
            }
            .padding()
        }
    }
}

private struct MetricWidget: View {
    let title: String
    let value: String
    let change: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.title3.bold())
            Text(change)
                .font(.caption2.bold())
                .foregroundStyle(.green)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct AnalyticsReportView: View {
    let reportId: String
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 8) {
                Text("Weekly Performance Summary")
                    .font(.title2.bold())
                Text("Report ID: \(reportId)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Divider()
                Text("• Customer Retention: 84.5%")
                    .font(.subheadline)
                Text("• Top Traffic Channel: Organic Search (45%)")
                    .font(.subheadline)
                Text("• Server Latency: 120ms avg")
                    .font(.subheadline)
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))

            Button("Back to Dashboard") {
                onBack()
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)

            Spacer()
        }
        .padding()
    }
}

// MARK: - Feature controls

private struct FeatureControlsView: View {
    @ObservedObject var model: SakHostModel
    let onBack: () -> Void

    private let featureIds = ["analytics", "licensing"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Runtime feature state")
                        .font(.headline)
                    Text("Disabling a feature destroys it and removes its entry points. Re-enabling recreates and restarts it.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16))

                ForEach(featureIds, id: \.self) { featureId in
                    let enabled = model.isFeatureEnabled(featureId)
                    HStack {
                        Text(featureId)
                            .font(.subheadline)
                        Spacer()
                        Text(enabled ? "ENABLED" : "DISABLED")
                            .font(.caption2.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(enabled ? Color.green : Color.orange)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())
                    }
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button(enabled ? "Disable \(featureId)" : "Enable \(featureId)") {
                        model.setFeatureEnabled(featureId, !enabled)
                        model.refreshEntries()
                    }
                    .buttonStyle(.bordered)
                    .frame(maxWidth: .infinity)
                }

                Button("Back to Main Shell") {
                    onBack()
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity)
            }
            .padding()
        }
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