package sak.harness.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/** Host-injected demo service. */
interface HarnessLogService {
    val messages: StateFlow<List<String>>
    fun log(message: String)
    fun clear()
}

/** In-memory implementation registered by [sak.harness.HarnessApp]. */
class InMemoryHarnessLogService : HarnessLogService {
    private val _messages = MutableStateFlow<List<String>>(emptyList())

    override val messages: StateFlow<List<String>> = _messages.asStateFlow()

    override fun log(message: String) {
        _messages.value = (_messages.value + message).takeLast(20)
    }

    override fun clear() {
        _messages.value = emptyList()
    }
}
