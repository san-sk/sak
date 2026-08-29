# Configuration & Licensing

SAK uses a dual-layer JSON configuration system to manage feature availability, licensing, and remote attributes.

---

## 1. The Configuration Layers

| Layer | Source | Description |
| :--- | :--- | :--- |
| **Default Config** | Bundled Asset | Shipped with the app. Defines the baseline feature set for that build. |
| **License Overlay** | Remote API | A signed JSON payload that updates `enabled` states and provides entitlements. |
| **Active Config** | Merged (In-Memory) | The final source of truth calculated by the `SakRuntime`. |

---

## 2. JSON Schema

### Root Object
```json
{
  "version": 1,
  "features": {
    "feature-id": {
      "enabled": true,
      "plan": "premium",
      "grants": ["high_res_export"],
      "attrs": {
        "max_projects": 10
      }
    }
  }
}
```

### Feature Toggle Fields
- **`enabled`** (Boolean): If `false`, the feature is not instantiated or is deactivated.
- **`plan`** (String): Opaque business tier (e.g., `"trial"`, `"pro"`). Features use this for internal gating.
- **`grants`** (Set<String>): Specific capabilities unlocked by the license.
- **`attrs`** (Map<String, JsonElement>): Arbitrary parameters. Can be complex nested objects.

---

## 3. Merging Logic
When a License Overlay is applied:
1.  If a feature exists in the overlay, it **overwrites** the definition in the Default Config.
2.  If it doesn't exist in the overlay, the Default Config version remains.
3.  The `version` field is updated to the overlay version.

---

## 4. Security & Verification
To prevent tampering with entitlements:
1.  The License Overlay should be fetched from a secure backend.
2.  The payload should be **signed** (e.g., JWS).
3.  Implement a `LicenseVerifier` and pass it to the `SakLauncher`.

```kotlin
class MySecureVerifier : LicenseVerifier {
    override fun verify(payload: SignedLicensePayload): Boolean {
        // Perform cryptographic signature check
        return true 
    }
}
```
