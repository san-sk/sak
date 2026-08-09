# App Config JSON Schema

**Schema version:** `1`  
**Guides:** [ARCHITECTURE.md](ARCHITECTURE.md) · [ANDROID.md](ANDROID.md) · [IOS.md](IOS.md)

---

## Overview

| Source | Contains |
|---|---|
| **DefaultConfig** | `scaffold` + `features` |
| **License overlay** | `features` only (signed) |
| **ActiveConfig** | `merge(default, license)` |

---

## Root — DefaultConfig

```json
{
  "version": 1,
  "scaffold": { ... },
  "features": { ... }
}
```

---

## `scaffold` — app chrome (DefaultConfig only, KMP naming)

Uses Compose / Material terms: **Scaffold**, **bottomBar**, **destinations**.

```json
{
  "scaffold": {
    "bottomBar": {
      "items": [
        { "id": "home", "label": "Home", "icon": "home", "destination": "home" },
        { "id": "alerts", "label": "Alerts", "icon": "bell", "destination": "alerts" },
        { "id": "profile", "label": "Profile", "icon": "person", "destination": "profile" }
      ]
    },
    "destinations": {
      "home": {
        "sections": [
          {
            "type": "entries",
            "refs": [
              { "feature": "analytics", "entry": "summary-card" },
              { "feature": "messaging", "entry": "recent-card" }
            ]
          },
          { "type": "query", "categories": ["home"], "styles": ["Card"] }
        ]
      },
      "alerts": {
        "sections": [{ "type": "query", "categories": ["alerts"] }]
      }
    }
  }
}
```

| Field | Description |
|---|---|
| `bottomBar.items` | Bottom navigation bar entries (Material `NavigationBar`) |
| `destination` | Route to scaffold destination: `scaffold/{destination}` in NavHost |
| `destinations` | Map of destination id → widget layout sections |

**Never in license overlay.**

---

## Section types

**entries:** `{ "type": "entries", "refs": [{ "feature": "...", "entry": "..." }] }`

**query:** `{ "type": "query", "categories": ["menu"], "styles": ["Row"] }`

---

## `features` — flexible access

```json
{
  "features": {
    "licensing": { "enabled": true, "plan": "complete" },
    "analytics": {
      "enabled": true,
      "visible": true,
      "plan": "trial",
      "grants": ["analytics.summary"],
      "entries": { "allowed": ["summary-card"] },
      "attrs": { "maxReports": 3 }
    },
    "billing": {
      "enabled": false,
      "visible": false,
      "plan": "none",
      "grants": []
    }
  }
}
```

| Field | Runtime | Feature interprets |
|---|---|---|
| `enabled` | lifecycle | — |
| `visible` | shell filter | — |
| `plan` | pass-through | business tier string |
| `grants` | pass-through | capability strings |
| `entries` | widget filter | — |
| `routes` | nav guard (optional) | — |
| `attrs` | pass-through | quotas, expiry, etc. |

`plan` and `grants` are opaque — features interpret them. Runtime enforces `enabled`, `visible`, `entries`, and optional `routes`.

---

## License overlay

```json
{
  "version": 1,
  "features": {
    "analytics": {
      "plan": "pro",
      "grants": ["analytics.summary", "analytics.reports"],
      "entries": null
    },
    "billing": { "enabled": true, "plan": "pro" }
  }
}
```

No `scaffold` key.

---

## Merge rules

```text
scaffold: always from DefaultConfig
features: deep-merge; license wins per feature id
```

---

## SDK mapping

| JSON | Kotlin |
|---|---|
| `scaffold` | `ScaffoldConfig` |
| `bottomBar` | `BottomBarConfig` |
| `destinations` | `DestinationLayouts` |
| Feature toggle | `FeatureToggle` |
| Root | `AppConfig` |

---

## Security

| Layer | Role |
|-------|------|
| DefaultConfig | Baseline access shipped in APK |
| Signed license overlay | Updates `features` only — verify with `LicenseVerifier` |
| Backend APIs | **Required** for real enforcement (config alone is UI gating) |

Client JSON can be modified for UI; premium actions must fail on the server without a valid entitlement.
