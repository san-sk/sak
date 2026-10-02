# Configuration & Licensing

SAK uses a dual-layer JSON configuration system (`SakConfig` & `FeatureAccess`) to manage feature availability and entitlements.

---

## 1. The Configuration Layers

| Layer | Source | Description |
| :--- | :--- | :--- |
| **Default Config** | Bundled Asset (`SakConfig`) | Shipped with the app. Defines the baseline feature set for that build. |
| **License Overlay** | Remote API (`SignedLicensePayload`) | Signed JSON payload updating `FeatureAccess` states and capability grants. |
| **Active Config** | Merged State (`activeConfig`) | The final source of truth calculated by `SakRuntime`. |

---

## 2. SakConfig Schema

```json
{
  "version": 1,
  "features": {
    "analytics": {
      "enabled": true,
      "plan": "pro",
      "grants": ["analytics.reports"],
      "attrs": {
        "max_reports": 50
      }
    }
  }
}
```

### FeatureAccess Fields
- **`enabled`** (Boolean): If `false`, the feature is deactivated (`onDestroy()`).
- **`plan`** (String): Business tier (e.g. `"trial"`, `"pro"`).
- **`grants`** (Set<String>): Capability tokens unlocked by the license (e.g. `"analytics.reports"`).
- **`attrs`** (Map<String, JsonElement>): Custom attribute map.
