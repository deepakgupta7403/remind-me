package `in`.deepak.remindme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import `in`.deepak.remindme.ui.LocalAppContainer
import `in`.deepak.remindme.ui.navigation.RemindMeNavHost
import `in`.deepak.remindme.ui.theme.RemindMeTheme

/**
 * Single Activity hosting all screens. Provides the [AppContainer] to the
 * composition so every screen can resolve its dependencies via
 * [LocalAppContainer] without reaching into the Application directly.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RemindMeApp).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                RemindMeTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RemindMeNavHost()
                    }
                }
            }
        }
    }
}
