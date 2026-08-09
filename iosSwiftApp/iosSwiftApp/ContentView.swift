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
                    ProgressView("Starting SAK…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    DestinationView(model: model)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    TabBarView(model: model)
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
                        Text(tab.icon.prefix(1).uppercased())
                            .font(.caption.bold())
                        Text(tab.label)
                            .font(.caption2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(
                        model.selectedDestination == tab.destination
                            ? Color.accentColor.opacity(0.15)
                            : Color.clear
                    )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .background(Color(.secondarySystemBackground))
    }
}

private struct DestinationView: View {
    @ObservedObject var model: SakHostModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(model.selectedDestination.capitalized)
                    .font(.title2.bold())
                    .padding(.horizontal)

                if model.entries.isEmpty {
                    Text("No entry widgets for this tab.")
                        .foregroundStyle(.secondary)
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
            VStack(alignment: .leading, spacing: 6) {
                Text("\(row.featureId) · \(row.entryId)")
                    .font(.headline)
                Text(row.style)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .padding(.horizontal)
    }
}
