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
                DestinationView(model: model)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                FeatureControlsBar(model: model)
            }
            .navigationTitle("SAK Host Shell")
            .navigationBarTitleDisplayMode(.inline)
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

/// Host-owned demonstration of runtime feature enable/disable.
private struct FeatureControlsBar: View {
    @ObservedObject var model: SakHostModel

    private let featureIds = ["analytics", "licensing"]

    var body: some View {
        HStack(spacing: 8) {
            ForEach(featureIds, id: \.self) { featureId in
                let enabled = model.isFeatureEnabled(featureId)
                Button {
                    model.setFeatureEnabled(featureId, !enabled)
                    model.refreshEntries()
                } label: {
                    VStack(spacing: 3) {
                        Image(systemName: enabled ? "checkmark.circle.fill" : "pause.circle.fill")
                            .font(.subheadline)
                        Text(featureId)
                            .font(.caption2)
                        Text(enabled ? "on" : "off")
                            .font(.caption2.bold())
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                    .foregroundStyle(enabled ? Color.green : Color.secondary)
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

    private var visibleEntries: [SakHostModel.EntryRow] {
        model.selectedDestination == SakHostModel.Home
            ? model.entries
            : model.entries.filter { $0.featureId == model.selectedDestination }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Picker("Destination", selection: $model.selectedDestination) {
                    ForEach(model.availableDestinations, id: \.self) { destination in
                        Text(destination.capitalized).tag(destination)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                if visibleEntries.isEmpty {
                    VStack(spacing: 8) {
                        Text("No entry points published")
                            .font(.headline)
                            .foregroundStyle(.secondary)
                        Text("Features publish entry points dynamically. Disabled features publish nothing.")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                    .padding(.horizontal)
                } else {
                    ForEach(visibleEntries) { row in
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

                if !row.subtitle.isEmpty {
                    Text(row.subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                HStack(spacing: 6) {
                    Text(row.type)
                    Text("·")
                    Text(row.style)
                    if let route = row.actionRoute {
                        Text("·")
                        Text(route).font(.caption.monospaced())
                    }
                }
                .font(.caption2)
                .foregroundStyle(.tertiary)
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