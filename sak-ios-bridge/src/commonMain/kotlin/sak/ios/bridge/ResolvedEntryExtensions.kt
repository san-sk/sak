package sak.ios.bridge

import sak.runtime.ResolvedEntry
import sak.sdk.entry.EntryAction

/** Route string for Swift/native navigation when the user taps an entry widget. */
fun ResolvedEntry.navigationTarget(): String? =
    when (val action = entry.action) {
        is EntryAction.Navigate -> action.route
        is EntryAction.DeepLink -> action.uri
        EntryAction.None -> null
    }
