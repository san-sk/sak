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
            return "Real-Time Analytics"
        case .analyticsReport(let reportId):
            return "Report: \(reportId)"
        case .licensingUpgrade:
            return "License & Entitlements Hub"
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
                    model: model,
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
                    model: model,
                    onUnlock: { model.unlockPro() },
                    onReset: { model.resetToTrial() },
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
    @ObservedObject var model: SakHostModel
    let onOpenReport: (String) -> Void
    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Revenue Card
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

                    // Simple Chart Simulation
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

                // Grid Metrics
                HStack(spacing: 12) {
                    MetricWidget(title: "Active Users", value: "14,210", change: "+12%")
                    MetricWidget(title: "Avg Session", value: "4m 12s", change: "+5%")
                }

                if model.isProPlan {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Pro Analytics Suite Active")
                            .font(.headline)
                        Text("Full cohort analysis and automated exports unlocked.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Button("Open Weekly Performance Report") {
                            onOpenReport("weekly")
                        }
                        .buttonStyle(.borderedProminent)
                        .frame(maxWidth: .infinity)
                    }
                    .padding()
                    .background(Color.accentColor.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                } else {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Upgrade to Pro Tier")
                            .font(.headline)
                        Text("Unlock weekly report breakdowns & export capabilities.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Button("Upgrade License via SAK Licensing") {
                            model.navigate(to: "licensing/upgrade")
                        }
                        .buttonStyle(.borderedProminent)
                        .frame(maxWidth: .infinity)
                    }
                    .padding()
                    .background(Color.orange.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

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
                Text("Report ID: \(reportId) • Generated Real-Time")
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

// MARK: - Licensing (mirrors sample-licensing Compose screen)

private struct LicensingUpgradeView: View {
    @ObservedObject var model: SakHostModel
    let onUnlock: () -> Void
    let onReset: () -> Void
    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Tier Card
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Current Plan Tier")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(model.isProPlan ? "PRO ENTERPRISE TIER" : "STANDARD TRIAL TIER")
                                .font(.headline.bold())
                        }
                        Spacer()
                        Text(model.isProPlan ? "ACTIVE" : "TRIAL")
                            .font(.caption2.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(model.isProPlan ? Color.green : Color.orange)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())
                    }
                    Text(model.isProPlan ? "Full access to advanced analytics reports, custom widgets, and priority API grants." : "Limited trial access. Upgrade to Pro to unlock advanced reporting and export features.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16))

                // Interactive Controls
                VStack(alignment: .leading, spacing: 12) {
                    Text("Interactive Entitlement Control")
                        .font(.headline)
                    Text("Simulate live remote license overlay emissions from server.")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    if model.isProPlan {
                        Button("Downgrade / Reset to Trial Overlay") {
                            onReset()
                        }
                        .buttonStyle(.bordered)
                        .tint(.red)
                        .frame(maxWidth: .infinity)
                    } else {
                        Button("Simulate Pro Upgrade (Emit Pro Overlay)") {
                            onUnlock()
                        }
                        .buttonStyle(.borderedProminent)
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding()
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16))

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
