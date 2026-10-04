package sak.harness

/** Embedded demo DefaultConfig — shared across Android assets and Kotlin loads on iOS. */
internal val harnessDefaultConfigJson: String =
    """
    {
      "version": 1,
      "features": {
        "analytics": { "enabled": true },
        "licensing": { "enabled": true },
        "demos": { "enabled": true },
        "sak-debug": { "enabled": true }
      }
    }
    """.trimIndent()