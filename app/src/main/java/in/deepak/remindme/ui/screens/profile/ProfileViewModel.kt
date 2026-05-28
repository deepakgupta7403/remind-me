package `in`.deepak.remindme.ui.screens.profile

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import `in`.deepak.remindme.data.preferences.GreetingStyle
import `in`.deepak.remindme.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Backs the Settings → Profile editor.
 *
 * Edits are buffered in [state] and only persisted to [UserPreferences] on
 * [save] — so backing out without tapping Save discards changes, matching the
 * "← Profile … Save" header in the design. No account, no network: everything
 * lives in local prefs.
 */
class ProfileViewModel(
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ProfileUiState(
            name = preferences.userName,
            avatarColorIndex = preferences.avatarColorIndex,
            showGreeting = preferences.showGreeting,
            greetingStyle = preferences.greetingStyle,
        )
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun setAvatarColor(index: Int) = _state.update { it.copy(avatarColorIndex = index) }

    fun setShowGreeting(value: Boolean) = _state.update { it.copy(showGreeting = value) }

    /** The style row cycles through the options on tap (there are only a few). */
    fun cycleGreetingStyle() =
        _state.update { it.copy(greetingStyle = it.greetingStyle.next()) }

    fun save() {
        val s = _state.value
        preferences.userName = s.name
        preferences.avatarColorIndex = s.avatarColorIndex
        preferences.showGreeting = s.showGreeting
        preferences.greetingStyle = s.greetingStyle
    }
}

data class ProfileUiState(
    val name: String,
    val avatarColorIndex: Int,
    val showGreeting: Boolean,
    val greetingStyle: GreetingStyle,
) {
    /** First letter for the avatar; a neutral dot when no name is set. */
    val avatarInitial: String = name.trim().firstOrNull()?.uppercase() ?: "🙂"

    /** Exactly how Home will render the greeting with the current settings. */
    val previewGreeting: String = greetingPreview(showGreeting, greetingStyle, name)
}

/** Avatar accent palette — six fixed swatches, indexed by [UserPreferences.avatarColorIndex]. */
object AvatarPalette {
    val colors: List<Color> = listOf(
        Color(0xFF6C5CE7), // indigo
        Color(0xFF1E8C6B), // green
        Color(0xFFB1442B), // rust
        Color(0xFFC9821B), // amber
        Color(0xFF9B2D54), // maroon
        Color(0xFF1F2A44), // navy
    )

    fun colorAt(index: Int): Color = colors[index.coerceIn(colors.indices)]
}

/**
 * Pure greeting formatter shared by the Profile preview. Home computes the same
 * line in [in.deepak.remindme.ui.screens.home.HomeViewModel] using the live
 * time-of-day word; here we use a representative "Good morning" so the preview
 * is stable. When the greeting is off, returns the empty string.
 */
private fun greetingPreview(show: Boolean, style: GreetingStyle, name: String): String {
    if (!show) return ""
    val opener = when (style) {
        GreetingStyle.TimeBased -> "Good morning"
        GreetingStyle.AlwaysHello -> "Hello"
    }
    val trimmed = name.trim()
    return if (trimmed.isEmpty()) opener else "$opener, $trimmed"
}
