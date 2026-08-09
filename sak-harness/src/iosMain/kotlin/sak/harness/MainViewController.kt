package sak.harness

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController

/** iOS entry: UIViewController hosting Compose [HarnessApp]. */
fun MainViewController() = ComposeUIViewController {
    MaterialTheme {
        HarnessApp(defaultConfig = loadHarnessConfig())
    }
}
