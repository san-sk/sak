package sak.harness

import sak.sdk.config.SakConfig
import sak.sdk.config.SakJson

/** Parse harness JSON into [SakConfig]. */
fun decodeHarnessConfig(json: String): SakConfig =
    SakJson.decodeFromString(SakConfig.serializer(), json)

/** Embedded demo config. */
fun loadHarnessConfig(): SakConfig =
    decodeHarnessConfig(harnessDefaultConfigJson)
