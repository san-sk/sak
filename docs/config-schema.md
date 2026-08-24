# App config JSON schema

**Schema:** `schema: 1`  
**Architecture:** [ARCHITECTURE.md](ARCHITECTURE.md)

Entitlement document only — **no scaffold, no tab layout**.

---

## Shape

```json
{
  "schema": 1,
  "license": {
    "type": "trial",
    "rev": "optional-backend-revision"
  },
  "features": {
    "analytics": {
      "state": "enabled",
      "capabilities": ["analytics.summary"],
      "settings": { "refreshMinutes": 15 }
    }
  }
}
```

| Field | Role |
|-------|------|
| `license.type` | Display label (guest, trial, pro) — **not used for gating** |
| `license.rev` | Optional cache / staleness hint |
| `features.{id}.state` | `enabled` \| `disabled` \| `hide` — load policy |
| `features.{id}.capabilities` | Strings for UI locks and route guards |
| `features.{id}.settings` | Opaque JSON — feature interprets; runtime pass-through |

---

## Sources (same shape everywhere)

| Source | Example |
|--------|---------|
| Bundled default | `assets/default-config.json` |
| Login / backend | `EntitlementConfigProvider` |
| Signed overlay | `LicenseProvider` + verifier |

Runtime: `default.mergeEntitlementOverlay(overlay)` → **ActiveConfig**.

Overlay wins for `state`, `capabilities`, and `license`; `settings` shallow-merge per feature id.

---

## `state` values

| JSON | Runtime |
|------|---------|
| `enabled` | `Feature.onCreate()` |
| `disabled` | `Feature.onDestroy()` — host may show locked UI |
| `hide` | `Feature.onDestroy()` — host hides all surfaces for id |

---

## Kotlin types

| JSON | Type |
|------|------|
| Root | `sak.sdk.config.AppConfig` |
| `license` | `sak.sdk.license.LicenseInfo` |
| Feature row | `sak.sdk.feature.FeatureConfig` |
| `state` enum | `sak.sdk.feature.FeatureState` |
| Codec | `sak.sdk.config.ConfigJson` |

---

## Entry placement (not in this JSON)

Features publish `EntryPoint` with `tags` (e.g. `home`, `alerts`). Host shell maps tags to tabs — see [ARCHITECTURE.md §7](ARCHITECTURE.md#7-entry-points--shell).
