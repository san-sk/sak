package sak.harness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/** Android harness Activity — loads config and shows [HarnessApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultConfig = loadHarnessConfig()
        setContent {
            MaterialTheme {
                HarnessApp(defaultConfig = defaultConfig)
            }
        }
    }
}
