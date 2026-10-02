import SwiftUI

struct ContentView: View {
    @StateObject private var model = SakHostModel()

    private var routePresented: Binding<Bool> {
        Binding(
            get: { model.activeRoute != nil },
            set: { if !$0 { model.activeRoute = nil } }
        )
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if model.tabs.isEmpty {
                    ProgressView("Starting SAK Engine…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    DestinationView(model: model)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    TabBarView(model: model)
                }
            }
            .navigationTitle("SAK SuperApp Shell")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Text(model.isProPlan ? "PRO ACTIVE" : "TRIAL")
                        .font(.caption2.bold())
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(model.isProPlan ? Color.green.opacity(0.2) : Color.orange.opacity(0.2))
                        .foregroundStyle(model.isProPlan ? Color.green : Color.orange)
                        .clipShape(Capsule())
                }
            }
            .navigationDestination(isPresented: routePresented) {
                if let route = model.activeRoute {
                    FeatureRouteView(route: route.value, model: model)
                }
            }
        }
        .onAppear {
            model.start()
        }
    }
}

private struct TabBarView: View {
    @ObservedObject var model: SakHostModel

    var body: some View {
        HStack {
            ForEach(model.tabs) { tab in
                Button {
                    model.selectDestination(tab.destination)
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.icon)
                            .font(.subheadline)
                        Text(tab.label)
                            .font(.caption2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .foregroundStyle(model.selectedDestination == tab.destination ? Color.accentColor : Color.secondary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(Color(.secondarySystemBackground))
    }
}

private struct DestinationView: View {
    @ObservedObject var model: SakHostModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(model.selectedDestination.capitalized)
                    .font(.title2.bold())
                    .padding(.horizontal)

                if model.entries.isEmpty {
                    VStack(spacing: 8) {
                        Text("No active micro-frontend widgets for '\(model.selectedDestination)'")
                            .font(.headline)
                            .foregroundStyle(.secondary)
                        Text("Features publish entry points dynamically based on entitlement grants.")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                    .padding(.horizontal)
                } else {
                    ForEach(model.entries) { row in
                        EntryCardView(row: row) {
                            model.onEntryTapped(row)
                        }
                    }
                }
            }
            .padding(.vertical, 16)
        }
    }
}

private struct EntryCardView: View {
    let row: SakHostModel.EntryRow
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(row.title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Spacer()
                    if let badge = row.badge {
                        Text(badge)
                            .font(.caption2.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.accentColor.opacity(0.15))
                            .foregroundStyle(Color.accentColor)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                }

                Text(row.subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                if let val = row.valueText {
                    Text(val)
                        .font(.title2.bold())
                        .foregroundStyle(Color.accentColor)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
    }
}
