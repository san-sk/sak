package sak.ios.bridge

/** Embedded demo DefaultConfig for [SakIosHost] (mirrors sak-harness config). */
internal val harnessDefaultConfigJson: String =
    """
    {
      "version": 1,
      "scaffold": {
        "bottomBar": {
          "items": [
            { "id": "home", "label": "Home", "icon": "home", "destination": "home" },
            { "id": "alerts", "label": "Alerts", "icon": "alert", "destination": "alerts" }
          ]
        },
        "destinations": {
          "home": {
            "sections": [
              {
                "type": "entries",
                "refs": [
                  { "feature": "analytics", "entry": "summary-card" },
                  { "feature": "analytics", "entry": "trial-tile" },
                  { "feature": "licensing", "entry": "unlock-banner" },
                  { "feature": "licensing", "entry": "plan-row" },
                  { "feature": "licensing", "entry": "pro-widget" }
                ]
              }
            ]
          },
          "alerts": {
            "sections": [
              { "type": "query", "categories": ["alerts"], "styles": ["Row"] }
            ]
          }
        }
      },
      "features": {
        "licensing": { "enabled": true, "visible": true, "plan": "trial" },
        "analytics": {
          "enabled": true,
          "visible": true,
          "plan": "trial",
          "grants": ["analytics.summary"],
          "entries": { "allowed": ["summary-card", "trial-tile"] }
        }
      }
    }
    """.trimIndent()
