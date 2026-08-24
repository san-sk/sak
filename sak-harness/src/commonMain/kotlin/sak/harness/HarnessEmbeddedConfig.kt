package sak.harness

/** Embedded demo DefaultConfig — shared across Android assets and Kotlin loads on iOS. */
internal val harnessDefaultConfigJson: String =
    """
    {
      "schema": 1,
      "license": { "type": "trial" },
      "features": {
        "licensing": {
          "state": "enabled",
          "capabilities": []
        },
        "analytics": {
          "state": "enabled",
          "capabilities": ["analytics.summary"]
        },
        "demos": {
          "state": "enabled",
          "capabilities": ["demos.all"],
          "settings": { "maxReports": 3, "expiry": "2027-01-01" }
        }
      }
    }
    """.trimIndent()
