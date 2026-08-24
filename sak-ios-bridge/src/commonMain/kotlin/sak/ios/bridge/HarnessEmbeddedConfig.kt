package sak.ios.bridge

/** Embedded demo DefaultConfig for [SakIosHost] (mirrors sak-harness config). */
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
        }
      }
    }
    """.trimIndent()
