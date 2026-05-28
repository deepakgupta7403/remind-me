package `in`.deepak.remindme.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.deepak.remindme.ui.navigation.Destination
import `in`.deepak.remindme.ui.screens.common.AppBottomNavigation
import `in`.deepak.remindme.ui.theme.BrandColors
import kotlinx.coroutines.launch

/**
 * Settings tab.
 *
 * The screen is intentionally just a static surface of grouped rows for now —
 * the backing features (custom sound, DND window, accent picker, backup) don't
 * exist yet, so taps show a "Coming soon" snackbar. UI shipping ahead of the
 * domain is a deliberate choice: the design comp drives layout, and the
 * placeholders make it obvious which features still need to be built.
 *
 * Sections: Notifications, Quiet Hours, Appearance, Data. To add a new row,
 * append to the relevant [SettingsSection] block — each row is one
 * [SettingsRow] call.
 */
@Composable
fun SettingsScreen(
    onTabSelected: (Destination) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun comingSoon(label: String) {
        scope.launch { snackbarHostState.showSnackbar("$label is coming soon.") }
    }

    var vibrationOn   by rememberSaveable { mutableStateOf(true) }
    var dndOn         by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        containerColor = BrandColors.PageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Destination.Settings.route,
                onSelect = onTabSelected,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandColors.PageBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandColors.TextHeading,
            )
            Spacer(Modifier.height(20.dp))

            SettingsSection(label = "ACCOUNT") {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = "Profile",
                    subtitle = "Your name, greeting & avatar",
                    trailing = { TrailingValue(value = null, chevron = true) },
                    onClick = onOpenProfile,
                )
            }

            Spacer(Modifier.height(20.dp))

            SettingsSection(label = "NOTIFICATIONS") {
                SettingsRow(
                    icon = Icons.Filled.VolumeUp,
                    title = "Default sound",
                    trailing = { TrailingValue(value = "Gentle chime", chevron = true) },
                    onClick = { comingSoon("Sound picker") },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Filled.GraphicEq,
                    title = "Vibration",
                    trailing = {
                        Switch(
                            checked = vibrationOn,
                            onCheckedChange = { vibrationOn = it },
                            colors = primarySwitchColors(),
                        )
                    },
                    onClick = { vibrationOn = !vibrationOn },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Filled.AccessTime,
                    title = "Snooze duration",
                    trailing = { TrailingValue(value = "10 min", chevron = true) },
                    onClick = { comingSoon("Snooze duration") },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "QUIET HOURS") {
                SettingsRow(
                    icon = Icons.Filled.Bedtime,
                    title = "Do not disturb",
                    subtitle = "10:30 pm – 7:00 am",
                    trailing = {
                        Switch(
                            checked = dndOn,
                            onCheckedChange = { dndOn = it },
                            colors = primarySwitchColors(),
                        )
                    },
                    onClick = { dndOn = !dndOn },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "APPEARANCE") {
                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Theme",
                    trailing = { TrailingValue(value = "System", chevron = true) },
                    onClick = { comingSoon("Theme picker") },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Filled.ColorLens,
                    title = "Accent color",
                    trailing = { AccentDots() },
                    onClick = { comingSoon("Accent color picker") },
                )
            }

            Spacer(Modifier.height(20.dp))
            SettingsSection(label = "DATA") {
                SettingsRow(
                    icon = Icons.Filled.Backup,
                    title = "Backup & sync",
                    trailing = { TrailingValue(value = null, chevron = true) },
                    onClick = { comingSoon("Backup & sync") },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- Section / Row primitives ---------------------------------------------

@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = BrandColors.TextBody,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.SurfaceCard),
    ) { Column { content() } }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandColors.Primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandColors.TextHeading,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.TextBody,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun TrailingValue(value: String?, chevron: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.TextBody,
            )
        }
        if (chevron) {
            Spacer(Modifier.size(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BrandColors.TextBody,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AccentDots() {
    val accents = listOf(
        BrandColors.Primary,
        BrandColors.Tile.WeeklyFg,
        BrandColors.Tile.DailyFg,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        accents.forEach { c ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(c),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp)
            .height(1.dp)
            .background(BrandColors.BorderSubtle),
    )
}

@Composable
private fun primarySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = BrandColors.Primary,
    uncheckedThumbColor = BrandColors.SurfaceCard,
    uncheckedTrackColor = BrandColors.SurfaceDim,
    uncheckedBorderColor = BrandColors.BorderSubtle,
)
