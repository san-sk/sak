package sak.harness

import sak.sdk.config.AppConfig
import sak.sdk.config.SakJson

/** Parse harness JSON into [AppConfig]. */
fun decodeHarnessConfig(json: String): AppConfig =
    SakJson.decodeFromString(AppConfig.serializer(), json)

/** Embedded demo config (all platforms — no asset loading required). */
fun loadHarnessConfig(): AppConfig =
    decodeHarnessConfig(harnessDefaultConfigJson)
