package sak.harness.demo

import sak.runtime.SakRuntime

/** Set once by [sak.harness.HarnessApp] after launch so demo screens can read runtime StateFlows. */
object HarnessDemoRuntimeRef {
    var runtime: SakRuntime? = null
}
